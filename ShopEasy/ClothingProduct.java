class ClothingProduct extends Product {
    private final String size;

    public ClothingProduct(int id, String name, double price, int stock, String size) {
        super(id, name, "Clothing", price, stock);
        this.size = Validator.requireText(size, "Size");
    }

    public String getSize() { return size; }

    @Override
    public double taxRate() { return 0.12; }

    @Override
    public String describe() {
        return String.format("%s%nCategory: Clothing | Price: Rs. %.2f | Tax: 12%%%nSize: %s | In stock: %d",
                getName(), getPrice(), size, getStock());
    }
}
