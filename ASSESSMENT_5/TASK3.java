import java.sql.*;
import java.util.*;

public class PassLoader {

    /** Loads all passes in one transaction, in batches of 1,000 rows.
     *  Returns the number of rows loaded. */
    public static int loadAll(Connection con, List<String[]> passes) throws SQLException {
        String sql = "INSERT INTO bus_pass(pass_id, reg_no, name, route_no, amount) "
                   + "VALUES (?, ?, ?, ?, ?)";
        boolean oldAutoCommit = con.getAutoCommit();
        con.setAutoCommit(false);
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            int n = 0;
            for (String[] p : passes) {
                // TODO 1 COMPLETED: Bind 5 parameters, addBatch, execute every 1,000 rows
                ps.setString(1, p[0]);
                ps.setString(2, p[1]);
                ps.setString(3, p[2]);
                ps.setInt(4, Integer.parseInt(p[3]));
                ps.setDouble(5, Double.parseDouble(p[4]));
                ps.addBatch();
                if (++n % 1000 == 0) {
                    ps.executeBatch();
                }
            }
            ps.executeBatch();
            // TODO 2 COMPLETED: Commit transaction and return count
            con.commit();
            return n;
        } catch (SQLException | RuntimeException e) {
            // TODO 3 COMPLETED: Rollback every insert in this transaction, then re-throw
            con.rollback();
            throw e;
        } finally {
            con.setAutoCommit(oldAutoCommit);
        }
    }

    private static List<String[]> generatePasses(int count) {
        List<String[]> list = new ArrayList<>(count);
        for (int i = 1; i <= count; i++) {
            list.add(new String[]{"BP" + i, "1923" + i, "Student" + i,
                    String.valueOf(i % 20 + 1), "4500.00"});
        }
        return list;
    }

    public static void main(String[] args) throws SQLException {
        String url = "jdbc:mysql://localhost:3306/transport_db?rewriteBatchedStatements=true";
        try (Connection con = DriverManager.getConnection(url, "root", "password")) {
            // Benchmark 1,000 / 10,000 / 100,000 rows
            for (int size : new int[]{1_000, 10_000, 100_000}) {
                try (Statement st = con.createStatement()) { st.executeUpdate("TRUNCATE TABLE bus_pass"); }
                List<String[]> data = generatePasses(size);
                long start = System.nanoTime();
                int loaded = loadAll(con, data);
                long ms = (System.nanoTime() - start) / 1_000_000;
                System.out.printf("Batch Loaded %7d rows in %5d ms%n", loaded, ms);
            }

            // Verify Rollback on Mid-Batch Failure
            try (Statement st = con.createStatement()) { st.executeUpdate("TRUNCATE TABLE bus_pass"); }
            List<String[]> badList = generatePasses(1_000);
            badList.set(500, new String[]{"BP501", "1923501", "BadRow", "5", "INVALID_AMOUNT"});
            try {
                loadAll(con, badList);
            } catch (RuntimeException ex) {
                System.out.println("Caught expected failure: " + ex.getMessage());
            }
            try (Statement st = con.createStatement();
                 ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM bus_pass")) {
                rs.next();
                System.out.println("Rows remaining in bus_pass after rollback: " + rs.getInt(1) + " (expected 0)");
            }
        }
    }
}