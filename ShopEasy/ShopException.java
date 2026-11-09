class ShopException extends RuntimeException {
    public ShopException(String message) {
        super(message);
    }

    public static class Validation extends ShopException {
        public Validation(String message) {
            super(message);
        }
    }

    public static class OutOfStock extends ShopException {
        public OutOfStock(String productName, int available) {
            super(available == 0
                    ? productName + " is out of stock"
                    : "Only " + available + " unit(s) of " + productName + " in stock");
        }
    }

    public static class InvalidCoupon extends ShopException {
        public InvalidCoupon(String message) {
            super(message);
        }
    }
}
