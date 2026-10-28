package model;

import java.time.LocalDate;

public class Loan {
    private int loanId;
    private int memberId;
    private int bookId;
    private LocalDate checkoutDate;
    private LocalDate dueDate;
    private LocalDate returnDate;   // null means not returned yet
    private String memberName;      // filled in by the JOIN, for display only
    private String bookTitle;       // filled in by the JOIN, for display only

    public Loan(int loanId, int memberId, int bookId,
                LocalDate checkoutDate, LocalDate dueDate, LocalDate returnDate) {
        this.loanId = loanId;
        this.memberId = memberId;
        this.bookId = bookId;
        this.checkoutDate = checkoutDate;
        this.dueDate = dueDate;
        this.returnDate = returnDate;
    }

    public int getLoanId() { return loanId; }
    public int getMemberId() { return memberId; }
    public int getBookId() { return bookId; }
    public LocalDate getCheckoutDate() { return checkoutDate; }
    public LocalDate getDueDate() { return dueDate; }
    public LocalDate getReturnDate() { return returnDate; }
    public String getMemberName() { return memberName; }
    public String getBookTitle() { return bookTitle; }

    public void setMemberName(String memberName) { this.memberName = memberName; }
    public void setBookTitle(String bookTitle) { this.bookTitle = bookTitle; }

    public boolean isReturned() { return returnDate != null; }

    @Override
    public String toString() {
        return String.format("Loan %-3d %-15s %-26s out %s  due %s  %s",
                loanId, memberName, bookTitle, checkoutDate, dueDate,
                returnDate == null ? "NOT RETURNED" : "returned " + returnDate);
    }
}
