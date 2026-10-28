package model;

public class Category {
    private int categoryId;
    private String name;

    public Category(int categoryId, String name) {
        this.categoryId = categoryId;
        this.name = name;
    }

    public int getCategoryId() { return categoryId; }
    public String getName() { return name; }

    @Override
    public String toString() {
        return "[" + categoryId + "] " + name;
    }
}
