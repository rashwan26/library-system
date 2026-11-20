package fileio;

import entities.Transaction;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** One atomic authoritative snapshot; legacy text files are read until migration. */
public class FileHandler {
    private static final Object LOCK = new Object();
    private final Path root;
    public static Path dataDirectory() { return Paths.get(System.getProperty("library.dataDir", "data")).toAbsolutePath().normalize(); }
    public FileHandler() { this(dataDirectory()); }
    public FileHandler(Path directory) {
        root = directory.toAbsolutePath().normalize();
        try { Files.createDirectories(root); }
        catch (IOException e) { throw new IllegalStateException("Cannot create the library data directory.", e); }
    }
    public Path getDirectory() { return root; }
    private Path path(String name) { return root.resolve(name + ".txt"); }
    private String key(String name) {
        for (String k : Arrays.asList("books", "members", "transactions"))
            if (name.equals("data/" + k + ".txt") || Paths.get(name).toAbsolutePath().normalize().equals(path(k))) return k;
        return null;
    }
    private Map<String,List<String>> state() throws IOException {
        Map<String,List<String>> result = new LinkedHashMap<>();
        for (String k : Arrays.asList("books", "members", "transactions")) result.put(k, new ArrayList<>());
        Path snapshot = root.resolve("library-state.txt");
        if (!Files.exists(snapshot)) {
            for (String k : result.keySet()) if (Files.exists(path(k)))
                for (String line : Files.readAllLines(path(k), StandardCharsets.UTF_8))
                    if (!line.trim().isEmpty()) result.get(k).add(line);
            return result;
        }
        List<String> lines = Files.readAllLines(snapshot, StandardCharsets.UTF_8);
        if (lines.isEmpty() || !lines.get(0).equals("LIBRARY_STATE_V1")) throw new IOException("Invalid library snapshot header.");
        try {
            for (int i=1; i<lines.size(); i++) {
                String line=lines.get(i); int at=line.indexOf(':');
                if (at<0 || !result.containsKey(line.substring(0,at))) throw new IllegalArgumentException();
                result.get(line.substring(0,at)).add(new String(Base64.getDecoder().decode(line.substring(at+1)), StandardCharsets.UTF_8));
            }
        } catch (IllegalArgumentException e) { throw new IOException("Malformed library snapshot. Restore a verified backup.", e); }
        return result;
    }
    public void commitSnapshot(List<String> books, List<String> members, List<String> transactions) throws IOException {
        synchronized (LOCK) {
            List<String> lines = new ArrayList<>(); lines.add("LIBRARY_STATE_V1");
            List<List<String>> records = Arrays.asList(books, members, transactions);
            String[] keys={"books","members","transactions"};
            for(int i=0;i<keys.length;i++) for(String row:records.get(i))
                lines.add(keys[i]+":"+Base64.getEncoder().encodeToString(row.getBytes(StandardCharsets.UTF_8)));
            atomicWrite(root.resolve("library-state.txt"),lines);
        }
    }
    public static void atomicWrite(Path target, List<String> lines) throws IOException {
        Path parent = target.toAbsolutePath().getParent(); Files.createDirectories(parent);
        Path temporary = Files.createTempFile(parent,"library-write-",".tmp");
        try {
            Files.write(temporary, lines, StandardCharsets.UTF_8);
            try { Files.move(temporary,target.toAbsolutePath(),StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException e) { Files.move(temporary,target.toAbsolutePath(),StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temporary); }
    }
    private boolean replace(String k,List<String> rows) throws IOException {
        synchronized(LOCK) {
            Map<String,List<String>> data=state(); data.put(k,new ArrayList<>(rows));
            commitSnapshot(data.get("books"),data.get("members"),data.get("transactions")); return true;
        }
    }
    private boolean append(String k,String row) throws IOException {
        synchronized(LOCK) { List<String> rows=new ArrayList<>(state().get(k)); rows.add(row); return replace(k,rows); }
    }
    public List<String> readBooks() throws IOException { synchronized(LOCK) { return state().get("books"); } }
    public List<String> readMembers() throws IOException { synchronized(LOCK) { return state().get("members"); } }
    public List<String> readTransactions() throws IOException { synchronized(LOCK) { return state().get("transactions"); } }
    public List<String> readLibrarians() throws IOException { return Files.exists(path("librarians")) ? Files.readAllLines(path("librarians"),StandardCharsets.UTF_8) : new ArrayList<>(); }
    public boolean saveBook(String row) throws IOException { return append("books",row); }
    public boolean saveBook(List<String> rows) throws IOException { return replace("books",rows); }
    public boolean saveMember(String row) throws IOException { return append("members",row); }
    public boolean saveMember(List<String> rows) throws IOException { return replace("members",rows); }
    public boolean saveTransaction(String row) throws IOException { return append("transactions",row); }
    public boolean saveTransaction(List<String> rows) throws IOException { return replace("transactions",rows); }
    public List<String> readFromFile(String name) throws IOException {
        String k=key(name); if(k!=null) { synchronized(LOCK) {return state().get(k);} }
        Path p=Paths.get(name); return Files.exists(p)? Files.readAllLines(p,StandardCharsets.UTF_8):new ArrayList<>();
    }
    public boolean saveToFile(String name,List<String> rows) throws IOException {
        String k=key(name); if(k!=null) return replace(k,rows);
        atomicWrite(Paths.get(name),rows); return true;
    }
    public boolean saveToFile(String name,String row,boolean append) throws IOException {
        List<String> rows=append?readFromFile(name):new ArrayList<>(); rows.add(row); return saveToFile(name,rows);
    }
    public List<Transaction> readAllTransactions() throws IOException {
        List<Transaction> result=new ArrayList<>();
        for(String row:readTransactions()) result.add(Transaction.fromFileString(row));
        return result;
    }
    public boolean deleteFromFile(String name,String identifier) throws IOException {
        List<String> rows=readFromFile(name),updated=new ArrayList<>();
        for(String row:rows) {
            String id="books".equals(key(name)) ? patterns.BookFactory.createFromFileString(row).getIsbn() : util.Csv.parse(row)[0];
            if(!id.equals(identifier)) updated.add(row);
        }
        if(updated.size()==rows.size()) return false;
        return saveToFile(name,updated);
    }
    public boolean updateInFile(String name,String oldLine,String newLine) throws IOException {
        List<String> rows=readFromFile(name);int index=rows.indexOf(oldLine);
        if(index<0)return false; rows.set(index,newLine);return saveToFile(name,rows);
    }
}
