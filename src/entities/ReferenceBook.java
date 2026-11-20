package entities;

public class ReferenceBook extends NonFictionBook {
    private String location;
    private boolean canBorrow;
    private int maxLoanDays;

    public ReferenceBook() {
        super();
        this.canBorrow = false;
        this.maxLoanDays = 1;
    }

    public ReferenceBook(String isbn, String title, String author, int publicationYear,
                         double price, String subject, String edition,
                         String location, boolean canBorrow, int maxLoanDays) {
        super(isbn, title, author, publicationYear, price, subject, edition);
        this.location = location;
        this.canBorrow = canBorrow;
        this.maxLoanDays = maxLoanDays;
    }

    @Override
    public void displayDetails() {
        super.displayDetails();
        System.out.println("Location: " + location);
        System.out.println("Can Borrow: " + (canBorrow ? "Yes" : "No"));
        System.out.println("Max Loan Days: " + maxLoanDays);
        System.out.println("Type: Reference Book");
    }

    @Override
    public String toFileString() {
        return util.Csv.encode("REFERENCE", isbn, title, author, publicationYear, price, isAvailable, getSubject(), getEdition(), hasReferences(), location, canBorrow, maxLoanDays);
    }

    // Getters and setters
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public boolean canBorrow() { return canBorrow; }
    public void setCanBorrow(boolean canBorrow) { this.canBorrow = canBorrow; }

    public int getMaxLoanDays() { return maxLoanDays; }
    public void setMaxLoanDays(int maxLoanDays) { this.maxLoanDays = maxLoanDays; }

    public static ReferenceBook fromFileString(String line) {
        String[] parts = util.Csv.parse(line);
        if (parts.length == 14 && parts[1].equals("NONFICTION")) {
            String[] fixed = new String[13]; fixed[0] = parts[0];
            System.arraycopy(parts, 2, fixed, 1, 12); parts = fixed;
        }
        if (parts.length != 13) throw new IllegalArgumentException("Invalid ReferenceBook record.");
        ReferenceBook book = new ReferenceBook();
        book.setIsbn(parts[1]);
        book.setTitle(parts[2]);
        book.setAuthor(parts[3]);
        book.setPublicationYear(Integer.parseInt(parts[4]));
        book.setPrice(Double.parseDouble(parts[5]));
        book.setAvailable(util.Validation.booleanValue(parts[6]));
        book.setSubject(parts[7]);
        book.setEdition(parts[8]);
        book.setHasReferences(util.Validation.booleanValue(parts[9]));
        book.setLocation(parts[10]);
        book.setCanBorrow(util.Validation.booleanValue(parts[11]));
        book.setMaxLoanDays(Integer.parseInt(parts[12]));
        return book;
    }
}
