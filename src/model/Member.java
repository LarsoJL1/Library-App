package model;

public class Member extends Person {
    private int memberId;

    public Member(int memberId, String name, String email) {
        super(name, email);
        this.memberId = memberId;
    }

    public int getMemberId() { return memberId; }

    @Override
    public String describe() {
        return "Member #" + memberId + ": " + getName() + " <" + getEmail() + ">";
    }

    @Override
    public String toString() {
        return String.format("[%d] %-15s %s", memberId, getName(), getEmail());
    }
}
