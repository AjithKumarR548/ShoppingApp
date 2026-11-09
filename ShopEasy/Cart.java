import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

class Cart {
    private final Map<Integer, CartItem> items = new LinkedHashMap<>();
    private Discount discount = Discount.none();

    public void add(Product product, int quantity) {
        if (quantity <= 0) {
            throw new ShopException.Validation("Quantity must be at least 1");
        }
        CartItem existing = items.get(product.getId());
        int newQuantity = quantity + (existing == null ? 0 : existing.quantity());
        if (newQuantity > product.getStock()) {
            throw new ShopException.OutOfStock(product.getName(), product.getStock());
        }
        items.put(product.getId(), new CartItem(product, newQuantity));
    }

    public void setQuantity(int productId, int quantity) {
        CartItem existing = items.get(productId);
        if (existing == null) {
            return;
        }
        if (quantity <= 0) {
            items.remove(productId);
            return;
        }
        if (quantity > existing.product().getStock()) {
            throw new ShopException.OutOfStock(existing.product().getName(), existing.product().getStock());
        }
        items.put(productId, existing.withQuantity(quantity));
    }

    public void remove(int productId) {
        items.remove(productId);
    }

    public void clear() {
        items.clear();
        discount = Discount.none();
    }

    public List<CartItem> getItems() {
        return List.copyOf(items.values());
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public int itemCount() {
        return items.values().stream().mapToInt(CartItem::quantity).sum();
    }

    public Discount getDiscount() {
        return discount;
    }

    public void applyDiscount(Discount discount) {
        this.discount = discount;
    }

    public double subtotal() {
        return Validator.round2(items.values().stream().mapToDouble(CartItem::lineTotal).sum());
    }

    public double discountAmount() {
        return discount.amountFor(subtotal());
    }

    public double tax() {
        double subtotal = subtotal();
        double ratio = subtotal == 0 ? 0 : discountAmount() / subtotal;
        double rawTax = items.values().stream().mapToDouble(CartItem::lineTax).sum();
        return Validator.round2(rawTax * (1 - ratio));
    }

    public double total() {
        return Validator.round2(subtotal() - discountAmount() + tax());
    }
}
