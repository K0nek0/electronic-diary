package planner.models;

public class Category {
    private int id;
    private String name;
    
    // Конструкторы, геттеры и сеттеры
    public Category() {}
    
    public Category(String name) {
        this.name = name;
    }
    
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    @Override
    public String toString() {
        return name;
    }
}
