import java.io.*;
import java.util.logging.*;

public class DailySalesReportManual {
    private static final Logger LOGGER = Logger.getLogger(DailySalesReportManual.class.getName());

    public static void generateReport(String[] storeFiles, String summaryFile) {
        BufferedWriter writer = null;
        int openHandles = 0;
        try {
            writer = new BufferedWriter(new FileWriter(summaryFile));
            openHandles++;
            double grandTotal = 0;

            for (String file : storeFiles) {
                BufferedReader reader = null;
                try {
                    reader = new BufferedReader(new FileReader(file));
                    openHandles++;
                    String line = reader.readLine();
                    double sales = Double.parseDouble(line.trim()); // Throws on corrupt file
                    grandTotal += sales;
                    writer.write(file + ": " + sales);
                    writer.newLine();
                    reader.close(); // Skipped if parseDouble throws -> LEAK!
                    openHandles--;
                } catch (NumberFormatException e) {
                    LOGGER.severe("Corrupt data in file: " + file + " -> " + e.getMessage());
                }
            }
            writer.write("GRAND TOTAL: " + grandTotal);
        } catch (IOException e) {
            LOGGER.severe("I/O Failure: " + e.getMessage());
        } finally {
            System.out.println("[Manual Version] Open handles before finally: " + openHandles);
            if (writer != null) {
                try {
                    writer.close();
                    openHandles--;
                } catch (IOException ignored) {}
            }
            System.out.println("[Manual Version] Leaked handles remaining after exit: " + openHandles);
        }
    }

    public static void main(String[] args) throws IOException {
        try (PrintWriter p1 = new PrintWriter("store1.txt");
             PrintWriter p2 = new PrintWriter("store2.txt");
             PrintWriter p3 = new PrintWriter("store3.txt")) {
            p1.println("1500.50");
            p2.println("INVALID_AMOUNT");
            p3.println("2450.00");
        }
        generateReport(new String[]{"store1.txt", "store2.txt", "store3.txt"}, "summary_manual.txt");
    }
}