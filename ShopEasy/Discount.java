interface Discount {
    String code();

    String description();

    double amountFor(double subtotal);

    default boolean isApplicable(double subtotal) {
        return true;
    }

    static Discount none() {
        return new None();
    }

    final class None implements Discount {
        public String code() { return "NONE"; }
        public String description() { return "No coupon"; }
        public double amountFor(double subtotal) { return 0; }
    }

    final class Percent implements Discount {
        private final String code;
        private final double percent;

        public Percent(String code, double percent) {
            this.code = code;
            this.percent = percent;
        }

        public String code() { return code; }
        public String description() { return percent + "% off"; }
        public double amountFor(double subtotal) {
            return Validator.round2(subtotal * percent / 100.0);
        }
    }

    final class Flat implements Discount {
        private final String code;
        private final double amount;
        private final double minOrder;

        public Flat(String code, double amount, double minOrder) {
            this.code = code;
            this.amount = amount;
            this.minOrder = minOrder;
        }

        public String code() { return code; }
        public String description() {
            return "Rs. " + (int) amount + " off on orders of Rs. " + (int) minOrder + " or more";
        }
        @Override
        public boolean isApplicable(double subtotal) { return subtotal >= minOrder; }
        public double amountFor(double subtotal) {
            return isApplicable(subtotal) ? Math.min(amount, subtotal) : 0;
        }
    }
}
