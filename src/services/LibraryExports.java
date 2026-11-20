package services;

import entities.*;
import fileio.FileHandler;
import util.Csv;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import java.util.stream.Stream;

public final class LibraryExports {
    private LibraryExports() {}
    public static void report(LibraryService service,Path destination) throws IOException {
        List<String> rows=new ArrayList<>();
        rows.add(Csv.encode("ISBN / inventory ID","Title","Author","Year","Type","Available","Price"));
        for(Book b:service.getBooks())rows.add(Csv.encode(safe(b.getIsbn()),safe(b.getTitle()),safe(b.getAuthor()),b.getPublicationYear(),LibraryService.typeOf(b),b.isAvailable(),b.getPrice()));
        FileHandler.atomicWrite(destination,rows);
    }
    private static String safe(String text) {
        // Export as spreadsheet-safe text; leave stored book metadata unchanged.
        String probe=text.stripLeading();
        return !probe.isEmpty() && "=+-@".indexOf(probe.charAt(0))>=0 ? "'"+text : text;
    }
    public static void log(String text,Path destination) throws IOException {
        FileHandler.atomicWrite(destination,Arrays.asList(text.split("\\R",-1)));
    }
    public static void backup(Path data,Path destination) throws IOException {
        Path root=data.toAbsolutePath().normalize(),target=destination.toAbsolutePath().normalize();
        if(target.startsWith(root))throw new IOException("Choose a backup location outside the data directory.");
        Files.createDirectories(target.getParent());
        Path temp=Files.createTempFile(target.getParent(),"library-backup-",".tmp");
        try {
            try(ZipOutputStream zip=new ZipOutputStream(Files.newOutputStream(temp));Stream<Path> walk=Files.walk(root)) {
                for(Path p:(Iterable<Path>)walk::iterator) {
                    if(Files.isSymbolicLink(p) || !Files.isRegularFile(p) || p.getFileName().toString().endsWith(".tmp") || p.getFileName().toString().equals(".app.lock"))continue;
                    zip.putNextEntry(new ZipEntry(root.relativize(p).toString().replace(File.separatorChar,'/')));
                    Files.copy(p,zip);zip.closeEntry();
                }
            }
            Files.move(temp,target,StandardCopyOption.REPLACE_EXISTING);
        } finally { Files.deleteIfExists(temp); }
    }
}
