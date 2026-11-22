import java.awt.*;
import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class CampusEventApp extends JFrame {

    // ===== MODEL =====
    static class Registration {
        final int id;
        final String studentId, studentName, email, department, eventName;

        Registration(int id, String studentId, String studentName,
                     String email, String department, String eventName) {
            this.id = id;
            this.studentId = studentId;
            this.studentName = studentName;
            this.email = email;
            this.department = department;
            this.eventName = eventName;
        }
    }

    // ===== DATA ACCESS LAYER (JDBC + Safe Fallback) =====
    static class RegistrationDAO {
        private static final String URL = "jdbc:mysql://localhost:3306/campus_events";
        private static final String USER = "root";
        private static final String PWD = "password";

        private final List<Registration> fallbackStore = new ArrayList<>();
        private int nextId = 1;
        private boolean useFallback = false;

        private Connection getConnection() throws SQLException {
            return DriverManager.getConnection(URL, USER, PWD);
        }

        public boolean exists(String studentId, String eventName) throws SQLException {
            if (!useFallback) {
                String sql = "SELECT 1 FROM registrations WHERE student_id = ? AND event_name = ?";
                try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
                    ps.setString(1, studentId);
                    ps.setString(2, eventName);
                    try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
                } catch (SQLException e) {
                    useFallback = true;
                }
            }
            return fallbackStore.stream()
                    .anyMatch(r -> r.studentId.equalsIgnoreCase(studentId)
                                && r.eventName.equalsIgnoreCase(eventName));
        }

        public void insert(Registration r) throws SQLException {
            if (!useFallback) {
                String sql = "INSERT INTO registrations (student_id, student_name, email, department, event_name) "
                           + "VALUES (?, ?, ?, ?, ?)";
                try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
                    ps.setString(1, r.studentId);
                    ps.setString(2, r.studentName);
                    ps.setString(3, r.email);
                    ps.setString(4, r.department);
                    ps.setString(5, r.eventName);
                    ps.executeUpdate();
                    return;
                } catch (SQLException e) {
                    if ("23000".equals(e.getSQLState())) throw e; // Real duplicate key error
                    useFallback = true;
                }
            }
            fallbackStore.add(new Registration(nextId++, r.studentId, r.studentName,
                    r.email, r.department, r.eventName));
        }

        public List<Registration> findByEvent(String eventFilter) throws SQLException {
            if (!useFallback) {
                boolean all = "All Events".equals(eventFilter);
                String sql = all
                        ? "SELECT id, student_id, student_name, email, department, event_name FROM registrations ORDER BY id DESC"
                        : "SELECT id, student_id, student_name, email, department, event_name FROM registrations WHERE event_name = ? ORDER BY id DESC";
                try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
                    if (!all) ps.setString(1, eventFilter);
                    List<Registration> list = new ArrayList<>();
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            list.add(new Registration(
                                rs.getInt("id"), rs.getString("student_id"),
                                rs.getString("student_name"), rs.getString("email"),
                                rs.getString("department"), rs.getString("event_name")));
                        }
                    }
                    return list;
                } catch (SQLException e) {
                    useFallback = true;
                }
            }
            List<Registration> out = new ArrayList<>();
            for (Registration r : fallbackStore) {
                if ("All Events".equals(eventFilter) || r.eventName.equals(eventFilter)) out.add(r);
            }
            return out;
        }
    }

    // ===== SERVICE LAYER =====
    static class RegistrationService {
        private final RegistrationDAO dao = new RegistrationDAO();

        public void validate(Registration r) {
            if (r.studentId.isEmpty() || !r.studentId.matches("[A-Za-z0-9]{4,15}")) {
                throw new IllegalArgumentException("Student ID must be 4-15 alphanumeric characters.");
            }
            if (r.studentName.isEmpty()) {
                throw new IllegalArgumentException("Student Name cannot be empty.");
            }
            if (!r.email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
                throw new IllegalArgumentException("Please enter a valid email address.");
            }
        }

        public void register(Registration r) throws Exception {
            validate(r);
            if (dao.exists(r.studentId, r.eventName)) {
                throw new IllegalStateException("Duplicate Registration: " + r.studentId
                        + " is already registered for " + r.eventName + ".");
            }
            dao.insert(r);
        }

        public List<Registration> getByEvent(String eventFilter) throws Exception {
            return dao.findByEvent(eventFilter);
        }

        public void exportCsv(List<Registration> rows, File file) throws IOException {
            try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
                pw.println("ID,StudentID,Name,Email,Department,Event");
                for (Registration r : rows) {
                    pw.printf("%d,\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"%n",
                            r.id, r.studentId, r.studentName, r.email, r.department, r.eventName);
                }
            }
        }
    }

    // ===== UI LAYER (SWING) =====
    private final RegistrationService service = new RegistrationService();
    private final JTextField regNoField = new JTextField(10);
    private final JTextField nameField = new JTextField(14);
    private final JTextField emailField = new JTextField(16);
    private final JComboBox<String> deptBox = new JComboBox<>(new String[]{"CSE", "ECE", "IT", "MECH", "CIVIL"});
    private final JComboBox<String> eventBox = new JComboBox<>(
            new String[]{"Hackathon", "Classical Dance", "Debate", "Short Film", "Music Band"});
    private final JComboBox<String> filterBox = new JComboBox<>(
            new String[]{"All Events", "Hackathon", "Classical Dance", "Debate", "Short Film", "Music Band"});
    private final JButton registerBtn = new JButton("Register Student");
    private final DefaultTableModel tableModel = new DefaultTableModel(
            new String[]{"ID", "Reg No", "Student Name", "Email", "Department", "Event"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JLabel statusLabel = new JLabel("Ready");
    private List<Registration> currentRows = new ArrayList<>();

    public CampusEventApp() {
        super("Campus Cultural Event Registration");
        setLayout(new BorderLayout(8, 8));
        buildMenuBar();

        // Top: Registration Form + Filter Row
        JPanel formPanel = new JPanel(new GridLayout(2, 6, 6, 6));
        formPanel.setBorder(BorderFactory.createTitledBorder("Student Event Registration"));
        formPanel.add(new JLabel("Reg No:")); formPanel.add(regNoField);
        formPanel.add(new JLabel("Name:"));   formPanel.add(nameField);
        formPanel.add(new JLabel("Email:"));  formPanel.add(emailField);
        formPanel.add(new JLabel("Dept:"));   formPanel.add(deptBox);
        formPanel.add(new JLabel("Event:"));  formPanel.add(eventBox);
        formPanel.add(new JLabel(""));        formPanel.add(registerBtn);

        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        JButton filterBtn = new JButton("Apply Filter");
        JButton exportBtn = new JButton("Export CSV");
        filterPanel.add(new JLabel("Filter by Event:"));
        filterPanel.add(filterBox);
        filterPanel.add(filterBtn);
        filterPanel.add(exportBtn);

        JPanel top = new JPanel(new BorderLayout());
        top.add(formPanel, BorderLayout.NORTH);
        top.add(filterPanel, BorderLayout.SOUTH);

        JTable table = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(table);

        statusLabel.setBorder(BorderFactory.createEmptyBorder(4, 10, 6, 10));
        add(top, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);

        registerBtn.addActionListener(e -> handleRegister());
        filterBtn.addActionListener(e -> handleFilter());
        exportBtn.addActionListener(e -> handleExport());

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(900, 520);
        setLocationRelativeTo(null);
        handleFilter();
    }

    private void buildMenuBar() {
        JMenuBar bar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        JMenuItem exportItem = new JMenuItem("Export CSV...");
        JMenuItem exitItem = new JMenuItem("Exit");
        exportItem.addActionListener(e -> handleExport());
        exitItem.addActionListener(e -> dispose());
        fileMenu.add(exportItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);

        JMenu eventsMenu = new JMenu("Events");
        JMenuItem refreshItem = new JMenuItem("Refresh Table");
        JMenuItem clearItem = new JMenuItem("Clear Form");
        refreshItem.addActionListener(e -> handleFilter());
        clearItem.addActionListener(e -> { regNoField.setText(""); nameField.setText(""); emailField.setText(""); });
        eventsMenu.add(refreshItem);
        eventsMenu.add(clearItem);

        JMenu helpMenu = new JMenu("Help");
        JMenuItem aboutItem = new JMenuItem("About");
        aboutItem.addActionListener(e -> JOptionPane.showMessageDialog(this,
                "Campus Event Registration v1.0\nLayered Swing + JDBC Application",
                "About", JOptionPane.INFORMATION_MESSAGE));
        helpMenu.add(aboutItem);

        bar.add(fileMenu); bar.add(eventsMenu); bar.add(helpMenu);
        setJMenuBar(bar);
    }

    private void handleRegister() {
        Registration reg = new Registration(0,
                regNoField.getText().trim(), nameField.getText().trim(),
                emailField.getText().trim(), (String) deptBox.getSelectedItem(),
                (String) eventBox.getSelectedItem());
        try {
            service.validate(reg);
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Invalid Input", JOptionPane.WARNING_MESSAGE);
            return;
        }

        registerBtn.setEnabled(false);
        statusLabel.setText("Saving registration...");
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception {
                service.register(reg);
                return null;
            }
            @Override protected void done() {
                registerBtn.setEnabled(true);
                try {
                    get();
                    regNoField.setText(""); nameField.setText(""); emailField.setText("");
                    handleFilter();
                    JOptionPane.showMessageDialog(CampusEventApp.this, "Registration successful!",
                            "Confirmed", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    statusLabel.setText("Registration rejected.");
                    JOptionPane.showMessageDialog(CampusEventApp.this, cause.getMessage(),
                            "Registration Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void handleFilter() {
        String selected = (String) filterBox.getSelectedItem();
        statusLabel.setText("Loading registrations...");
        new SwingWorker<List<Registration>, Void>() {
            @Override protected List<Registration> doInBackground() throws Exception {
                return service.getByEvent(selected);
            }
            @Override protected void done() {
                try {
                    currentRows = get();
                    tableModel.setRowCount(0);
                    for (Registration r : currentRows) {
                        tableModel.addRow(new Object[]{r.id, r.studentId, r.studentName,
                                r.email, r.department, r.eventName});
                    }
                    statusLabel.setText("Loaded " + currentRows.size() + " registrations (" + selected + ").");
                } catch (Exception ex) {
                    statusLabel.setText("Error loading registrations.");
                }
            }
        }.execute();
    }

    private void handleExport() {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("registrations.csv"));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File target = chooser.getSelectedFile();
            try {
                service.exportCsv(currentRows, target);
                statusLabel.setText("Exported " + currentRows.size() + " rows to " + target.getName());
                JOptionPane.showMessageDialog(this, "Exported to " + target.getAbsolutePath(),
                        "Export Complete", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Export failed: " + ex.getMessage(),
                        "I/O Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new CampusEventApp().setVisible(true));
    }
}