package entities;

public class Textbook extends NonFictionBook {
    private String courseCode;
    private int level;
    private String publisher;

    public Textbook() {
        super();
    }

    public Textbook(String isbn, String title, String author, int publicationYear,
                    double price, String subject, String edition,
                    String courseCode, int level, String publisher) {
        super(isbn, title, author, publicationYear, price, subject, edition);
        this.courseCode = courseCode;
        this.level = level;
        this.publisher = publisher;
    }

    @Override
    public void displayDetails() {
        super.displayDetails();
        System.out.println("Course Code: " + courseCode);
        System.out.println("Level: " + level);
        System.out.println("Publisher: " + publisher);
        System.out.println("Type: Textbook");
    }

    @Override
    public String toFileString() {
        return util.Csv.encode("TEXTBOOK", isbn, title, author, publicationYear, price, isAvailable, getSubject(), getEdition(), hasReferences(), courseCode, level, publisher);
    }

    // Getters and setters
    public String getCourseCode() { return courseCode; }
    public void setCourseCode(String courseCode) { this.courseCode = courseCode; }

    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }

    public String getPublisher() { return publisher; }
    public void setPublisher(String publisher) { this.publisher = publisher; }

    public static Textbook fromFileString(String line) {
        String[] parts = util.Csv.parse(line);
        if (parts.length == 14 && parts[1].equals("NONFICTION")) {
            String[] fixed = new String[13]; fixed[0] = parts[0];
            System.arraycopy(parts, 2, fixed, 1, 12); parts = fixed;
        }
        if (parts.length != 13) throw new IllegalArgumentException("Invalid Textbook record.");
        Textbook book = new Textbook();
        book.setIsbn(parts[1]);
        book.setTitle(parts[2]);
        book.setAuthor(parts[3]);
        book.setPublicationYear(Integer.parseInt(parts[4]));
        book.setPrice(Double.parseDouble(parts[5]));
        book.setAvailable(util.Validation.booleanValue(parts[6]));
        book.setSubject(parts[7]);
        book.setEdition(parts[8]);
        book.setHasReferences(util.Validation.booleanValue(parts[9]));
        book.setCourseCode(parts[10]);
        book.setLevel(Integer.parseInt(parts[11]));
        book.setPublisher(parts[12]);
        return book;
    }
}
