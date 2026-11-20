package entities;

import java.time.LocalDate;

public class Member extends Person {
    private String membershipId;
    private LocalDate joinDate;
    private int borrowedBooks;
    private static final int MAX_BORROW_LIMIT = 5;

    public Member() {
        super();
        this.joinDate = LocalDate.now();
        this.borrowedBooks = 0;
    }

    public Member(String id, String name, String email, String phone, String membershipId) {
        super(id, name, email, phone);
        this.membershipId = membershipId;
        this.joinDate = LocalDate.now();
        this.borrowedBooks = 0;
    }

    @Override
    public void displayInfo() {
        System.out.println("=== Member Information ===");
        System.out.println("Member ID: " + id);
        System.out.println("Name: " + name);
        System.out.println("Email: " + email);
        System.out.println("Membership ID: " + membershipId);
        System.out.println("Join Date: " + joinDate);
        System.out.println("Borrowed Books: " + borrowedBooks + "/" + MAX_BORROW_LIMIT);
    }

    @Override
    public String toString() {
        return util.Csv.encode(id, name, email, phone, membershipId, joinDate, borrowedBooks);
    }

    public boolean canBorrow() {
        return borrowedBooks < MAX_BORROW_LIMIT;
    }

    public boolean canBorrow(int additionalBooks) {
        return (borrowedBooks + additionalBooks) <= MAX_BORROW_LIMIT;
    }

    public void borrowBook() {
        if (canBorrow()) {
            borrowedBooks++;
        }
    }

    public void returnBook() {
        if (borrowedBooks > 0) {
            borrowedBooks--;
        }
    }

    public String getMembershipId() { return membershipId; }
    public void setMembershipId(String membershipId) { this.membershipId = membershipId; }

    public LocalDate getJoinDate() { return joinDate; }
    public void setJoinDate(LocalDate joinDate) { this.joinDate = joinDate; }

    public int getBorrowedBooks() { return borrowedBooks; }
    public void setBorrowedBooks(int borrowedBooks) { this.borrowedBooks = borrowedBooks; }

    public static int getMaxBorrowLimit() { return MAX_BORROW_LIMIT; }

    public static Member fromFileString(String line) {
        String[] parts = util.Csv.parse(line);
        if (parts.length != 7) throw new IllegalArgumentException("Invalid member record.");
        Member member = new Member();
        member.setId(parts[0]);
        member.setName(parts[1]);
        member.setEmail(parts[2]);
        member.setPhone(parts[3]);
        member.setMembershipId(parts[4]);
        member.setJoinDate(LocalDate.parse(parts[5]));
        member.setBorrowedBooks(Integer.parseInt(parts[6]));
        return member;
    }
}
