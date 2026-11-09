abstract class Product {
    private final int id;
    private final String name;
    private final String category;
    private final double price;
    private int stock;

    protected Product(int id, String name, String category, double price, int stock) {
        this.id = id;
        this.name = Validator.requireText(name, "Product name");
        this.category = Validator.requireText(category, "Category");
        this.price = Validator.requireNonNegative(price, "Price");
        this.stock = Validator.requireNonNegative(stock, "Stock");
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public double getPrice() { return price; }
    public int getStock() { return stock; }

    public void reduceStock(int quantity) {
        if (quantity <= 0) {
            throw new ShopException.Validation("Quantity must be at least 1");
        }
        if (quantity > stock) {
            throw new ShopException.OutOfStock(name, stock);
        }
        stock -= quantity;
    }

    public void addStock(int quantity) {
        if (quantity <= 0) {
            throw new ShopException.Validation("Quantity must be at least 1");
        }
        stock += quantity;
    }

    public abstract double taxRate();

    public abstract String describe();

    @Override
    public String toString() {
        return name;
    }
}
