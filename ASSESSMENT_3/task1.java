import java.io.File;
import java.util.Scanner;

public class ATMSimulationErrorCodes {
    // Error codes: 0 = OK, -1 = Missing file, -2 = Non-numeric, -3 = Non-positive, -4 = Insufficient funds
    public static int withdrawWithErrorCodes(String cardFile, double balance, Scanner sc) {
        File f = new File(cardFile);
        if (!f.exists() || !f.canRead()) return -1;

        System.out.print("Enter Withdrawal Amount: ");
        if (!sc.hasNextDouble()) {
            sc.next();
            return -2;
        }
        double amount = sc.nextDouble();
        if (amount <= 0) return -3;
        if (amount > balance) return -4;

        System.out.printf("Dispensing cash: Rs. %.2f%nRemaining Balance: Rs. %.2f%n",
                          amount, balance - amount);
        return 0;
    }

    public static void main(String[] args) {
        try (Scanner sc = new Scanner(System.in)) {
            int code = withdrawWithErrorCodes("card_data.txt", 5000.0, sc);
            if (code != 0) {
                System.out.println("Withdrawal failed with error code: " + code);
            }
        }
    }
}