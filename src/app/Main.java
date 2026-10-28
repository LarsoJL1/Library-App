package app;

import dao.BookDAO;
import dao.CategoryDAO;
import dao.Database;
import dao.LoanDAO;
import dao.MemberDAO;
import model.Book;
import model.Category;
import model.Loan;
import model.Member;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Scanner;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/** Desktop entry point. All SQL lives in the DAO classes. */
public class Main {
    private static final Scanner in = new Scanner(System.in);
    private static final BookDAO bookDAO = new BookDAO();
    private static final MemberDAO memberDAO = new MemberDAO();
    private static final CategoryDAO categoryDAO = new CategoryDAO();
    private static final LoanDAO loanDAO = new LoanDAO();

    private static final int LOAN_DAYS = 14;

    public static void main(String[] args) {
        try {
            Database.ensureReady();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                    "Could not set up the database: " + e.getMessage()
                            + "\nRun the program from the folder that contains schema.sql and seed.sql.",
                    "Library setup error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        SwingUtilities.invokeLater(() -> new LibraryWindow().setVisible(true));
    }

    // ------------------------------------------------------------------
    // Menu
    // ------------------------------------------------------------------
    private static void printMenu() {
        System.out.println();
        System.out.println("--- BOOKS ---");
        System.out.println(" 1. List all books        2. Search books");
        System.out.println(" 3. Add a book            4. Update a book      5. Delete a book");
        System.out.println("--- MEMBERS ---");
        System.out.println(" 6. List members          7. Add a member");
        System.out.println(" 8. Update a member       9. Delete a member");
        System.out.println("--- LOANS ---");
        System.out.println("10. Check out a book     11. Return a book");
        System.out.println("12. Current loans        13. Overdue loans + late fees");
        System.out.println("14. Loans per member     15. One member's history");
        System.out.println("--- OTHER ---");
        System.out.println("16. Reset database to sample data      0. Exit");
    }

    // ------------------------------------------------------------------
    // Books
    // ------------------------------------------------------------------
    private static void listBooks() throws SQLException {
        printBooks(bookDAO.findAll());
    }

    private static void searchBooks() throws SQLException {
        String keyword = promptRequired("Search title or author: ");
        printBooks(bookDAO.search(keyword));
    }

    private static void printBooks(List<Book> books) {
        if (books.isEmpty()) {
            System.out.println("No books found.");
            return;
        }
        for (Book b : books) {
            System.out.println(b);
        }
    }

    private static void addBook() throws SQLException {
        String title = promptRequired("Title: ");
        String author = promptRequired("Author: ");
        Integer categoryId = promptCategory(null);
        int id = bookDAO.insert(title, author, categoryId);
        System.out.println("Added book #" + id + ".");
    }

    private static void updateBook() throws SQLException {
        int id = promptInt("Book id to update: ");
        Book book = bookDAO.findById(id);
        if (book == null) {
            System.out.println("No book with id " + id + ".");
            return;
        }
        System.out.println("Current: " + book);
        System.out.println("(Press Enter to keep the current value.)");

        String title = prompt("Title [" + book.getTitle() + "]: ");
        if (!title.isEmpty()) book.setTitle(title);

        String author = prompt("Author [" + book.getAuthor() + "]: ");
        if (!author.isEmpty()) book.setAuthor(author);

        book.setCategoryId(promptCategory(book.getCategoryId()));

        bookDAO.update(book);
        System.out.println("Book updated.");
    }

    private static void deleteBook() throws SQLException {
        int id = promptInt("Book id to delete: ");
        Book book = bookDAO.findById(id);
        if (book == null) {
            System.out.println("No book with id " + id + ".");
            return;
        }
        if (confirm("Delete \"" + book.getTitle() + "\"?")) {
            bookDAO.delete(id);
            System.out.println("Book deleted.");
        } else {
            System.out.println("Cancelled.");
        }
    }

    /** Shows categories and asks for one. Blank keeps 'current' (or none). */
    private static Integer promptCategory(Integer current) throws SQLException {
        System.out.println("Categories:");
        for (Category c : categoryDAO.findAll()) {
            System.out.println("  " + c);
        }
        while (true) {
            String text = prompt("Category id (Enter to skip/keep): ");
            if (text.isEmpty()) {
                return current;
            }
            try {
                int id = Integer.parseInt(text);
                if (categoryDAO.exists(id)) {
                    return id;
                }
                System.out.println("There is no category with that id.");
            } catch (NumberFormatException e) {
                System.out.println("Please enter a number.");
            }
        }
    }

    // ------------------------------------------------------------------
    // Members
    // ------------------------------------------------------------------
    private static void listMembers() throws SQLException {
        List<Member> members = memberDAO.findAll();
        if (members.isEmpty()) {
            System.out.println("No members.");
        }
        for (Member m : members) {
            System.out.println(m);
        }
    }

    private static void addMember() throws SQLException {
        String name = promptRequired("Name: ");
        String email = promptEmail("Email: ");
        int id = memberDAO.insert(name, email);
        System.out.println("Added member #" + id + ".");
    }

    private static void updateMember() throws SQLException {
        int id = promptInt("Member id to update: ");
        Member m = memberDAO.findById(id);
        if (m == null) {
            System.out.println("No member with id " + id + ".");
            return;
        }
        System.out.println("Current: " + m.describe());
        System.out.println("(Press Enter to keep the current value.)");

        String name = prompt("Name [" + m.getName() + "]: ");
        if (!name.isEmpty()) m.setName(name);

        String email = prompt("Email [" + m.getEmail() + "]: ");
        if (!email.isEmpty()) {
            if (isValidEmail(email)) {
                m.setEmail(email);
            } else {
                System.out.println("That doesn't look like an email, keeping the old one.");
            }
        }
        memberDAO.update(m);
        System.out.println("Member updated.");
    }

    private static void deleteMember() throws SQLException {
        int id = promptInt("Member id to delete: ");
        Member m = memberDAO.findById(id);
        if (m == null) {
            System.out.println("No member with id " + id + ".");
            return;
        }
        if (confirm("Delete " + m.getName() + "?")) {
            memberDAO.delete(id);
            System.out.println("Member deleted.");
        } else {
            System.out.println("Cancelled.");
        }
    }

    // ------------------------------------------------------------------
    // Loans
    // ------------------------------------------------------------------
    private static void checkoutBook() throws SQLException {
        int memberId = promptInt("Member id: ");
        Member m = memberDAO.findById(memberId);
        if (m == null) {
            System.out.println("No member with id " + memberId + ".");
            return;
        }
        int bookId = promptInt("Book id: ");
        Book b = bookDAO.findById(bookId);
        if (b == null) {
            System.out.println("No book with id " + bookId + ".");
            return;
        }
        loanDAO.checkout(memberId, bookId, LOAN_DAYS);
        System.out.println(m.getName() + " checked out \"" + b.getTitle()
                + "\". Due in " + LOAN_DAYS + " days.");
    }

    private static void returnBook() throws SQLException {
        int loanId = promptInt("Loan id to return: ");
        if (loanDAO.returnBook(loanId)) {
            System.out.println("Book returned.");
        } else {
            System.out.println("No unreturned loan with id " + loanId + ".");
        }
    }

    private static void showCurrentLoans() throws SQLException {
        List<Loan> loans = loanDAO.findActive();
        if (loans.isEmpty()) {
            System.out.println("Nothing is checked out right now.");
        }
        for (Loan l : loans) {
            System.out.println(l);
        }
    }

    private static void showOverdueAndFees() throws SQLException {
        LocalDate today = LocalDate.now();
        List<Loan> overdue = loanDAO.findOverdue(today);
        if (overdue.isEmpty()) {
            System.out.println("No overdue loans.");
            return;
        }
        System.out.println("Overdue loans as of " + today + ":");
        for (Loan l : overdue) {
            System.out.println(l + "  (" + LateFeeCalculator.daysLate(l, today) + " days late)");
        }

        System.out.println();
        System.out.printf("Late fees owed (rate $%.2f/day), highest first:%n",
                LateFeeCalculator.DAILY_RATE);
        for (LateFeeCalculator.Fee fee : LateFeeCalculator.calculate(overdue, today)) {
            System.out.printf("  %-15s %3d days late   $%.2f%n",
                    fee.getMemberName(), fee.getTotalDaysLate(), fee.getAmount());
        }
    }

    private static void showLoanCounts() throws SQLException {
        int min = promptInt("Show members with at least how many loans? ");
        Map<String, Integer> counts = loanDAO.loanCountsPerMember(min);
        if (counts.isEmpty()) {
            System.out.println("No members have that many loans.");
        }
        for (Map.Entry<String, Integer> e : counts.entrySet()) {
            System.out.printf("  %-15s %d loans%n", e.getKey(), e.getValue());
        }
    }

    private static void showMemberHistory() throws SQLException {
        int id = promptInt("Member id: ");
        if (memberDAO.findById(id) == null) {
            System.out.println("No member with id " + id + ".");
            return;
        }
        List<Loan> loans = loanDAO.findByMember(id);
        if (loans.isEmpty()) {
            System.out.println("That member has no loans.");
        }
        for (Loan l : loans) {
            System.out.println(l);
        }
    }

    private static void resetDatabase() throws Exception {
        if (confirm("This erases all changes and reloads the sample data. Continue?")) {
            Database.rebuild();
            System.out.println("Database reset.");
        } else {
            System.out.println("Cancelled.");
        }
    }

    // ------------------------------------------------------------------
    // Input helpers (validation lives here so bad input never crashes the app)
    // ------------------------------------------------------------------
    private static String prompt(String message) {
        System.out.print(message);
        return in.nextLine().trim();
    }

    private static String promptRequired(String message) {
        while (true) {
            String text = prompt(message);
            if (!text.isEmpty()) {
                return text;
            }
            System.out.println("This can't be blank.");
        }
    }

    private static int promptInt(String message) {
        while (true) {
            String text = prompt(message);
            try {
                return Integer.parseInt(text);
            } catch (NumberFormatException e) {
                System.out.println("Please enter a whole number.");
            }
        }
    }

    private static String promptEmail(String message) {
        while (true) {
            String text = prompt(message);
            if (isValidEmail(text)) {
                return text;
            }
            System.out.println("Please enter a valid email, like name@example.com.");
        }
    }

    private static boolean isValidEmail(String text) {
        int at = text.indexOf('@');
        return at > 0 && text.indexOf('.', at) > at + 1 && !text.contains(" ");
    }

    private static boolean confirm(String message) {
        String answer = prompt(message + " (y/n): ");
        return answer.equalsIgnoreCase("y") || answer.equalsIgnoreCase("yes");
    }

    /** Turns raw SQLite error text into something a user can understand. */
    private static String friendlyMessage(SQLException e) {
        String msg = String.valueOf(e.getMessage());
        if (msg.contains("FOREIGN KEY")) {
            return "That record is still linked to other records (for example, a member or "
                    + "book with loan history), so the change was blocked.";
        }
        if (msg.contains("UNIQUE")) {
            return "That value already exists (for example, a duplicate email).";
        }
        if (msg.contains("CHECK")) {
            return "One of the dates breaks a rule (for example, a return before checkout).";
        }
        return msg;
    }
}
