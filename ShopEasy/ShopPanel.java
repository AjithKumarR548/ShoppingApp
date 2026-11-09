import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

class ShopPanel extends JPanel {
    static boolean testMode = false;
    String lastMessage = "";
    String lastReceipt = "";

    private static final Color PRIMARY = new Color(0x1F3A5F);
    private static final Color ACCENT = new Color(0xF28C28);
    private static final DateTimeFormatter SHORT_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final ShopService service;
    private final Cart cart = new Cart();
    private List<Order> shownOrders = List.of();

    final JTextField searchField = new JTextField(16);
    final JComboBox<String> categoryBox = new JComboBox<>();
    final JComboBox<String> sortBox = new JComboBox<>(new String[]{
            "Name (A-Z)", "Price: low to high", "Price: high to low", "Stock: most first"});
    final JCheckBox inStockOnly = new JCheckBox("In stock only");
    final DefaultTableModel productModel = readOnlyModel("ID", "Name", "Category", "Price (Rs.)", "Stock", "Status");
    final JTable productTable = new JTable(productModel);
    final DefaultTableModel cartModel = readOnlyModel("ID", "Item", "Qty", "Unit (Rs.)", "Total (Rs.)");
    final JTable cartTable = new JTable(cartModel);
    final JSpinner qtySpinner = new JSpinner(new SpinnerNumberModel(1, 1, 99, 1));
    final JButton addButton = new JButton("Add to cart");
    final JButton updateButton = new JButton("Update quantity");
    final JButton removeButton = new JButton("Remove");
    final JButton clearButton = new JButton("Clear cart");
    final JTextField couponField = new JTextField(10);
    final JButton couponButton = new JButton("Apply");
    final JTextField nameField = new JTextField(14);
    final JButton checkoutButton = new JButton("Place order");
    final JLabel subtotalLabel = new JLabel("", SwingConstants.RIGHT);
    final JLabel discountLabel = new JLabel("", SwingConstants.RIGHT);
    final JLabel taxLabel = new JLabel("", SwingConstants.RIGHT);
    final JLabel totalLabel = new JLabel("", SwingConstants.RIGHT);
    final DefaultTableModel orderModel = readOnlyModel("Order ID", "Date", "Customer", "Items", "Coupon", "Total (Rs.)");
    final JTable orderTable = new JTable(orderModel);
    final JTextArea orderDetails = new JTextArea(12, 50);
    final JTextArea reportArea = new JTextArea();
    final JTabbedPane tabs = new JTabbedPane();
    private final JLabel statusLabel = new JLabel(" Ready");

    public ShopPanel(ShopService service) {
        this.service = service;
        setLayout(new BorderLayout());

        add(buildHeader(), BorderLayout.NORTH);
        tabs.addTab("Shop", buildShopTab());
        tabs.addTab("My Orders", buildOrdersTab());
        tabs.addTab("Reports", buildReportsTab());
        add(tabs, BorderLayout.CENTER);
        statusLabel.setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 8));
        add(statusLabel, BorderLayout.SOUTH);

        categoryBox.addItem("All");
        service.categories().forEach(categoryBox::addItem);
        wireEvents();                       // listeners are attached after the boxes are filled

        refreshProducts();
        refreshCart();
        refreshOrders();
        refreshReports();
        setStatus("Welcome to ShopEasy");
        setPreferredSize(new Dimension(1200, 720));
    }

    private JComponent buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(PRIMARY);
        header.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
        JLabel title = new JLabel("ShopEasy");
        title.setForeground(Color.WHITE);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 24f));
        JLabel tagline = new JLabel("Search, compare and buy in one place");
        tagline.setForeground(new Color(0xD6E0F0));
        header.add(title, BorderLayout.WEST);
        header.add(tagline, BorderLayout.EAST);
        return header;
    }

    private JComponent buildShopTab() {
        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        filters.add(new JLabel("Search:"));
        filters.add(searchField);
        filters.add(new JLabel("Category:"));
        filters.add(categoryBox);
        filters.add(new JLabel("Sort by:"));
        filters.add(sortBox);
        filters.add(inStockOnly);

        configureTable(productTable);
        productTable.setDefaultRenderer(Object.class, new StockAwareRenderer());
        int[] widths = {40, 190, 90, 90, 55, 100};
        for (int i = 0; i < widths.length; i++) {
            productTable.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }

        JPanel addBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        addBar.add(new JLabel("Quantity:"));
        addBar.add(qtySpinner);
        addBar.add(addButton);
        addBar.add(new JLabel("(double-click a product for details)"));

        JPanel left = new JPanel(new BorderLayout(0, 4));
        left.setBorder(BorderFactory.createTitledBorder("Products"));
        left.add(filters, BorderLayout.NORTH);
        left.add(new JScrollPane(productTable), BorderLayout.CENTER);
        left.add(addBar, BorderLayout.SOUTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, buildCartPanel());
        split.setResizeWeight(0.6);
        split.setDividerLocation(700);
        return split;
    }

    private JComponent buildCartPanel() {
        configureTable(cartTable);
        DefaultTableCellRenderer right = new DefaultTableCellRenderer();
        right.setHorizontalAlignment(SwingConstants.RIGHT);
        for (int i = 2; i <= 4; i++) {
            cartTable.getColumnModel().getColumn(i).setCellRenderer(right);
        }
        int[] widths = {35, 170, 40, 80, 90};
        for (int i = 0; i < widths.length; i++) {
            cartTable.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        buttons.add(updateButton);
        buttons.add(removeButton);
        buttons.add(clearButton);

        JPanel coupon = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        coupon.add(new JLabel("Coupon:"));
        coupon.add(couponField);
        coupon.add(couponButton);

        JPanel totals = new JPanel(new GridLayout(4, 2, 4, 2));
        totals.add(new JLabel("Subtotal"));
        totals.add(subtotalLabel);
        totals.add(new JLabel("Discount"));
        totals.add(discountLabel);
        totals.add(new JLabel("Tax (GST)"));
        totals.add(taxLabel);
        JLabel totalText = new JLabel("TOTAL");
        totalText.setFont(totalText.getFont().deriveFont(Font.BOLD, 16f));
        totalLabel.setFont(totalLabel.getFont().deriveFont(Font.BOLD, 16f));
        totals.add(totalText);
        totals.add(totalLabel);
        totals.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

        JPanel buyer = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        buyer.add(new JLabel("Your name:"));
        buyer.add(nameField);
        checkoutButton.setBackground(ACCENT);
        checkoutButton.setForeground(Color.BLACK);
        checkoutButton.setFont(checkoutButton.getFont().deriveFont(Font.BOLD));
        buyer.add(checkoutButton);

        JPanel south = new JPanel();
        south.setLayout(new BoxLayout(south, BoxLayout.Y_AXIS));
        for (JComponent part : new JComponent[]{buttons, coupon, totals, buyer}) {
            part.setAlignmentX(Component.LEFT_ALIGNMENT);
            south.add(part);
        }

        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.setBorder(BorderFactory.createTitledBorder("Your cart"));
        panel.add(new JScrollPane(cartTable), BorderLayout.CENTER);
        panel.add(south, BorderLayout.SOUTH);
        panel.setMinimumSize(new Dimension(430, 300));
        return panel;
    }

    private JComponent buildOrdersTab() {
        configureTable(orderTable);
        orderDetails.setEditable(false);
        orderDetails.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JButton refresh = new JButton("Refresh");
        refresh.addActionListener(e -> refreshOrders());
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(refresh);
        top.add(new JLabel("Select an order to see its receipt"));
        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        panel.add(top, BorderLayout.NORTH);
        panel.add(new JScrollPane(orderTable), BorderLayout.CENTER);
        JScrollPane details = new JScrollPane(orderDetails);
        details.setBorder(BorderFactory.createTitledBorder("Receipt"));
        panel.add(details, BorderLayout.SOUTH);
        return panel;
    }

    private JComponent buildReportsTab() {
        reportArea.setEditable(false);
        reportArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        reportArea.setMargin(new Insets(10, 12, 10, 12));
        JButton refresh = new JButton("Refresh reports");
        refresh.addActionListener(e -> refreshReports());
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(refresh);
        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        panel.add(top, BorderLayout.NORTH);
        panel.add(new JScrollPane(reportArea), BorderLayout.CENTER);
        return panel;
    }

    private static void configureTable(JTable table) {
        table.setRowHeight(25);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setReorderingAllowed(false);
        table.setFillsViewportHeight(true);
    }

    private static DefaultTableModel readOnlyModel(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    private void wireEvents() {
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { refreshProducts(); }
            public void removeUpdate(DocumentEvent e) { refreshProducts(); }
            public void changedUpdate(DocumentEvent e) { refreshProducts(); }
        });
        categoryBox.addActionListener(e -> refreshProducts());
        sortBox.addActionListener(e -> refreshProducts());
        inStockOnly.addActionListener(e -> refreshProducts());

        addButton.addActionListener(e -> onAddToCart());
        updateButton.addActionListener(e -> onUpdateQuantity());
        removeButton.addActionListener(e -> onRemove());
        clearButton.addActionListener(e -> {
            cart.clear();
            refreshCart();
            setStatus("Cart cleared");
        });
        couponButton.addActionListener(e -> onApplyCoupon());
        checkoutButton.addActionListener(e -> onCheckout());

        productTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    showDetails(productTable.getSelectedRow());
                }
            }
        });
        orderTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                showSelectedOrder();
            }
        });
        tabs.addChangeListener(e -> {
            if (tabs.getSelectedIndex() == 1) refreshOrders();
            if (tabs.getSelectedIndex() == 2) refreshReports();
        });

        cartTable.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "removeItem");
        cartTable.getActionMap().put("removeItem", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { onRemove(); }
        });
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK), "focusSearch");
        getActionMap().put("focusSearch", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                tabs.setSelectedIndex(0);
                searchField.requestFocusInWindow();
                searchField.selectAll();
            }
        });
    }

    void onAddToCart() {
        int row = productTable.getSelectedRow();
        if (row < 0) {
            notifyUser("Select a product", "Please select a product in the table first.", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        try {
            Product product = service.findById((int) productModel.getValueAt(row, 0));
            int quantity = (int) qtySpinner.getValue();
            cart.add(product, quantity);
            refreshCart();
            setStatus("Added " + quantity + " x " + product.getName() + " to the cart");
        } catch (ShopException ex) {
            notifyUser("Cannot add to cart", ex.getMessage(), JOptionPane.WARNING_MESSAGE);
        }
    }

    void onUpdateQuantity() {
        int row = cartTable.getSelectedRow();
        if (row < 0) {
            notifyUser("Select an item", "Please select an item in the cart first.", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        try {
            cart.setQuantity((int) cartModel.getValueAt(row, 0), (int) qtySpinner.getValue());
            refreshCart();
            setStatus("Cart updated");
        } catch (ShopException ex) {
            notifyUser("Cannot update quantity", ex.getMessage(), JOptionPane.WARNING_MESSAGE);
        }
    }

    void onRemove() {
        int row = cartTable.getSelectedRow();
        if (row < 0) {
            notifyUser("Select an item", "Please select an item in the cart first.", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        cart.remove((int) cartModel.getValueAt(row, 0));
        refreshCart();
        setStatus("Item removed from the cart");
    }

    void onApplyCoupon() {
        try {
            Discount discount = service.applyCoupon(cart, couponField.getText());
            refreshCart();
            setStatus("Coupon " + discount.code() + " applied: " + discount.description());
        } catch (ShopException ex) {
            notifyUser("Coupon", ex.getMessage(), JOptionPane.WARNING_MESSAGE);
        }
    }

    void onCheckout() {
        try {
            Validator.requireText(nameField.getText(), "Customer name");
            if (cart.isEmpty()) {
                throw new ShopException.Validation("Your cart is empty");
            }
            if (!confirm("Confirm order", "Place this order for Rs. " + money(cart.total()) + "?")) {
                return;
            }
            Order order = service.checkout(cart, nameField.getText());
            lastReceipt = order.toReceipt();
            couponField.setText("");
            refreshProducts();
            refreshCart();
            refreshOrders();
            refreshReports();
            setStatus("Order " + order.getId() + " placed successfully");
            if (!testMode) {
                showReceipt(lastReceipt);
            }
        } catch (ShopException ex) {
            notifyUser("Cannot place order", ex.getMessage(), JOptionPane.WARNING_MESSAGE);
        }
    }

    void showDetails(int row) {
        if (row < 0) {
            return;
        }
        Product product = service.findById((int) productModel.getValueAt(row, 0));
        notifyUser("Product details", product.describe(), JOptionPane.INFORMATION_MESSAGE);   // polymorphism
    }

    private void showSelectedOrder() {
        int row = orderTable.getSelectedRow();
        orderDetails.setText(row >= 0 && row < shownOrders.size() ? shownOrders.get(row).toReceipt() : "");
        orderDetails.setCaretPosition(0);
    }

    void refreshProducts() {
        List<Product> products = service.search(searchField.getText(),
                (String) categoryBox.getSelectedItem(), (String) sortBox.getSelectedItem(),
                inStockOnly.isSelected());
        productModel.setRowCount(0);
        for (Product p : products) {
            productModel.addRow(new Object[]{p.getId(), p.getName(), p.getCategory(),
                    money(p.getPrice()), p.getStock(), stockStatus(p)});
        }
        setStatus("Showing " + products.size() + " of " + service.getCatalog().size() + " products");
    }

    void refreshCart() {
        cartModel.setRowCount(0);
        for (CartItem item : cart.getItems()) {
            Product p = item.product();
            cartModel.addRow(new Object[]{p.getId(), p.getName(), item.quantity(),
                    money(p.getPrice()), money(item.lineTotal())});
        }
        subtotalLabel.setText("Rs. " + money(cart.subtotal()));
        String code = cart.getDiscount().code();
        discountLabel.setText("- Rs. " + money(cart.discountAmount()) + ("NONE".equals(code) ? "" : "  (" + code + ")"));
        taxLabel.setText("Rs. " + money(cart.tax()));
        totalLabel.setText("Rs. " + money(cart.total()));
    }

    void refreshOrders() {
        shownOrders = service.orders();
        orderModel.setRowCount(0);
        for (Order o : shownOrders) {
            orderModel.addRow(new Object[]{o.getId(), o.getPlacedAt().format(SHORT_DATE), o.getCustomer(),
                    o.itemCount(), o.getCouponCode(), money(o.getTotal())});
        }
        orderDetails.setText("");
    }

    void refreshReports() {
        StringBuilder sb = new StringBuilder();
        sb.append("SALES BY CATEGORY (before tax and discount)\n");
        var byCategory = service.revenueByCategory();
        if (byCategory.isEmpty()) {
            sb.append("  No orders yet\n");
        }
        byCategory.forEach((k, v) -> sb.append(String.format(Locale.US, "  %-14s Rs. %,12.2f%n", k, v)));
        sb.append("\nBEST SELLERS (units sold)\n");
        var best = service.bestSellers(5);
        if (best.isEmpty()) {
            sb.append("  No orders yet\n");
        }
        best.forEach(e -> sb.append(String.format(Locale.US, "  %-24s %3d%n", e.getKey(), e.getValue())));
        sb.append(String.format(Locale.US, "%nTOTAL REVENUE (all orders, including tax)%n  Rs. %,.2f%n", service.totalRevenue()));
        sb.append("\nLOW STOCK (3 units or fewer)\n");
        service.lowStock(3).forEach(p -> sb.append(String.format(Locale.US, "  %-24s %3d left%n", p.getName(), p.getStock())));
        reportArea.setText(sb.toString());
        reportArea.setCaretPosition(0);
    }

    private static String stockStatus(Product p) {
        if (p.getStock() == 0) return "Out of stock";
        if (p.getStock() <= 3) return "Only " + p.getStock() + " left";
        return "In stock";
    }

    static String money(double value) {
        return String.format(Locale.US, "%,.2f", value);
    }

    void setStatus(String message) {
        statusLabel.setText(" " + message.replace('\n', ' '));
    }

    private void notifyUser(String title, String message, int type) {
        lastMessage = message;
        setStatus(message);
        if (!testMode) {
            JOptionPane.showMessageDialog(this, message, title, type);
        }
    }

    private boolean confirm(String title, String message) {
        return testMode || JOptionPane.showConfirmDialog(this, message, title,
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
    }

    private void showReceipt(String receipt) {
        JTextArea area = new JTextArea(receipt);
        area.setEditable(false);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JOptionPane.showMessageDialog(this, new JScrollPane(area), "Order placed - thank you!",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private static class StockAwareRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (!isSelected) {
                int stock = (int) table.getModel().getValueAt(row, 4);
                c.setForeground(stock == 0 ? Color.GRAY : stock <= 3 ? new Color(0xB45F06) : Color.BLACK);
                c.setBackground(stock == 0 ? new Color(0xEEEEEE) : Color.WHITE);
            }
            setHorizontalAlignment(column == 3 || column == 4 ? SwingConstants.RIGHT : SwingConstants.LEFT);
            return c;
        }
    }
}
