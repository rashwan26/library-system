package entities;

public class FictionBook extends Book {
    private String genre;
    private String targetAudience;
    private boolean isAwardWinning;

    public FictionBook() {
        super();
    }

    public FictionBook(String isbn, String title, String author, int publicationYear,
                       double price, String genre, String targetAudience) {
        super(isbn, title, author, publicationYear, price);
        this.genre = genre;
        this.targetAudience = targetAudience;
        this.isAwardWinning = false;
    }

    @Override
    public void displayDetails() {
        super.displayDetails();
        System.out.println("Genre: " + genre);
        System.out.println("Target Audience: " + targetAudience);
        System.out.println("Award Winning: " + (isAwardWinning ? "Yes" : "No"));
        System.out.println("Type: Fiction");
    }

    // Method overriding
    @Override
    public String toFileString() {
        return util.Csv.encode("FICTION", isbn, title, author, publicationYear, price, isAvailable, genre, targetAudience, isAwardWinning);
    }

    // Getters and setters
    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }

    public String getTargetAudience() { return targetAudience; }
    public void setTargetAudience(String targetAudience) { this.targetAudience = targetAudience; }

    public boolean isAwardWinning() { return isAwardWinning; }
    public void setAwardWinning(boolean awardWinning) { isAwardWinning = awardWinning; }

    public static FictionBook fromFileString(String line) {
        String[] parts = util.Csv.parse(line);
        if (parts.length != 10) throw new IllegalArgumentException("Invalid FictionBook record.");
        FictionBook book = new FictionBook();
        book.setIsbn(parts[1]);
        book.setTitle(parts[2]);
        book.setAuthor(parts[3]);
        book.setPublicationYear(Integer.parseInt(parts[4]));
        book.setPrice(Double.parseDouble(parts[5]));
        book.setAvailable(util.Validation.booleanValue(parts[6]));
        book.setGenre(parts[7]);
        book.setTargetAudience(parts[8]);
        book.setAwardWinning(util.Validation.booleanValue(parts[9]));
        return book;
    }
}
