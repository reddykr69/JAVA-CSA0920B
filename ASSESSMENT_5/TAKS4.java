import java.io.*;
import java.nio.file.*;
import java.sql.*;
import java.util.*;

public class CampusMigrationTool {
    private static final String URL = "jdbc:mysql://localhost:3306/transport_db?rewriteBatchedStatements=true";
    private static final String USER = "root";
    private static final String PWD = "password";

    // B4.2: Inspect Database Metadata
    static void inspectDatabase(Connection con) throws SQLException {
        DatabaseMetaData meta = con.getMetaData();
        System.out.println("=== Database Metadata ===");
        System.out.println("Product : " + meta.getDatabaseProductName() + " " + meta.getDatabaseProductVersion());
        System.out.println("Driver  : " + meta.getDriverName() + " " + meta.getDriverVersion());
        System.out.print("Tables  : ");
        try (ResultSet rs = meta.getTables(con.getCatalog(), null, "%", new String[]{"TABLE"})) {
            List<String> tables = new ArrayList<>();
            while (rs.next()) tables.add(rs.getString("TABLE_NAME"));
            System.out.println(tables);
        }
    }

    // B4.1: Migrate CSV to MySQL in Batches + Transaction, Reporting Invalid Rows
    static void migrateCsvToDb(Connection con, Path csvFile) throws Exception {
        String sql = "INSERT INTO bus_pass(pass_id, reg_no, name, route_no, amount) VALUES (?, ?, ?, ?, ?)";
        List<String> errorReport = new ArrayList<>();
        int migrated = 0, lineNo = 0;

        boolean oldAuto = con.getAutoCommit();
        con.setAutoCommit(false);

        try (BufferedReader br = Files.newBufferedReader(csvFile);
             PreparedStatement ps = con.prepareStatement(sql)) {
            String line;
            while ((line = br.readLine()) != null) {
                lineNo++;
                String[] f = line.split(",", -1);
                if (f.length != 5) {
                    errorReport.add("Line " + lineNo + " -> Wrong field count: " + line);
                    continue;
                }
                try {
                    int route = Integer.parseInt(f[3].trim());
                    double amt = Double.parseDouble(f[4].trim());
                    if (amt <= 0) throw new IllegalArgumentException("Non-positive amount");

                    ps.setString(1, f[0].trim());
                    ps.setString(2, f[1].trim());
                    ps.setString(3, f[2].trim());
                    ps.setInt(4, route);
                    ps.setDouble(5, amt);
                    ps.addBatch();
                    if (++migrated % 1000 == 0) ps.executeBatch();
                } catch (RuntimeException ex) {
                    errorReport.add("Line " + lineNo + " -> Validation failed (" + ex.getMessage() + "): " + line);
                }
            }
            ps.executeBatch();
            con.commit();
        } catch (SQLException e) {
            con.rollback();
            throw e;
        } finally {
            con.setAutoCommit(oldAuto);
        }

        System.out.println("\n=== Migration Summary ===");
        System.out.println("Successfully Migrated : " + migrated);
        System.out.println("Failed Validation     : " + errorReport.size());
        errorReport.forEach(System.out::println);
    }

    public static void main(String[] args) throws Exception {
        Path sampleCsv = Files.createTempFile("legacy_passes_", ".csv");
        Files.writeString(sampleCsv,
                "MP101,192324001,Kiran,12,4500.00\n" +
                "MP102,BAD_ROW\n" +
                "MP103,192324003,Sneha,8,-500.00\n" +
                "MP104,192324004,Arjun,4,3800.00\n");

        try (Connection con = DriverManager.getConnection(URL, USER, PWD)) {
            inspectDatabase(con);
            try (Statement st = con.createStatement()) { st.executeUpdate("TRUNCATE TABLE bus_pass"); }
            migrateCsvToDb(con, sampleCsv);
        } finally {
            Files.deleteIfExists(sampleCsv);
        }
    }
}