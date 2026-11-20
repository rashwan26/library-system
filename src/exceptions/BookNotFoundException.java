package exceptions;

public class BookNotFoundException extends LibraryException {

    public BookNotFoundException(String isbn) {
        super("Book with ISBN '" + isbn + "' was not found in the library.", "BOOK-404");
    }

    public BookNotFoundException(String title, String author) {
        super("Book '" + title + "' by " + author + " was not found.", "BOOK-404");
    }
}
