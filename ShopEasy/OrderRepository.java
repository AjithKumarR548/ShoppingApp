import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

class OrderRepository {
    private final Path file;

    public OrderRepository(Path file) {
        this.file = file;
    }

    public synchronized List<Order> loadAll() {
        if (!Files.exists(file)) {
            return new ArrayList<>();
        }
        try {
            return Files.readAllLines(file, StandardCharsets.UTF_8).stream()
                    .filter(line -> !line.isBlank())
                    .map(Order::fromRecord)
                    .collect(Collectors.toCollection(ArrayList::new));
        } catch (IOException e) {
            throw new ShopException("Cannot read orders: " + e.getMessage());
        }
    }

    public synchronized void save(Order order) {
        try {
            Files.writeString(file, order.toRecord() + System.lineSeparator(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new ShopException("Cannot save order: " + e.getMessage());
        }
    }

    public String nextId() {
        return String.format("ORD-%04d", loadAll().size() + 1);
    }
}
