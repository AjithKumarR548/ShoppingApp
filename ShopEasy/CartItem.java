record CartItem(Product product, int quantity) {
    public CartItem {
        if (quantity <= 0) {
            throw new ShopException.Validation("Quantity must be at least 1");
        }
    }

    public CartItem withQuantity(int newQuantity) {
        return new CartItem(product, newQuantity);
    }

    public double lineTotal() {
        return Validator.round2(product.getPrice() * quantity);
    }

    public double lineTax() {
        return Validator.round2(lineTotal() * product.taxRate());   // polymorphic taxRate()
    }
}
