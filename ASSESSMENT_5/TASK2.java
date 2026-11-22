import java.io.IOException;
import java.nio.file.*;
import java.sql.*;

public class AdversarialDemo {
    private static final String URL = "jdbc:mysql://localhost:3306/transport_db";
    private static final String USER = "root";
    private static final String PWD = "password";

    static boolean unsafeLogin(Connection con, String id, String pwd) throws SQLException {
        String sql = "SELECT * FROM users WHERE id='" + id + "' AND pwd='" + pwd + "'";
        System.out.println("[Unsafe SQL] " + sql);
        try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            return rs.next();
        }
    }

    static boolean safeLogin(Connection con, String id, String pwd) throws SQLException {
        String sql = "SELECT * FROM users WHERE id = ? AND pwd = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.setString(2, pwd);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public static void main(String[] args) throws Exception {
        String craftedId = "admin' -- ";
        String craftedPwd = "anything";

        System.out.println("=== Part 1: SQL Injection Adversarial Test ===");
        try (Connection con = DriverManager.getConnection(URL, USER, PWD)) {
            System.out.println("Unsafe Login Result (Injected) : " + unsafeLogin(con, craftedId, craftedPwd));
            System.out.println("Safe Login Result   (Injected) : " + safeLogin(con, craftedId, craftedPwd));
            System.out.println("Safe Login Result   (Valid)    : " + safeLogin(con, "admin", "StrongPass#2026"));
        }

        System.out.println("\n=== Part 2: Corrupt Bus-Pass File Test ===");
        Path corruptFile = Files.createTempFile("corrupt_passes_", ".csv");
        Files.writeString(corruptFile,
                "\n" +                                      // Empty line
                "P2001,ONLY_TWO_FIELDS\n" +                 // Wrong number of fields
                "P2002,192324002,Ravi,10,NOT_A_NUMBER\n" +  // Non-numeric amount
                "P2003,192324003,Meena,5,4200.0\n");        // Valid record

        int lineNo = 0, valid = 0, skipped = 0;
        for (String line : Files.readAllLines(corruptFile)) {
            lineNo++;
            String[] f = line.split(",", -1);
            if (f.length != 5) {
                System.out.println("Line " + lineNo + " SKIPPED (wrong field count): " + line);
                skipped++;
                continue;
            }
            try {
                Integer.parseInt(f[3].trim());
                Double.parseDouble(f[4].trim());
                System.out.println("Line " + lineNo + " VALID: " + f[0] + " (" + f[2] + ")");
                valid++;
            } catch (NumberFormatException e) {
                System.out.println("Line " + lineNo + " SKIPPED (non-numeric route/amount): " + line);
                skipped++;
            }
        }
        System.out.printf("Summary -> Valid: %d, Skipped/Damaged: %d%n", valid, skipped);
        Files.deleteIfExists(corruptFile);
    }
}