package services;

import entities.*;
import exceptions.*;
import fileio.FileHandler;
import patterns.BookFactory;
import util.Validation;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/** Shared business rules for GUI actions and automated regression tests. */
public class LibraryService {
    private final FileHandler fileHandler;
    private final List<Book> books = new ArrayList<>();
    private final List<Member> members = new ArrayList<>();
    private final List<Transaction> transactions = new ArrayList<>();
    public LibraryService() { this(new FileHandler()); }
    public LibraryService(Path directory) { this(new FileHandler(directory)); }
    public LibraryService(FileHandler handler) { fileHandler=handler; loadAllData(); }
    private void loadAllData() {
        books.clear();members.clear();transactions.clear();
        try {
            for(String row:fileHandler.readBooks()) books.add(BookFactory.createFromFileString(row));
            for(String row:fileHandler.readMembers()) members.add(Member.fromFileString(row));
            for(String row:fileHandler.readTransactions()) transactions.add(Transaction.fromFileString(row));
            Set<String> ids=new HashSet<>();
            for(Book b:books) { validateBook(b);if(!ids.add(b.getIsbn()))throw new IllegalArgumentException("Duplicate book ID."); }
            ids.clear();
            for(Member m:members) { validateMember(m);if(!ids.add(m.getId()))throw new IllegalArgumentException("Duplicate member ID.");m.setBorrowedBooks(0); }
            Set<String> loaned=new HashSet<>();ids.clear();
            for(Book b:books)b.setAvailable(true);
            for(Transaction t:transactions) {
                if(!ids.add(t.getTransactionId()))throw new IllegalArgumentException("Duplicate transaction ID.");
                if(t.getDueDate().isBefore(t.getBorrowDate()) || t.isReturned() != (t.getReturnDate()!=null))
                    throw new IllegalArgumentException("Invalid loan dates/status.");
                if(!t.isReturned()) {
                    Book b=findBookByIsbn(t.getBookIsbn());Member m=findMemberById(t.getMemberId());
                    if(b==null || m==null || !loaned.add(t.getBookIsbn()))throw new IllegalArgumentException("Invalid active loan references.");
                    b.setAvailable(false);m.setBorrowedBooks(m.getBorrowedBooks()+1);
                }
            }
        } catch(IOException | RuntimeException e) {
            throw new IllegalStateException("Library data could not be loaded. No records were overwritten. Check the data directory or restore a backup.",e);
        }
    }
    private void persist() {
        try {
            fileHandler.commitSnapshot(books.stream().map(Book::toFileString).collect(Collectors.toList()),
                members.stream().map(Member::toString).collect(Collectors.toList()),
                transactions.stream().map(Transaction::toString).collect(Collectors.toList()));
        } catch(IOException e) { loadAllData();throw new IllegalStateException("Changes were not saved. Check disk space and directory permissions.",e); }
    }
    private void validateBook(Book b) {
        Validation.identifier(b.getIsbn());Validation.text(b.getTitle(),"title",true);Validation.text(b.getAuthor(),"author",true);
        if(b.getPublicationYear()<1000 || b.getPublicationYear()>java.time.Year.now().getValue()+1)
            throw new IllegalArgumentException("Publication year is out of range.");
        if(!Double.isFinite(b.getPrice()) || b.getPrice()<=0)throw new IllegalArgumentException("Price must be a finite positive number.");
        if(b instanceof ReferenceBook && (((ReferenceBook)b).getMaxLoanDays()<1 || ((ReferenceBook)b).getMaxLoanDays()>365))
            throw new IllegalArgumentException("Reference loan length must be 1-365 days.");
    }
    private void validateMember(Member m) {
        Validation.identifier(m.getId());Validation.text(m.getName(),"member name",true);
        Validation.text(m.getEmail(),"email",false);Validation.text(m.getPhone(),"phone",false);
        if(m.getJoinDate()==null)throw new IllegalArgumentException("Member join date is required.");
    }
    public synchronized void addBook(Book book) {
        validateBook(book);
        if(findBookByIsbn(book.getIsbn())!=null)throw new IllegalArgumentException("A book with this ID already exists.");
        Book copy=BookFactory.createFromFileString(book.toFileString());copy.setAvailable(true);books.add(copy);persist();
    }
    public synchronized void updateBook(String isbn,String type,String title,String author,int year,double price) {
        Book current=findBookByIsbn(isbn);
        if(current==null)throw new IllegalArgumentException("Book not found.");
        Book updated=typeOf(current).equals(type)?BookFactory.createFromFileString(current.toFileString()):BookFactory.createBook(type);
        updated.setIsbn(isbn);updated.setTitle(title);updated.setAuthor(author);updated.setPublicationYear(year);updated.setPrice(price);
        updated.setAvailable(current.isAvailable());validateBook(updated);
        if(!current.isAvailable() && !typeOf(current).equals(type))throw new IllegalStateException("Return this book before changing its type.");
        books.set(books.indexOf(current),updated);persist();
    }
    public synchronized void addMember(Member member) {
        validateMember(member);
        if(findMemberById(member.getId())!=null)throw new IllegalArgumentException("A member with this ID already exists.");
        Member copy=Member.fromFileString(member.toString());copy.setBorrowedBooks(0);members.add(copy);persist();
    }
    public synchronized void updateMember(String id,String name,String email,String phone) {
        Member current=findMemberById(id);
        if(current==null)throw new IllegalArgumentException("Member not found.");
        Member updated=Member.fromFileString(current.toString());updated.setName(name);updated.setEmail(email);updated.setPhone(phone);
        validateMember(updated);members.set(members.indexOf(current),updated);persist();
    }
    public synchronized void deleteMember(String id) {
        Member m=findMemberById(id);
        if(m==null)throw new IllegalArgumentException("Member not found.");
        if(transactions.stream().anyMatch(t->t.getMemberId().equals(id)&&!t.isReturned()))
            throw new IllegalStateException("Return all borrowed books before deleting this member.");
        members.remove(m);persist(); // Preserve completed loan history.
    }
    public synchronized void deleteBook(String isbn) {
        Book b=findBookByIsbn(isbn);
        if(b==null)throw new IllegalArgumentException("Book not found.");
        if(!canDeleteBook(isbn))throw new IllegalStateException("Return this book before deleting it.");
        books.remove(b);persist();
    }
    public synchronized Transaction borrowBook(String memberId,String isbn) throws BookNotFoundException,MemberLimitException {
        Member m=findMemberById(memberId);
        if(m==null)throw new IllegalArgumentException("Member not found.");
        if(!m.canBorrow())throw new MemberLimitException(m.getBorrowedBooks(),Member.getMaxBorrowLimit());
        Book b=findBookByIsbn(isbn);
        if(b==null)throw new BookNotFoundException(isbn);
        if(!b.isAvailable())throw new IllegalStateException("This book is already borrowed.");
        if(b instanceof ReferenceBook && !((ReferenceBook)b).canBorrow())throw new IllegalStateException("This reference book is for in-library use only.");
        int days=b instanceof ReferenceBook?((ReferenceBook)b).getMaxLoanDays():14;
        Transaction t=new Transaction("TXN-"+UUID.randomUUID(),memberId,isbn,LocalDate.now(),LocalDate.now().plusDays(days));
        transactions.add(t);b.setAvailable(false);m.setBorrowedBooks(m.getBorrowedBooks()+1);persist();
        return Transaction.fromFileString(t.toString());
    }
    public synchronized Transaction returnBook(String memberId,String isbn) throws BookNotFoundException {
        Transaction t=transactions.stream().filter(x->x.getMemberId().equals(memberId)&&x.getBookIsbn().equals(isbn)&&!x.isReturned()).findFirst().orElse(null);
        if(t==null)throw new IllegalArgumentException("No active loan matches this member and book.");
        Book b=findBookByIsbn(isbn);if(b==null)throw new BookNotFoundException(isbn);
        Member m=findMemberById(memberId);if(m==null)throw new IllegalArgumentException("Member not found.");
        t.returnBook();b.setAvailable(true);m.setBorrowedBooks(Math.max(0,m.getBorrowedBooks()-1));persist();
        return Transaction.fromFileString(t.toString());
    }
    private Book findBookByIsbn(String id) { return books.stream().filter(b->b.getIsbn().equals(id)).findFirst().orElse(null); }
    private Member findMemberById(String id) { return members.stream().filter(m->m.getId().equals(id)).findFirst().orElse(null); }
    public synchronized List<Book> getBooks() { return books.stream().map(b->BookFactory.createFromFileString(b.toFileString())).collect(Collectors.toList()); }
    public synchronized List<Member> getMembers() { return members.stream().map(m->Member.fromFileString(m.toString())).collect(Collectors.toList()); }
    public synchronized List<Transaction> getTransactions() { return transactions.stream().map(t->Transaction.fromFileString(t.toString())).collect(Collectors.toList()); }
    public synchronized List<Transaction> getOverdueTransactions() { return getTransactions().stream().filter(Transaction::isOverdue).collect(Collectors.toList()); }
    public synchronized boolean canDeleteBook(String isbn) { return findBookByIsbn(isbn)!=null && transactions.stream().noneMatch(t->t.getBookIsbn().equals(isbn)&&!t.isReturned()); }
    public static String typeOf(Book b) {
        if(b instanceof Textbook)return "TEXTBOOK";if(b instanceof ReferenceBook)return "REFERENCE";
        if(b instanceof NonFictionBook)return "NONFICTION";if(b instanceof FictionBook)return "FICTION";return "BOOK";
    }
    public synchronized List<Book> searchBooks(String query,String searchType) {
        if(query==null || searchType==null)throw new IllegalArgumentException("Search text and type are required.");
        String q=query.trim().toLowerCase(Locale.ROOT),kind=searchType.toLowerCase(Locale.ROOT);
        if(!Arrays.asList("title","author","isbn","all").contains(kind))throw new IllegalArgumentException("Unknown search type.");
        return getBooks().stream().filter(b->
            ((kind.equals("title")||kind.equals("all"))&&b.getTitle().toLowerCase(Locale.ROOT).contains(q)) ||
            ((kind.equals("author")||kind.equals("all"))&&b.getAuthor().toLowerCase(Locale.ROOT).contains(q)) ||
            ((kind.equals("isbn")||kind.equals("all"))&&b.getIsbn().toLowerCase(Locale.ROOT).contains(q))).collect(Collectors.toList());
    }
}
