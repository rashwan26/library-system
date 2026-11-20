package entities;

import java.time.LocalDate;

public class Transaction {
    private String transactionId;
    private String memberId;
    private String bookIsbn;
    private LocalDate borrowDate;
    private LocalDate dueDate;
    private LocalDate returnDate;
    private boolean isReturned;
    private double fineAmount;

    public Transaction() {
        this.borrowDate = LocalDate.now();
        this.dueDate = borrowDate.plusDays(14);
        this.isReturned = false;
        this.fineAmount = 0.0;
    }

    public Transaction(String transactionId, String memberId, String bookIsbn) {
        this();
        this.transactionId = transactionId;
        this.memberId = memberId;
        this.bookIsbn = bookIsbn;
    }

    public Transaction(String transactionId, String memberId, String bookIsbn,
                      LocalDate borrowDate, LocalDate dueDate) {
        this.transactionId = transactionId;
        this.memberId = memberId;
        this.bookIsbn = bookIsbn;
        this.borrowDate = borrowDate;
        this.dueDate = dueDate;
        this.isReturned = false;
        this.fineAmount = 0.0;
    }

    public void calculateFine() {
        LocalDate end = isReturned ? returnDate : LocalDate.now();
        if (end == null || dueDate == null) throw new IllegalStateException("Missing transaction dates.");
        fineAmount = Math.max(0, end.toEpochDay() - dueDate.toEpochDay()) * 1.0;
    }

    public void returnBook() {
        if (isReturned) throw new IllegalStateException("This loan has already been returned.");
        this.returnDate = LocalDate.now();
        this.isReturned = true;
        calculateFine();
    }

    public boolean isOverdue() {
        return !isReturned && LocalDate.now().isAfter(dueDate);
    }

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public String getMemberId() { return memberId; }
    public void setMemberId(String memberId) { this.memberId = memberId; }

    public String getBookIsbn() { return bookIsbn; }
    public void setBookIsbn(String bookIsbn) { this.bookIsbn = bookIsbn; }

    public LocalDate getBorrowDate() { return borrowDate; }
    public void setBorrowDate(LocalDate borrowDate) { this.borrowDate = borrowDate; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    public LocalDate getReturnDate() { return returnDate; }
    public void setReturnDate(LocalDate returnDate) { this.returnDate = returnDate; }

    public boolean isReturned() { return isReturned; }
    public void setReturned(boolean returned) { isReturned = returned; }

    public double getFineAmount() { return fineAmount; }
    public void setFineAmount(double fineAmount) { this.fineAmount = fineAmount; }

    @Override
    public String toString() {
        return util.Csv.encode(transactionId, memberId, bookIsbn, borrowDate, dueDate,
            returnDate == null ? "null" : returnDate, isReturned, fineAmount);
    }

    public static Transaction fromFileString(String line) {
        String[] parts = util.Csv.parse(line);
        if (parts.length != 8) throw new IllegalArgumentException("Invalid transaction record.");
        Transaction transaction = new Transaction();
        transaction.setTransactionId(parts[0]);
        transaction.setMemberId(parts[1]);
        transaction.setBookIsbn(parts[2]);
        transaction.setBorrowDate(LocalDate.parse(parts[3]));
        transaction.setDueDate(LocalDate.parse(parts[4]));

        if (!parts[5].equals("null")) {
            transaction.setReturnDate(LocalDate.parse(parts[5]));
        }

        transaction.setReturned(util.Validation.booleanValue(parts[6]));
        transaction.setFineAmount(Double.parseDouble(parts[7]));

        return transaction;
    }

    public void displayTransactionInfo() {
        System.out.println("Transaction ID: " + transactionId);
        System.out.println("Member ID: " + memberId);
        System.out.println("Book ISBN: " + bookIsbn);
        System.out.println("Borrow Date: " + borrowDate);
        System.out.println("Due Date: " + dueDate);
        System.out.println("Returned: " + (isReturned ? "Yes" : "No"));
        if (isReturned) {
            System.out.println("Return Date: " + returnDate);
            System.out.println("Fine: $" + fineAmount);
        }
    }
}
