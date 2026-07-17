package src;

import java.util.ArrayList;
import java.util.Scanner;

/**
 * Driver for the Transaction Tallier.
 *
 * Two modes:
 *   - Interactive mode (default): same flow as the original assignment,
 *     but with input validation added (bad file names / non-numeric or
 *     non-positive worker counts no longer crash the program).
 *   - Benchmark mode ("--benchmark"): loads the file once, then re-runs
 *     the tally with several different thread counts back to back,
 *     timing each run and printing a summary table.
 */
public class Main {
    public static void main(String[] args) {
        if (args.length > 0 && args[0].equals("--benchmark")) {
            String file = args.length > 1 ? args[1] : "transactions.txt";
            runBenchmark(file);
        } else {
            runInteractive();
        }
    }

    // ---------------------------------------------------------------
    // Interactive mode
    // ---------------------------------------------------------------
    private static void runInteractive() {
        Scanner sc = new Scanner(System.in);
        System.out.println("[Transaction Tallier]");

        ArrayList<String> data = null;
        while (data == null) {
            System.out.print("Enter file name: ");
            String fileName = sc.nextLine().trim();
            data = TransactionReader.loadTransactions(fileName);
            if (data == null) {
                System.out.println("No such file: \"" + fileName + "\". Try again.");
            }
        }

        System.out.println("Transactions loaded.");
        TransactionTallier tt = new TransactionTallier(data);

        int numWorkers = readPositiveInt(sc, "Create how many workers? ");

        Thread[] threads = new Thread[numWorkers];
        for (int i = 0; i < numWorkers; i++) {
            threads[i] = new Thread(new TallyWorker(tt));
        }

        System.out.println("Workers created. Press 'enter' to start tallying...");
        sc.nextLine();

        System.out.println("Starting workers...");
        long start = System.nanoTime();
        for (Thread t : threads) t.start();

        for (Thread t : threads) {
            try {
                t.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        System.out.println("\nAll workers are done working.");
        System.out.println("Transaction total is $" + tt.getRunningTotal());
        System.out.println("\nCategories and their quantities:");

        for (String key : tt.getCategories().keySet()) {
            System.out.println(key + ": " + tt.getCategories().get(key));
        }

        System.out.printf("%nProcessed in %d ms.%n", elapsedMs);
        System.out.println("Program complete.");
    }

    /** Keeps prompting until the user enters a positive integer. */
    private static int readPositiveInt(Scanner sc, String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = sc.nextLine().trim();
            try {
                int value = Integer.parseInt(input);
                if (value > 0) return value;
                System.out.println("Please enter a positive number.");
            } catch (NumberFormatException e) {
                System.out.println("\"" + input + "\" isn't a valid number. Please try again.");
            }
        }
    }

    // ---------------------------------------------------------------
    // Benchmark mode
    // ---------------------------------------------------------------
    private static void runBenchmark(String fileName) {
        System.out.println("[Benchmark mode] file=\"" + fileName + "\"");

        ArrayList<String> master = TransactionReader.loadTransactions(fileName);
        if (master == null) {
            System.out.println("No such file: \"" + fileName + "\"");
            return;
        }
        System.out.println("Loaded " + master.size() + " transactions.\n");

        int[] threadCounts = {1, 2, 4, 8, 16, 32, 64};
        System.out.printf("%-8s %-12s %-15s%n", "threads", "time (ms)", "txns/sec");
        System.out.println("-".repeat(38));

        for (int n : threadCounts) {
            // Fresh copy each run since transactions are drained during processing.
            ArrayList<String> copy = new ArrayList<>(master);
            TransactionTallier tt = new TransactionTallier(copy);

            Thread[] threads = new Thread[n];
            for (int i = 0; i < n; i++) {
                threads[i] = new Thread(new TallyWorker(tt));
            }

            long start = System.nanoTime();
            for (Thread t : threads) t.start();
            for (Thread t : threads) {
                try {
                    t.join();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            long elapsedMs = (System.nanoTime() - start) / 1_000_000;
            double txnsPerSec = elapsedMs == 0 ? 0 : (master.size() / (elapsedMs / 1000.0));

            System.out.printf("%-8d %-12d %-15.0f%n", n, elapsedMs, txnsPerSec);

            // Sanity check: total should be identical across all runs regardless of thread count.
            if (n == threadCounts[0]) {
                System.out.println("  (transaction total for this run: $" + tt.getRunningTotal() + ")");
            }
        }
    }
}
