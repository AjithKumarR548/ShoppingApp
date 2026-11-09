import java.util.List;

final class SampleData {
    private SampleData() { }

    public static List<Product> catalog() {
        return List.of(
                new ElectronicsProduct(1, "Wireless Earbuds", 2499, 15, 12),
                new ElectronicsProduct(2, "Smartphone X", 14999, 8, 12),
                new ElectronicsProduct(3, "Laptop Pro 14", 54999, 4, 24),
                new ElectronicsProduct(4, "Power Bank 20000mAh", 1799, 25, 6),
                new ElectronicsProduct(5, "Smartwatch Fit", 3999, 2, 12),
                new ClothingProduct(6, "Cotton T-Shirt", 499, 40, "M"),
                new ClothingProduct(7, "Denim Jeans", 1299, 20, "32"),
                new ClothingProduct(8, "Winter Hoodie", 1499, 12, "L"),
                new ClothingProduct(9, "Running Shoes", 2199, 10, "9"),
                new GroceryProduct(10, "Basmati Rice", 649, 30, "5 kg"),
                new GroceryProduct(11, "Olive Oil", 799, 18, "1 L"),
                new GroceryProduct(12, "Green Tea 100 Bags", 349, 50, "100 bags"),
                new GroceryProduct(13, "Roasted Almonds", 599, 0, "500 g"));
    }
}
