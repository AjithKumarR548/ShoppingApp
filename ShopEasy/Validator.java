final class Validator {
    private Validator() { }

    public static String requireText(String value, String label) {
        if (value == null || value.trim().isEmpty()) {
            throw new ShopException.Validation(label + " is required");
        }
        return value.trim();
    }

    public static double requireNonNegative(double value, String label) {
        if (Double.isNaN(value) || value < 0) {
            throw new ShopException.Validation(label + " cannot be negative");
        }
        return value;
    }

    public static int requireNonNegative(int value, String label) {
        if (value < 0) {
            throw new ShopException.Validation(label + " cannot be negative");
        }
        return value;
    }

    public static int parsePositiveInt(String text, String label) {
        if (text == null) {
            throw new ShopException.Validation(label + " must be a whole number");
        }
        int number;
        try {
            number = Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            throw new ShopException.Validation(label + " must be a whole number");
        }
        if (number <= 0) {
            throw new ShopException.Validation(label + " must be greater than zero");
        }
        return number;
    }

    public static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
