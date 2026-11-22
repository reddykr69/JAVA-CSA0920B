import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.event.*;

public class HospitalAppointmentDesk extends JFrame {
    private final JTextField patientIdField = new JTextField(15);
    private final JComboBox<String> deptBox = new JComboBox<>(
            new String[]{"Cardiology", "Orthopedics", "Pediatrics", "Dermatology"});
    private final JComboBox<String> doctorBox = new JComboBox<>();
    private final JComboBox<String> slotBox = new JComboBox<>(
            new String[]{"09:00 AM", "10:00 AM", "11:00 AM", "02:00 PM", "03:30 PM"});
    private final JButton bookBtn = new JButton("Book Appointment");
    private final JButton clearBtn = new JButton("Clear");
    private final JLabel statusLabel = new JLabel("Enter Patient ID (e.g., P1001) to begin.");

    private final Map<String, List<String>> deptDoctors = new HashMap<>();
    private final Set<String> bookedSlots = new HashSet<>();
    private boolean bookingInProgress = false;

    public HospitalAppointmentDesk() {
        super("Hospital Outpatient Appointment Desk");
        deptDoctors.put("Cardiology", List.of("Dr. A. Menon", "Dr. R. Iyer"));
        deptDoctors.put("Orthopedics", List.of("Dr. S. Kulkarni", "Dr. V. Nair"));
        deptDoctors.put("Pediatrics", List.of("Dr. P. Sharma", "Dr. M. Das"));
        deptDoctors.put("Dermatology", List.of("Dr. K. Reddy"));

        setLayout(new BorderLayout(10, 10));
        JLabel title = new JLabel("OUTPATIENT APPOINTMENT DESK", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 15));
        title.setBorder(BorderFactory.createEmptyBorder(10, 10, 4, 10));

        JPanel form = new JPanel(new GridLayout(4, 2, 8, 8));
        form.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));
        form.add(new JLabel("Patient ID:"));  form.add(patientIdField);
        form.add(new JLabel("Department:"));  form.add(deptBox);
        form.add(new JLabel("Doctor:"));      form.add(doctorBox);
        form.add(new JLabel("Time Slot:"));   form.add(slotBox);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 4));
        btnPanel.add(bookBtn);
        btnPanel.add(clearBtn);

        JPanel bottom = new JPanel(new BorderLayout());
        statusLabel.setBorder(BorderFactory.createEmptyBorder(4, 12, 8, 12));
        bottom.add(btnPanel, BorderLayout.NORTH);
        bottom.add(statusLabel, BorderLayout.SOUTH);

        add(title, BorderLayout.NORTH);
        add(form, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);

        refreshDoctors();
        deptBox.addActionListener(e -> refreshDoctors());
        bookBtn.addActionListener(e -> bookAppointment());
        clearBtn.addActionListener(e -> clearForm());

        patientIdField.getDocument().addDocumentListener(new DocumentListener() {
            private void updateFlag() { bookingInProgress = !patientIdField.getText().trim().isEmpty(); }
            public void insertUpdate(DocumentEvent e) { updateFlag(); }
            public void removeUpdate(DocumentEvent e) { updateFlag(); }
            public void changedUpdate(DocumentEvent e) { updateFlag(); }
        });

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (bookingInProgress) {
                    int choice = JOptionPane.showConfirmDialog(
                            HospitalAppointmentDesk.this,
                            "A booking is currently in progress. Discard and close?",
                            "Confirm Exit", JOptionPane.YES_NO_OPTION);
                    if (choice != JOptionPane.YES_OPTION) return;
                }
                dispose();
            }
        });

        pack();
        setLocationRelativeTo(null);
    }

    private void refreshDoctors() {
        String dept = (String) deptBox.getSelectedItem();
        doctorBox.removeAllItems();
        for (String doc : deptDoctors.getOrDefault(dept, List.of())) {
            doctorBox.addItem(doc);
        }
    }

    private void bookAppointment() {
        String pid = patientIdField.getText().trim();
        if (!pid.matches("P[0-9]{4}")) {
            JOptionPane.showMessageDialog(this, "Invalid Patient ID. Expected format: P1001",
                    "Validation Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String doctor = (String) doctorBox.getSelectedItem();
        String slot = (String) slotBox.getSelectedItem();
        String key = doctor + "|" + slot;
        if (!bookedSlots.add(key)) {
            JOptionPane.showMessageDialog(this, slot + " is already booked for " + doctor + ".",
                    "Slot Unavailable", JOptionPane.WARNING_MESSAGE);
            return;
        }
        bookingInProgress = false;
        String msg = "Confirmed: " + pid + " with " + doctor + " at " + slot;
        statusLabel.setText(msg);
        JOptionPane.showMessageDialog(this, msg, "Appointment Confirmed", JOptionPane.INFORMATION_MESSAGE);
        patientIdField.setText("");
    }

    private void clearForm() {
        patientIdField.setText("");
        deptBox.setSelectedIndex(0);
        slotBox.setSelectedIndex(0);
        bookingInProgress = false;
        statusLabel.setText("Form cleared.");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new HospitalAppointmentDesk().setVisible(true));
    }
}