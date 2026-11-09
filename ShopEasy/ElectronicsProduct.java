class ElectronicsProduct extends Product {
    private final int warrantyMonths;

    public ElectronicsProduct(int id, String name, double price, int stock, int warrantyMonths) {
        super(id, name, "Electronics", price, stock);
        this.warrantyMonths = Validator.requireNonNegative(warrantyMonths, "Warranty");
    }

    public int getWarrantyMonths() { return warrantyMonths; }

    @Override
    public double taxRate() { return 0.18; }

    @Override
    public String describe() {
        return String.format("%s%nCategory: Electronics | Price: Rs. %.2f | Tax: 18%%%nWarranty: %d months | In stock: %d",
                getName(), getPrice(), warrantyMonths, getStock());
    }
}
