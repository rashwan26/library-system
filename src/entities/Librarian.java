package entities;

public class Librarian extends Person {
    private String employeeId;
    private String department;
    private String shift;

    public Librarian() {
        super();
    }

    public Librarian(String id, String name, String email, String phone,
                     String employeeId, String department) {
        super(id, name, email, phone);
        this.employeeId = employeeId;
        this.department = department;
    }

    @Override
    public void displayInfo() {
        System.out.println("=== Librarian Information ===");
        System.out.println("Employee ID: " + employeeId);
        System.out.println("Name: " + name);
        System.out.println("Department: " + department);
        System.out.println("Email: " + email);
        System.out.println("Shift: " + (shift != null ? shift : "Not assigned"));
    }

    // Method overloading
    public void assignShift(String shift) {
        this.shift = shift;
        System.out.println("Shift " + shift + " assigned to " + name);
    }

    public void assignShift(String shift, String location) {
        this.shift = shift;
        System.out.println("Shift " + shift + " at " + location + " assigned to " + name);
    }

    // Getters and setters
    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getShift() { return shift; }
    public void setShift(String shift) { this.shift = shift; }

    public String toFileString() {
        return util.Csv.encode(id, name, email, phone, employeeId, department, shift);
    }

    public static Librarian fromFileString(String line) {
        String[] parts = util.Csv.parse(line);
        Librarian librarian = new Librarian();
        librarian.setId(parts[0]);
        librarian.setName(parts[1]);
        librarian.setEmail(parts[2]);
        librarian.setPhone(parts[3]);
        librarian.setEmployeeId(parts[4]);
        librarian.setDepartment(parts[5]);
        if (parts.length > 6) {
            librarian.setShift(parts[6]);
        }
        return librarian;
    }
}
