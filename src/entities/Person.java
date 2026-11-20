package entities;

public abstract class Person {
    protected String id;
    protected String name;
    protected String email;
    protected String phone;

    // Overloaded constructors
    public Person() {}

    public Person(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public Person(String id, String name, String email, String phone) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phone = phone;
    }

    // Abstract method
    public abstract void displayInfo();

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    @Override
    public String toString() {
        return "ID: " + id + ", Name: " + name + ", Email: " + email;
    }
}
