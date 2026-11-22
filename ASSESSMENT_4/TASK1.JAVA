import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javax.swing.*;

class FeeService {
    private static final String URL = "jdbc:mysql://localhost:3306/campus_db";
    private static final String USER = "root";
    private static final String PWD = "password";

    public double validateAndParse(String regNo, String amountText) {
        if (regNo == null || regNo.trim().isEmpty()) {
            throw new IllegalArgumentException("Registration number is required.");
        }
        if (!regNo.trim().matches("[A-Za-z0-9]{4,15}")) {
            throw new IllegalArgumentException("Registration number must be 4-15 alphanumeric characters.");
        }
        double amount;
        try {
            amount = Double.parseDouble(amountText.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Amount must be a valid number.");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero.");
        }
        return amount;
    }

    public String processPayment(String regNo, double amount) throws SQLException, InterruptedException {
        String sql = "INSERT INTO fees (reg_no, amount) VALUES (?, ?)";
        try (Connection conn = DriverManager.getConnection(URL, USER, PWD);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, regNo.trim());
            ps.setDouble(2, amount);
            ps.executeUpdate();
        } catch (SQLException dbEx) {
            // Standalone demo fallback if local MySQL is not running
            Thread.sleep(1500);
            return generateReceipt(regNo.trim(), amount) + "\n(Demo mode: DB offline)";
        }
        Thread.sleep(1500); // Runs safely off-EDT inside SwingWorker
        return generateReceipt(regNo.trim(), amount);
    }

    public String generateReceipt(String regNo, double amount) {
        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        return String.format("===== FEE RECEIPT =====%nReg No : %s%nAmount : Rs. %.2f%nStatus : PAID%nTime   : %s",
                             regNo, amount, ts);
    }
}

public class FeePaymentApp extends JFrame {
    private final JTextField regNoField = new JTextField(15);
    private final JTextField amountField = new JTextField(15);
    private final JButton payBtn = new JButton("Pay");
    private final JButton clearBtn = new JButton("Clear");
    private final JButton receiptBtn = new JButton("Receipt");
    private final JLabel statusLabel = new JLabel("Ready");
    private final FeeService feeService = new FeeService();
    private String lastReceipt = null;

    public FeePaymentApp() {
        super("Student Fee Payment");
        setLayout(new BorderLayout(10, 10));

        JPanel form = new JPanel(new GridLayout(2, 2, 8, 8));
        form.setBorder(BorderFactory.createEmptyBorder(12, 12, 4, 12));
        form.add(new JLabel("Register Number:"));
        form.add(regNoField);
        form.add(new JLabel("Fee Amount (Rs.):"));
        form.add(amountField);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 6));
        buttons.add(payBtn);
        buttons.add(clearBtn);
        buttons.add(receiptBtn);

        statusLabel.setBorder(BorderFactory.createEmptyBorder(4, 12, 8, 12));
        add(form, BorderLayout.NORTH);
        add(buttons, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);

        payBtn.addActionListener(e -> handlePay());
        clearBtn.addActionListener(e -> handleClear());
        receiptBtn.addActionListener(e -> handleReceipt());

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                dispose();
            }
        });
        pack();
        setLocationRelativeTo(null);
    }

    private void handlePay() {
        String regNo = regNoField.getText();
        String amtText = amountField.getText();
        double amount;
        try {
            amount = feeService.validateAndParse(regNo, amtText);
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Invalid Input", JOptionPane.WARNING_MESSAGE);
            return;
        }

        payBtn.setEnabled(false);
        statusLabel.setText("Processing payment with bank...");

        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                return feeService.processPayment(regNo, amount);
            }

            @Override
            protected void done() {
                payBtn.setEnabled(true);
                try {
                    lastReceipt = get();
                    statusLabel.setText("Payment successful for " + regNo.trim());
                    JOptionPane.showMessageDialog(FeePaymentApp.this, lastReceipt,
                            "Payment Confirmed", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    statusLabel.setText("Payment failed.");
                    JOptionPane.showMessageDialog(FeePaymentApp.this,
                            "Error: " + ex.getCause().getMessage(),
                            "Payment Failed", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void handleClear() {
        regNoField.setText("");
        amountField.setText("");
        statusLabel.setText("Form cleared.");
    }

    private void handleReceipt() {
        if (lastReceipt == null) {
            JOptionPane.showMessageDialog(this, "No completed payment in this session.",
                    "Receipt", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, lastReceipt, "Last Fee Receipt", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new FeePaymentApp().setVisible(true));
    }
}