package model;

public class Book {
    private int bookId;
    private String title;
    private String author;
    private Integer categoryId;     // null if the book has no category
    private String categoryName;    // filled in by the JOIN, for display only
    private boolean available = true;

    public Book(int bookId, String title, String author, Integer categoryId) {
        this.bookId = bookId;
        this.title = title;
        this.author = author;
        this.categoryId = categoryId;
    }

    public int getBookId() { return bookId; }
    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public Integer getCategoryId() { return categoryId; }
    public String getCategoryName() { return categoryName; }
    public boolean isAvailable() { return available; }

    public void setTitle(String title) { this.title = title; }
    public void setAuthor(String author) { this.author = author; }
    public void setCategoryId(Integer categoryId) { this.categoryId = categoryId; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public void setAvailable(boolean available) { this.available = available; }

    @Override
    public String toString() {
        return String.format("[%2d] %-26s %-22s %-16s %s",
                bookId, title, author,
                categoryName == null ? "(none)" : categoryName,
                available ? "Available" : "Checked out");
    }
}
