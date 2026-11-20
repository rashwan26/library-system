import entities.*;
import exceptions.*;
import fileio.*;
import patterns.BookFactory;
import services.*;
import util.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;
import java.util.zip.ZipFile;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

/** Dependency-free regression suite. Every failure exits nonzero. */
public final class LibraryRegressionTests {
    private interface Check {void run() throws Exception;}
    private static int count;
    private static Path temporary;
    private static Path fresh() throws IOException {return Files.createTempDirectory(temporary,"case-");}
    private static Book book(String id){return BookFactory.createBook("BOOK",id,"A title","An author",2020,12.5);}
    private static Member member(String id){return new Member(id,"Demo member","demo@example.invalid","","MEM-"+id);}
    private static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private static void expect(Class<? extends Throwable> type,Check action) throws Exception {
        try{action.run();}catch(Throwable t){if(type.isInstance(t))return;throw new AssertionError("Wrong exception: "+t,t);}
        throw new AssertionError("Expected "+type.getSimpleName());
    }
    private static void test(String name,Check action) throws Exception {action.run();count++;System.out.println("PASS "+name);}
    public static void main(String[] args) throws Exception {
        temporary=Files.createTempDirectory("library-regression-");
        try {run();System.out.println(count+" regression scenarios passed.");}
        finally {try(java.util.stream.Stream<Path> walk=Files.walk(temporary)){for(Path p:walk.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(p);}}
    }
    private static void run() throws Exception {
        test("CSV quotes, commas and trailing empties",()->{
            String[] fields={"id","A, B","He said \"hello\"",""};require(Arrays.equals(fields,Csv.parse(Csv.encode((Object[])fields))),"CSV round trip");
            expect(IllegalArgumentException.class,()->Csv.parse("\"unclosed"));
        });
        test("Legacy plain BOOK records remain readable",()->{
            require(BookFactory.createFromFileString("OLD,A title,An author,2020,12.5,true").getIsbn().equals("OLD"),"Legacy base book");
        });
        for(String type:List.of("BOOK","FICTION","NONFICTION","TEXTBOOK","REFERENCE")) {
            test(type+" metadata round trip",()->{
                Book b=BookFactory.createBook(type,"ID-"+type,"الكتاب، \"عنوان\", Part 2","كاتب, Author",2020,9.5);
                Book copy=BookFactory.createFromFileString(b.toFileString());
                require(copy.getClass()==b.getClass() && copy.getTitle().equals(b.getTitle()) && copy.getAuthor().equals(b.getAuthor()),"Type/Unicode fields");
            });
        }
        test("Textbook specialized fields survive",()->{
            Textbook b=new Textbook("TX","Text","Writer",2020,10,"Math, CS","Second","CS101",2,"Publisher, Ltd");
            Textbook copy=(Textbook)BookFactory.createFromFileString(b.toFileString());
            require(copy.getSubject().equals("Math, CS")&&copy.getCourseCode().equals("CS101")&&copy.getLevel()==2&&copy.getPublisher().equals("Publisher, Ltd"),"Textbook attributes");
        });
        test("Legacy nested subtype prefix is recovered",()->{
            Textbook b=new Textbook("TX","Text","Writer",2020,10,"CS","First","CS101",1,"Publisher");
            String legacy=b.toFileString().replaceFirst("TEXTBOOK,","TEXTBOOK,NONFICTION,");
            require(BookFactory.createFromFileString(legacy).getIsbn().equals("TX"),"Nested legacy prefix");
        });
        test("Empty library has no seeded personal records",()->{
            LibraryService s=new LibraryService(fresh());require(s.getBooks().isEmpty()&&s.getMembers().isEmpty()&&s.getTransactions().isEmpty(),"Clean start");
        });
        test("Book and member persist across restart",()->{
            Path root=fresh();LibraryService s=new LibraryService(root);s.addBook(book("B1"));s.addMember(member("M1"));
            LibraryService reopened=new LibraryService(root);require(reopened.getBooks().size()==1&&reopened.getMembers().size()==1,"Reload");
        });
        test("Borrow and return persist consistent balances",()->{
            Path root=fresh();LibraryService s=new LibraryService(root);s.addBook(book("B"));s.addMember(member("M"));s.borrowBook("M","B");
            s=new LibraryService(root);require(!s.getBooks().get(0).isAvailable()&&s.getMembers().get(0).getBorrowedBooks()==1,"Borrow balances");
            s.returnBook("M","B");s=new LibraryService(root);
            require(s.getBooks().get(0).isAvailable()&&s.getMembers().get(0).getBorrowedBooks()==0&&s.getTransactions().get(0).isReturned(),"Return balances");
        });
        test("Active duplicate borrow is rejected",()->{
            LibraryService s=new LibraryService(fresh());s.addBook(book("B"));s.addMember(member("M"));s.borrowBook("M","B");
            expect(IllegalStateException.class,()->s.borrowBook("M","B"));require(s.getTransactions().size()==1,"No duplicate transaction");
        });
        test("Five-book member limit",()->{
            LibraryService s=new LibraryService(fresh());s.addMember(member("M"));
            for(int i=0;i<6;i++)s.addBook(book("B"+i));for(int i=0;i<5;i++)s.borrowBook("M","B"+i);
            expect(MemberLimitException.class,()->s.borrowBook("M","B5"));require(s.getMembers().get(0).getBorrowedBooks()==5,"Limit preserved");
        });
        test("Reference in-library use restriction",()->{
            LibraryService s=new LibraryService(fresh());s.addMember(member("M"));s.addBook(BookFactory.createBook("REFERENCE","R","Ref","Writer",2020,10));
            expect(IllegalStateException.class,()->s.borrowBook("M","R"));
        });
        test("Borrowable reference uses its own loan duration",()->{
            LibraryService s=new LibraryService(fresh());s.addMember(member("M"));
            ReferenceBook b=new ReferenceBook("R","Ref","Writer",2020,10,"CS","First","A1",true,3);s.addBook(b);
            require(s.borrowBook("M","R").getDueDate().equals(LocalDate.now().plusDays(3)),"Reference due date");
        });
        test("Overdue return fine is calculated and frozen",()->{
            Transaction t=new Transaction("T","M","B",LocalDate.now().minusDays(20),LocalDate.now().minusDays(6));
            t.returnBook();require(t.getFineAmount()==6,"Late fine");t.calculateFine();require(t.getFineAmount()==6&&!t.isOverdue(),"Frozen return fine");
            expect(IllegalStateException.class,t::returnBook);
        });
        test("On-time return fine stays zero",()->{
            Transaction t=new Transaction("T","M","B");t.returnBook();require(t.getFineAmount()==0,"On-time fine");
        });
        test("Unknown member and missing book are rejected",()->{
            LibraryService s=new LibraryService(fresh());s.addMember(member("M"));
            expect(IllegalArgumentException.class,()->s.borrowBook("UNKNOWN","B"));expect(BookNotFoundException.class,()->s.borrowBook("M","B"));
        });
        test("Duplicate identifiers rejected",()->{
            LibraryService s=new LibraryService(fresh());s.addBook(book("B"));s.addMember(member("M"));
            expect(IllegalArgumentException.class,()->s.addBook(book("B")));expect(IllegalArgumentException.class,()->s.addMember(member("M")));
        });
        test("Exact matching avoids prefix ISBN corruption",()->{
            LibraryService s=new LibraryService(fresh());s.addBook(book("12"));s.addBook(book("123"));s.addMember(member("M"));s.borrowBook("M","12");
            require(s.getBooks().stream().filter(b->b.getIsbn().equals("123")).findFirst().orElseThrow().isAvailable(),"Unrelated book unaffected");
            s.returnBook("M","12");s.deleteBook("12");require(s.getBooks().size()==1&&s.getBooks().get(0).getIsbn().equals("123"),"Exact deletion");
        });
        test("Member editing preserves join date and loan count",()->{
            Path root=fresh();LibraryService s=new LibraryService(root);Member m=member("M");m.setJoinDate(LocalDate.of(2020,1,2));s.addMember(m);s.addBook(book("B"));s.borrowBook("M","B");
            s.updateMember("M","New, Name","new@example.invalid","");s=new LibraryService(root);
            require(s.getMembers().get(0).getJoinDate().equals(LocalDate.of(2020,1,2))&&s.getMembers().get(0).getBorrowedBooks()==1,"Member metadata");
        });
        test("Book edits preserve subtype attributes and borrowing status",()->{
            Path root=fresh();LibraryService s=new LibraryService(root);
            s.addBook(new FictionBook("F","Title","Writer",2020,10,"Genre, One","Adult"));s.addMember(member("M"));s.borrowBook("M","F");
            s.updateBook("F","FICTION","Edited, Title","Edited Writer",2021,15);s=new LibraryService(root);
            FictionBook b=(FictionBook)s.getBooks().get(0);require(b.getGenre().equals("Genre, One")&&!b.isAvailable(),"Edit preserves subtype/loan");
        });
        test("Active loans block deletion and finished history remains",()->{
            LibraryService s=new LibraryService(fresh());s.addBook(book("B"));s.addMember(member("M"));s.borrowBook("M","B");
            expect(IllegalStateException.class,()->s.deleteBook("B"));expect(IllegalStateException.class,()->s.deleteMember("M"));
            s.returnBook("M","B");s.deleteBook("B");s.deleteMember("M");require(s.getTransactions().size()==1,"Completed history retained");
        });
        test("Unsafe IDs and nonfinite price rejected",()->{
            LibraryService s=new LibraryService(fresh());expect(IllegalArgumentException.class,()->s.addBook(book("../bad")));
            Book b=book("B");b.setPrice(Double.NaN);expect(IllegalArgumentException.class,()->s.addBook(b));
            b.setPrice(Double.POSITIVE_INFINITY);expect(IllegalArgumentException.class,()->s.addBook(b));
        });
        test("Getters cannot mutate authoritative library state",()->{
            LibraryService s=new LibraryService(fresh());s.addBook(book("B"));s.getBooks().get(0).setTitle("Tampered");require(s.getBooks().get(0).getTitle().equals("A title"),"Defensive copies");
        });
        test("Failed commit rolls back all borrowing state",()->{
            Path root=fresh();FailingHandler handler=new FailingHandler(root);LibraryService s=new LibraryService(handler);s.addBook(book("B"));s.addMember(member("M"));handler.fail=true;
            expect(IllegalStateException.class,()->s.borrowBook("M","B"));
            require(s.getTransactions().isEmpty()&&s.getBooks().get(0).isAvailable()&&s.getMembers().get(0).getBorrowedBooks()==0,"Memory rollback");
            require(new LibraryService(root).getTransactions().isEmpty(),"Disk rollback");
        });
        test("Malformed legacy data fails without overwriting it",()->{
            Path root=fresh();String bad="FICTION,broken";Files.writeString(root.resolve("books.txt"),bad,StandardCharsets.UTF_8);
            expect(IllegalStateException.class,()->new LibraryService(root));require(Files.readString(root.resolve("books.txt")).equals(bad),"Original data retained");
        });
        test("CSV report is written and neutralizes spreadsheet formulas",()->{
            LibraryService s=new LibraryService(fresh());Book b=book("B");b.setTitle("=A1, عنوان");s.addBook(b);Path report=temporary.resolve("report.csv");LibraryExports.report(s,report);
            String[] row=Csv.parse(Files.readAllLines(report).get(1));require(row[1].equals("'=A1, عنوان"),"Spreadsheet-safe CSV export");
        });
        test("Image validation and replacement",()->{
            Path root=fresh(),source=root.resolve("source.png"),covers=root.resolve("covers");
            ImageIO.write(new BufferedImage(4,4,BufferedImage.TYPE_INT_RGB),"png",source.toFile());
            Files.createDirectories(covers);Files.writeString(covers.resolve("B.jpg"),"old");Path stored=ImageStore.store(source,covers,"B");
            require(ImageIO.read(stored.toFile())!=null&&!Files.exists(covers.resolve("B.jpg")),"Normalized replacement");
            expect(IllegalArgumentException.class,()->ImageStore.store(source,covers,"../bad"));
            Path bad=root.resolve("bad.png");Files.writeString(bad,"not an image");expect(IOException.class,()->ImageStore.read(bad));
        });
        test("Backup ZIP contains state and images",()->{
            Path root=fresh();LibraryService s=new LibraryService(root);s.addBook(book("B"));Files.createDirectories(root.resolve("book_covers"));Files.write(root.resolve("book_covers/B.png"),new byte[]{1,2});
            Path output=temporary.resolve("backup.zip");LibraryExports.backup(root,output);
            try(ZipFile zip=new ZipFile(output.toFile())){require(zip.getEntry("library-state.txt")!=null&&zip.getEntry("book_covers/B.png")!=null,"Backup entries");}
            expect(IOException.class,()->LibraryExports.backup(root,root.resolve("recursive.zip")));
        });
        test("Visible log is really exported as UTF-8",()->{
            Path target=temporary.resolve("log.txt");LibraryExports.log("إرجاع\nLoan returned",target);require(Files.readString(target).contains("إرجاع"),"UTF-8 log export");
        });
        test("Sorted JTable selection targets correct model record",()->{
            SwingUtilities.invokeAndWait(()->{
                DefaultTableModel model=new DefaultTableModel(new Object[][]{{"B"},{"A"}},new String[]{"ID"});JTable table=new JTable(model);table.setAutoCreateRowSorter(true);table.getRowSorter().toggleSortOrder(0);table.setRowSelectionInterval(0,0);
                require(model.getValueAt(TableSelection.modelRow(table),0).equals("A"),"Sorted selection");
            });
        });
        test("Application lock prevents concurrent desktop instances",()->{
            Path root=fresh();try(ApplicationLock first=ApplicationLock.acquire(root)){expect(IOException.class,()->ApplicationLock.acquire(root));}
            try(ApplicationLock reopened=ApplicationLock.acquire(root)){require(reopened!=null,"Lock released");}
        });
        test("Oversized images are rejected before decoding",()->{
            Path image=fresh().resolve("large.png");try(RandomAccessFile file=new RandomAccessFile(image.toFile(),"rw")){file.setLength(10*1024*1024+1);}
            expect(IOException.class,()->ImageStore.read(image));
        });
        test("Malformed snapshot header fails safely",()->{
            Path root=fresh();Files.writeString(root.resolve("library-state.txt"),"NOT_A_LIBRARY");expect(IllegalStateException.class,()->new LibraryService(root));
        });
        test("Invalid CSV booleans and newline fields are rejected",()->{
            expect(IllegalArgumentException.class,()->BookFactory.createFromFileString("BOOK,B,Title,Author,2020,10,maybe"));
            expect(IllegalArgumentException.class,()->Csv.encode("line\nline"));
        });
        test("Completed history remains loadable after record deletion",()->{
            Path root=fresh();LibraryService service=new LibraryService(root);service.addBook(book("B"));service.addMember(member("M"));service.borrowBook("M","B");service.returnBook("M","B");service.deleteMember("M");service.deleteBook("B");
            LibraryService reopened=new LibraryService(root);require(reopened.getTransactions().size()==1&&reopened.getBooks().isEmpty()&&reopened.getMembers().isEmpty(),"Deleted records do not destroy completed history");
        });
        test("Search is case-insensitive and validates type",()->{
            LibraryService s=new LibraryService(fresh());s.addBook(book("B"));require(s.searchBooks("  TITLE  ","title").size()==1,"Search");expect(IllegalArgumentException.class,()->s.searchBooks("x","unknown"));
        });
    }
    private static final class FailingHandler extends FileHandler {
        boolean fail;
        FailingHandler(Path root){super(root);}
        @Override public void commitSnapshot(List<String>b,List<String>m,List<String>t)throws IOException{
            if(fail){fail=false;throw new IOException("Simulated disk write failure");}super.commitSnapshot(b,m,t);
        }
    }
}
