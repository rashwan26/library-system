package patterns;

import entities.*;

public class BookFactory {

    public static Book createBook(String type) {
        switch(type.toUpperCase(java.util.Locale.ROOT)) {
            case "FICTION":
                return new FictionBook();
            case "NONFICTION":
                return new NonFictionBook();
            case "TEXTBOOK":
                return new Textbook();
            case "REFERENCE":
                return new ReferenceBook();
            case "BOOK":
                return new Book();
            default:
                throw new IllegalArgumentException("Unknown book type.");
        }
    }

    public static Book createBook(String type, String isbn, String title, String author) {
        Book book = createBook(type);
        book.setIsbn(isbn);
        book.setTitle(title);
        book.setAuthor(author);
        return book;
    }

    public static Book createBook(String type, String isbn, String title,
                                 String author, int year, double price) {
        Book book = createBook(type, isbn, title, author);
        book.setPublicationYear(year);
        book.setPrice(price);
        return book;
    }

    public static Book createFromFileString(String line) {
        String[] parts = util.Csv.parse(line);
        if (parts.length == 6) return Book.fromFileString(line); // Legacy untyped base book
        switch (parts[0]) {
            case "FICTION": return FictionBook.fromFileString(line);
            case "NONFICTION": return NonFictionBook.fromFileString(line);
            case "TEXTBOOK": return Textbook.fromFileString(line);
            case "REFERENCE": return ReferenceBook.fromFileString(line);
            case "BOOK": return Book.fromFileString(line);
            default: throw new IllegalArgumentException("Unknown book record type.");
        }
    }
}
