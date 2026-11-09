import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

class ShopService {
    private final List<Product> catalog;
    private final OrderRepository repository;
    private final Map<String, Discount> coupons = new LinkedHashMap<>();

    public ShopService(List<Product> catalog, OrderRepository repository) {
        this.catalog = new ArrayList<>(catalog);
        this.repository = repository;
        registerCoupon(new Discount.Percent("WELCOME10", 10));
        registerCoupon(new Discount.Percent("SAVE5", 5));
        registerCoupon(new Discount.Flat("FLAT200", 200, 1000));
    }

    public List<Product> getCatalog() {
        return Collections.unmodifiableList(catalog);
    }

    public Product findById(int id) {
        return catalog.stream().filter(p -> p.getId() == id).findFirst()
                .orElseThrow(() -> new ShopException("Product " + id + " not found"));
    }

    public static Predicate<Product> textFilter(String term) {
        String needle = term == null ? "" : term.trim().toLowerCase();
        return p -> p.getName().toLowerCase().contains(needle)
                || p.getCategory().toLowerCase().contains(needle);
    }

    public static Predicate<Product> categoryFilter(String category) {
        return p -> category == null || category.equals("All") || p.getCategory().equals(category);
    }

    public static Predicate<Product> maxPriceFilter(double maxPrice) {
        return p -> p.getPrice() <= maxPrice;
    }

    public static Predicate<Product> inStockFilter() {
        return p -> p.getStock() > 0;
    }

    public static Comparator<Product> comparatorFor(String sortName) {
        return switch (sortName == null ? "" : sortName) {
            case "Price: low to high" -> Comparator.comparingDouble(Product::getPrice);
            case "Price: high to low" -> Comparator.comparingDouble(Product::getPrice).reversed();
            case "Stock: most first" -> Comparator.comparingInt(Product::getStock).reversed();
            default -> Comparator.comparing(Product::getName, String.CASE_INSENSITIVE_ORDER);
        };
    }

    public List<Product> search(String term, String category, String sortName, boolean inStockOnly) {
        Predicate<Product> filter = textFilter(term).and(categoryFilter(category));
        if (inStockOnly) {
            filter = filter.and(inStockFilter());
        }
        return catalog.stream()
                .filter(filter)
                .sorted(comparatorFor(sortName))
                .collect(Collectors.toList());
    }

    public List<String> categories() {
        return catalog.stream().map(Product::getCategory).distinct().sorted().toList();
    }

    private void registerCoupon(Discount discount) {
        coupons.put(discount.code(), discount);
    }

    public Discount findCoupon(String code) {
        String key = code == null ? "" : code.trim().toUpperCase();
        Discount discount = coupons.get(key);
        if (discount == null) {
            throw new ShopException.InvalidCoupon("Invalid coupon code: " + (key.isEmpty() ? "(empty)" : key));
        }
        return discount;
    }

    public Discount applyCoupon(Cart cart, String code) {
        Discount discount = findCoupon(code);
        if (!discount.isApplicable(cart.subtotal())) {
            throw new ShopException.InvalidCoupon(discount.code() + ": " + discount.description());
        }
        cart.applyDiscount(discount);
        return discount;
    }

    public Order checkout(Cart cart, String customerName) {
        String customer = Validator.requireText(customerName, "Customer name");
        if (cart.isEmpty()) {
            throw new ShopException.Validation("Your cart is empty");
        }
        for (CartItem item : cart.getItems()) {
            if (item.quantity() > item.product().getStock()) {
                throw new ShopException.OutOfStock(item.product().getName(), item.product().getStock());
            }
        }
        for (CartItem item : cart.getItems()) {
            item.product().reduceStock(item.quantity());
        }
        List<Order.Line> lines = cart.getItems().stream()
                .map(i -> new Order.Line(i.product().getName(), i.product().getCategory(),
                        i.quantity(), i.product().getPrice()))
                .toList();
        Order order = new Order(repository.nextId(), customer, LocalDateTime.now(),
                cart.getDiscount().code(), cart.subtotal(), cart.discountAmount(),
                cart.tax(), cart.total(), lines);
        try {
            repository.save(order);
        } catch (RuntimeException e) {
            cart.getItems().forEach(i -> i.product().addStock(i.quantity()));
            throw e;
        }
        cart.clear();
        return order;
    }

    public List<Order> orders() {
        List<Order> all = repository.loadAll();
        Collections.reverse(all);                     // newest first
        return all;
    }

    public Map<String, Double> revenueByCategory() {
        return repository.loadAll().stream()
                .flatMap(o -> o.getLines().stream())
                .collect(Collectors.groupingBy(Order.Line::category, TreeMap::new,
                        Collectors.summingDouble(Order.Line::lineTotal)));
    }

    public List<Map.Entry<String, Integer>> bestSellers(int limit) {
        return repository.loadAll().stream()
                .flatMap(o -> o.getLines().stream())
                .collect(Collectors.groupingBy(Order.Line::productName,
                        Collectors.summingInt(Order.Line::quantity)))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed()
                        .thenComparing(Map.Entry.comparingByKey()))
                .limit(limit)
                .toList();
    }

    public double totalRevenue() {
        return Validator.round2(repository.loadAll().stream().mapToDouble(Order::getTotal).sum());
    }

    public List<Product> lowStock(int threshold) {
        return catalog.stream()
                .filter(p -> p.getStock() <= threshold)
                .sorted(Comparator.comparingInt(Product::getStock).thenComparing(Product::getName))
                .toList();
    }
}
