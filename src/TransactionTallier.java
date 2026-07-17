
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Holds the shared state that multiple TallyWorker threads read from and
 * write to: the remaining transactions, the running dollar total, and a
 * count of items seen per category.
 *
 * This class is unchanged from the original assignment version, except for
 * the addition of getCategories()'s Javadoc and no behavioral changes.
 */
public class TransactionTallier {
    private final ArrayList<String> transactions;
    private final HashMap<String, Integer> categories;
    private int runningTotal;

    public TransactionTallier(ArrayList<String> transactions) {
        this.transactions = transactions;
        this.runningTotal = 0;
        this.categories = new HashMap<>();
    }

    public synchronized void addToTotal(int amount) {
        runningTotal += amount;
    }

    public int getRunningTotal() {
        return runningTotal;
    }

    public int getTransactionListSize() {
        return transactions.size();
    }

    public synchronized String getNextTransaction() {
        if (transactions.isEmpty()) return null;
        return transactions.remove(0);
    }

    public synchronized void updateCategories(String item, int quantity) {
        categories.put(item, categories.getOrDefault(item, 0) + quantity);
    }

    public HashMap<String, Integer> getCategories() {
        return categories;
    }
}
