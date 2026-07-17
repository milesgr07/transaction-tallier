package src;

/**
 * A worker that repeatedly pulls transactions from a shared TransactionTallier
 * and updates its running total and category counts.
 *
 * Expected line format: "<type> <item name...> <quantity> @ $<price>"
 * e.g. "buy copper wires 240 @ $1"
 */
public class TallyWorker implements Runnable {
    private final TransactionTallier tt;

    public TallyWorker(TransactionTallier tt) {
        this.tt = tt;
    }

    @Override
    public void run() {
        while (true) {
            String transaction = tt.getNextTransaction();
            if (transaction == null) break;

            try {
                processTransaction(transaction);
            } catch (Exception e) {
                // Don't let one malformed line crash the whole worker thread.
                System.err.println("Skipping malformed transaction: \"" + transaction + "\" (" + e.getMessage() + ")");
            }
        }
    }

    private void processTransaction(String transaction) {
        String[] parts = transaction.trim().split("\\s+");

        if (parts.length < 5) {
            throw new IllegalArgumentException("expected at least 5 tokens, got " + parts.length);
        }

        // Fixed positions relative to the end of the line: ... <qty> @ $<price>
        String type = parts[0];
        String atSymbol = parts[parts.length - 2];
        String priceToken = parts[parts.length - 1];
        String qtyToken = parts[parts.length - 3];

        if (!atSymbol.equals("@")) {
            throw new IllegalArgumentException("expected '@' at position -2, found \"" + atSymbol + "\"");
        }
        if (!priceToken.startsWith("$")) {
            throw new IllegalArgumentException("expected price to start with '$', found \"" + priceToken + "\"");
        }

        int quantity = Integer.parseInt(qtyToken);
        int price = Integer.parseInt(priceToken.substring(1));

        // Everything between the type and the quantity is the item name.
        StringBuilder nameBuilder = new StringBuilder();
        for (int i = 1; i < parts.length - 3; i++) {
            if (i > 1) nameBuilder.append(' ');
            nameBuilder.append(parts[i]);
        }
        String itemName = nameBuilder.toString();

        if (itemName.isEmpty()) {
            throw new IllegalArgumentException("no item name found");
        }

        int cost = quantity * price;

        if (type.equalsIgnoreCase("sell")) {
            tt.addToTotal(cost);
        } else if (type.equalsIgnoreCase("buy")) {
            tt.addToTotal(-cost);
        } else {
            throw new IllegalArgumentException("unknown transaction type \"" + type + "\"");
        }

        tt.updateCategories(itemName, quantity);
    }
}
