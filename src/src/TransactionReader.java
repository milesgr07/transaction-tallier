package src;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;

public class TransactionReader {
    public static ArrayList<String> loadTransactions(String filename){
        File f = new File(filename);
        ArrayList<String> transactions = null;

        try(Scanner reader = new Scanner(f)){
            transactions = new ArrayList<>(100_000);

            while(reader.hasNextLine()){
                transactions.add(reader.nextLine());
            }
        }
        catch(FileNotFoundException _){
            System.err.printf("No such file: \"%s\"\n", filename);
        }
        catch(Exception e){
            System.err.println("An unknown error has occurred:");
            System.err.println(e.getMessage());
            return null;
        }

        return transactions;
    }
}
