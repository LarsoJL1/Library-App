package model;

/**
 * Abstract parent class (inheritance requirement).
 * Member extends this. If you later add a Librarian or Staff class,
 * it can extend Person too.
 */
public abstract class Person {
    private String name;
    private String email;

    public Person(String name, String email) {
        this.name = name;
        this.email = email;
    }

    public String getName() { return name; }
    public String getEmail() { return email; }

    public void setName(String name) { this.name = name; }
    public void setEmail(String email) { this.email = email; }

    /** Each subclass describes itself differently. */
    public abstract String describe();
}
