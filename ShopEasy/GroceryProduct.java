class GroceryProduct extends Product {
    private final String packSize;

    public GroceryProduct(int id, String name, double price, int stock, String packSize) {
        super(id, name, "Grocery", price, stock);
        this.packSize = Validator.requireText(packSize, "Pack size");
    }

    public String getPackSize() { return packSize; }

    @Override
    public double taxRate() { return 0.05; }

    @Override
    public String describe() {
        return String.format("%s%nCategory: Grocery | Price: Rs. %.2f | Tax: 5%%%nPack size: %s | In stock: %d",
                getName(), getPrice(), packSize, getStock());
    }
}
