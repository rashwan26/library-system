package entities;

public class NonFictionBook extends Book {
    private String subject;
    private String edition;
    private boolean hasReferences;

    public NonFictionBook() {
        super();
    }

    public NonFictionBook(String isbn, String title, String author, int publicationYear,
                          double price, String subject, String edition) {
        super(isbn, title, author, publicationYear, price);
        this.subject = subject;
        this.edition = edition;
        this.hasReferences = true;
    }

    @Override
    public void displayDetails() {
        super.displayDetails();
        System.out.println("Subject: " + subject);
        System.out.println("Edition: " + edition);
        System.out.println("Has References: " + (hasReferences ? "Yes" : "No"));
        System.out.println("Type: Non-Fiction");
    }

    @Override
    public String toFileString() {
        return util.Csv.encode("NONFICTION", isbn, title, author, publicationYear, price, isAvailable, subject, edition, hasReferences);
    }

    // Getters and setters
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getEdition() { return edition; }
    public void setEdition(String edition) { this.edition = edition; }

    public boolean hasReferences() { return hasReferences; }
    public void setHasReferences(boolean hasReferences) { this.hasReferences = hasReferences; }

    public static NonFictionBook fromFileString(String line) {
        String[] parts = util.Csv.parse(line);
        if (parts.length != 10) throw new IllegalArgumentException("Invalid NonFictionBook record.");
        NonFictionBook book = new NonFictionBook();
        book.setIsbn(parts[1]);
        book.setTitle(parts[2]);
        book.setAuthor(parts[3]);
        book.setPublicationYear(Integer.parseInt(parts[4]));
        book.setPrice(Double.parseDouble(parts[5]));
        book.setAvailable(util.Validation.booleanValue(parts[6]));
        book.setSubject(parts[7]);
        book.setEdition(parts[8]);
        book.setHasReferences(util.Validation.booleanValue(parts[9]));
        return book;
    }
}
