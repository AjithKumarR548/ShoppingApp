import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

class Order {
    public record Line(String productName, String category, int quantity, double unitPrice) {
        public double lineTotal() {
            return Validator.round2(quantity * unitPrice);
        }
    }

    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final String id;
    private final String customer;
    private final LocalDateTime placedAt;
    private final String couponCode;
    private final double subtotal;
    private final double discount;
    private final double tax;
    private final double total;
    private final List<Line> lines;

    public Order(String id, String customer, LocalDateTime placedAt, String couponCode,
                 double subtotal, double discount, double tax, double total, List<Line> lines) {
        this.id = id;
        this.customer = customer;
        this.placedAt = placedAt;
        this.couponCode = couponCode;
        this.subtotal = subtotal;
        this.discount = discount;
        this.tax = tax;
        this.total = total;
        this.lines = List.copyOf(lines);
    }

    public String getId() { return id; }
    public String getCustomer() { return customer; }
    public LocalDateTime getPlacedAt() { return placedAt; }
    public String getCouponCode() { return couponCode; }
    public double getSubtotal() { return subtotal; }
    public double getDiscount() { return discount; }
    public double getTax() { return tax; }
    public double getTotal() { return total; }
    public List<Line> getLines() { return lines; }

    public int itemCount() {
        return lines.stream().mapToInt(Line::quantity).sum();
    }

    public String toRecord() {
        String items = lines.stream()
                .map(l -> clean(l.productName()) + "~" + clean(l.category()) + "~" + l.quantity() + "~" + l.unitPrice())
                .collect(Collectors.joining(";"));
        return String.join("|", id, clean(customer), placedAt.format(FORMAT), couponCode,
                String.valueOf(subtotal), String.valueOf(discount), String.valueOf(tax),
                String.valueOf(total), items);
    }

    public static Order fromRecord(String record) {
        String[] f = record.split("\\|", -1);
        if (f.length != 9) {
            throw new ShopException("Corrupt order record: " + record);
        }
        List<Line> lines = Arrays.stream(f[8].split(";"))
                .filter(s -> !s.isEmpty())
                .map(s -> {
                    String[] p = s.split("~");
                    return new Line(p[0], p[1], Integer.parseInt(p[2]), Double.parseDouble(p[3]));
                })
                .toList();
        return new Order(f[0], f[1], LocalDateTime.parse(f[2], FORMAT), f[3],
                Double.parseDouble(f[4]), Double.parseDouble(f[5]),
                Double.parseDouble(f[6]), Double.parseDouble(f[7]), lines);
    }

    private static String clean(String text) {
        return text.replaceAll("[|;~\\r\\n]", " ");
    }

    public String toReceipt() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("ORDER %s%n", id));
        sb.append(String.format("Customer : %s%n", customer));
        sb.append(String.format("Placed on: %s%n", placedAt.format(FORMAT)));
        sb.append("------------------------------------------------------\n");
        for (Line l : lines) {
            sb.append(String.format("%-26s %3d x %9.2f = %10.2f%n",
                    shorten(l.productName(), 26), l.quantity(), l.unitPrice(), l.lineTotal()));
        }
        sb.append("------------------------------------------------------\n");
        sb.append(String.format("%-38s %15.2f%n", "Subtotal", subtotal));
        sb.append(String.format("%-38s %15.2f%n", "Discount (" + couponCode + ")", -discount));
        sb.append(String.format("%-38s %15.2f%n", "Tax", tax));
        sb.append(String.format("%-38s %15.2f%n", "TOTAL (Rs.)", total));
        return sb.toString();
    }

    private static String shorten(String text, int max) {
        return text.length() <= max ? text : text.substring(0, max - 1) + ".";
    }
}
