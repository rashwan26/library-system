package entities;

public class Book {
    protected String isbn;
    protected String title;
    protected String author;
    protected int publicationYear;
    protected boolean isAvailable;
    protected double price;

    // Overloaded constructors
    public Book() {
        this.isAvailable = true;
    }

    public Book(String isbn, String title) {
        this.isbn = isbn;
        this.title = title;
        this.isAvailable = true;
    }

    public Book(String isbn, String title, String author, int publicationYear, double price) {
        this.isbn = isbn;
        this.title = title;
        this.author = author;
        this.publicationYear = publicationYear;
        this.price = price;
        this.isAvailable = true;
    }

    // Overridable display method
    public void displayDetails() {
        System.out.println("ISBN: " + isbn);
        System.out.println("Title: " + title);
        System.out.println("Author: " + author);
        System.out.println("Year: " + publicationYear);
        System.out.println("Available: " + (isAvailable ? "Yes" : "No"));
    }

    // Overloaded methods
    public String getInfo() {
        return title + " by " + author;
    }

    public String getInfo(boolean includeStatus) {
        return title + " by " + author + " - " + (isAvailable ? "Available" : "Borrowed");
    }

    // Getters and setters
    public String getIsbn() { return isbn; }
    public void setIsbn(String isbn) { this.isbn = isbn; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public int getPublicationYear() { return publicationYear; }
    public void setPublicationYear(int publicationYear) { this.publicationYear = publicationYear; }

    public boolean isAvailable() { return isAvailable; }
    public void setAvailable(boolean available) { isAvailable = available; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public String toFileString() {
        return util.Csv.encode("BOOK", isbn, title, author, publicationYear, price, isAvailable);
    }

    public static Book fromFileString(String line) {
        String[] parts = util.Csv.parse(line);
        if (parts.length == 7 && parts[0].equals("BOOK")) parts = java.util.Arrays.copyOfRange(parts, 1, parts.length);
        if (parts.length != 6) throw new IllegalArgumentException("Invalid book record.");
        Book book = new Book();
        book.setIsbn(parts[0]);
        book.setTitle(parts[1]);
        book.setAuthor(parts[2]);
        book.setPublicationYear(Integer.parseInt(parts[3]));
        book.setPrice(Double.parseDouble(parts[4]));
        book.setAvailable(util.Validation.booleanValue(parts[5]));
        return book;
    }
}
