import java.awt.*;
import java.util.*;
import java.util.List;
import javax.swing.*;

public class LibrarySearchScreen extends JFrame {
    private final JTextField searchField = new JTextField(22);
    private final JComboBox<String> categoryBox = new JComboBox<>(
            new String[]{"All", "Computer Science", "Mathematics", "Electronics", "Literature"});
    private final JTextArea resultsArea = new JTextArea(12, 50);
    private final JButton prevBtn = new JButton("Prev");
    private final JButton nextBtn = new JButton("Next");
    private final JButton reserveBtn = new JButton("Reserve");
    private final JLabel statusLabel = new JLabel("Ready. Enter a keyword and click Search.");

    private final List<String> catalog = List.of(
        "CS101 | Effective Java | Computer Science | Available",
        "CS102 | Java Concurrency in Practice | Computer Science | Available",
        "CS103 | Clean Code | Computer Science | Checked Out",
        "MA201 | Discrete Mathematics | Mathematics | Available",
        "MA202 | Linear Algebra Done Right | Mathematics | Available",
        "EC301 | Digital Logic Design | Electronics | Available",
        "LI401 | Technical Communication | Literature | Available"
    );
    private List<String> filtered = new ArrayList<>(catalog);
    private int page = 0;
    private static final int PAGE_SIZE = 3;

    public LibrarySearchScreen() {
        super("Campus Library Search");
        setLayout(new BorderLayout(8, 8));

        JPanel topPanel = new JPanel(new BorderLayout(4, 4));
        JLabel title = new JLabel("CAMPUS LIBRARY CATALOGUE SEARCH", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 16));
        title.setBorder(BorderFactory.createEmptyBorder(8, 8, 4, 8));

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        JButton searchBtn = new JButton("Search");
        searchPanel.add(new JLabel("Keyword:"));
        searchPanel.add(searchField);
        searchPanel.add(new JLabel("Category:"));
        searchPanel.add(categoryBox);
        searchPanel.add(searchBtn);

        topPanel.add(title, BorderLayout.NORTH);
        topPanel.add(searchPanel, BorderLayout.SOUTH);

        resultsArea.setEditable(false);
        resultsArea.setFont(new Font("Monospaced", Font.PLAIN, 13));
        JScrollPane scrollPane = new JScrollPane(resultsArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Search Results"));

        JPanel bottomPanel = new JPanel(new BorderLayout(4, 4));
        JPanel navPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 4));
        navPanel.add(prevBtn);
        navPanel.add(nextBtn);
        navPanel.add(reserveBtn);

        statusLabel.setBorder(BorderFactory.createEmptyBorder(4, 10, 6, 10));
        bottomPanel.add(navPanel, BorderLayout.CENTER);
        bottomPanel.add(statusLabel, BorderLayout.SOUTH);

        add(topPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        searchBtn.addActionListener(e -> performSearch());
        searchField.addActionListener(e -> performSearch());
        prevBtn.addActionListener(e -> { if (page > 0) { page--; renderPage(); } });
        nextBtn.addActionListener(e -> { if ((page + 1) * PAGE_SIZE < filtered.size()) { page++; renderPage(); } });
        reserveBtn.addActionListener(e -> statusLabel.setText("Reservation request submitted for current page."));

        renderPage();
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(760, 460);
        setLocationRelativeTo(null);
    }

    private void performSearch() {
        String kw = searchField.getText().trim().toLowerCase();
        String cat = (String) categoryBox.getSelectedItem();
        filtered = new ArrayList<>();
        for (String item : catalog) {
            boolean matchesKw = kw.isEmpty() || item.toLowerCase().contains(kw);
            boolean matchesCat = "All".equals(cat) || item.contains("| " + cat + " |");
            if (matchesKw && matchesCat) filtered.add(item);
        }
        page = 0;
        renderPage();
    }

    private void renderPage() {
        StringBuilder sb = new StringBuilder();
        int start = page * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, filtered.size());
        for (int i = start; i < end; i++) {
            sb.append(i + 1).append(". ").append(filtered.get(i)).append("\n");
        }
        resultsArea.setText(filtered.isEmpty() ? "No matching books found." : sb.toString());
        statusLabel.setText(String.format("Showing %d-%d of %d books (Page %d)",
                filtered.isEmpty() ? 0 : start + 1, end, filtered.size(), page + 1));
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LibrarySearchScreen().setVisible(true));
    }
}