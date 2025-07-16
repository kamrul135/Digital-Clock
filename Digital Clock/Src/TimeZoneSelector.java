import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.border.*;
import java.time.ZoneId;

/**
 * Dialog for selecting time zones to add to the world clock display.
 */
public class TimeZoneSelector extends JDialog {
    private JTextField searchField;
    private JList<TimeZoneItem> resultsList;
    private DefaultListModel<TimeZoneItem> resultsModel;
    private JCheckBox[] commonLocations;
    private JButton addButton;
    private JButton cancelButton;
    private boolean selectionMade = false;
    
    // Common cities with their time zones
    private static final Map<String, String> COMMON_CITIES = new LinkedHashMap<>();
    static {
        COMMON_CITIES.put("London", "Europe/London");
        COMMON_CITIES.put("New York", "America/New_York");
        COMMON_CITIES.put("Tokyo", "Asia/Tokyo");
        COMMON_CITIES.put("Sydney", "Australia/Sydney");
        COMMON_CITIES.put("Paris", "Europe/Paris");
        COMMON_CITIES.put("Dubai", "Asia/Dubai");
        COMMON_CITIES.put("Los Angeles", "America/Los_Angeles");
        COMMON_CITIES.put("Singapore", "Asia/Singapore");
    }
    
    private List<TimeZoneItem> selectedTimeZones = new ArrayList<>();
    
    public TimeZoneSelector(JFrame parent) {
        super(parent, "Add Another Place", true);
        initComponents();
        setupLayout();
        setupListeners();
        
        setSize(400, 500);
        setLocationRelativeTo(parent);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }
    
    private void initComponents() {
        // Search field
        searchField = new JTextField(20);
        searchField.setFont(new Font("Arial", Font.PLAIN, 14));
        searchField.setBorder(BorderFactory.createCompoundBorder(
            searchField.getBorder(),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        
        // Results list
        resultsModel = new DefaultListModel<>();
        resultsList = new JList<>(resultsModel);
        resultsList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        resultsList.setCellRenderer(new TimeZoneRenderer());
        resultsList.setBackground(Color.BLACK);
        resultsList.setForeground(Color.WHITE);
        resultsList.setFont(new Font("Arial", Font.PLAIN, 14));
        
        // Common locations checkboxes
        JPanel commonPanel = new JPanel(new GridLayout(0, 1, 0, 5));
        commonPanel.setBackground(Color.BLACK);
        commonLocations = new JCheckBox[COMMON_CITIES.size()];
        
        int i = 0;
        for (Map.Entry<String, String> entry : COMMON_CITIES.entrySet()) {
            commonLocations[i] = new JCheckBox(entry.getKey());
            commonLocations[i].setForeground(Color.WHITE);
            commonLocations[i].setBackground(Color.BLACK);
            commonLocations[i].setFont(new Font("Arial", Font.PLAIN, 14));
            commonLocations[i].setFocusPainted(false);
            commonPanel.add(commonLocations[i]);
            i++;
        }
        
        // Action buttons
        addButton = new JButton("Add");
        addButton.setFont(new Font("Arial", Font.BOLD, 14));
        addButton.setBackground(new Color(0, 150, 136));
        addButton.setForeground(Color.WHITE);
        addButton.setFocusPainted(false);
        
        cancelButton = new JButton("Cancel");
        cancelButton.setFont(new Font("Arial", Font.PLAIN, 14));
        cancelButton.setBackground(Color.DARK_GRAY);
        cancelButton.setForeground(Color.WHITE);
        cancelButton.setFocusPainted(false);
    }
    
    private void setupLayout() {
        setLayout(new BorderLayout(10, 10));
        
        // Set up search panel
        JPanel searchPanel = new JPanel(new BorderLayout(5, 5));
        searchPanel.setBackground(Color.BLACK);
        searchPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));
        
        JLabel searchLabel = new JLabel("Search for a city or time zone:");
        searchLabel.setForeground(Color.CYAN);
        searchLabel.setFont(new Font("Arial", Font.BOLD, 14));
        
        searchPanel.add(searchLabel, BorderLayout.NORTH);
        searchPanel.add(searchField, BorderLayout.CENTER);
        
        // Set up results panel
        JScrollPane resultsScrollPane = new JScrollPane(resultsList);
        resultsScrollPane.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
        resultsScrollPane.getViewport().setBackground(Color.BLACK);
        
        JPanel resultsPanel = new JPanel(new BorderLayout(5, 5));
        resultsPanel.setBackground(Color.BLACK);
        resultsPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        
        JLabel resultsLabel = new JLabel("Search Results:");
        resultsLabel.setForeground(Color.CYAN);
        resultsLabel.setFont(new Font("Arial", Font.BOLD, 14));
        
        resultsPanel.add(resultsLabel, BorderLayout.NORTH);
        resultsPanel.add(resultsScrollPane, BorderLayout.CENTER);
        
        // Set up common locations panel
        JPanel commonLocationsPanel = new JPanel(new BorderLayout(5, 5));
        commonLocationsPanel.setBackground(Color.BLACK);
        commonLocationsPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        
        JLabel commonLabel = new JLabel("Common Locations:");
        commonLabel.setForeground(Color.CYAN);
        commonLabel.setFont(new Font("Arial", Font.BOLD, 14));
        
        JScrollPane commonScrollPane = new JScrollPane();
        commonScrollPane.setViewportView(commonLocations[0].getParent());
        commonScrollPane.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
        commonScrollPane.getViewport().setBackground(Color.BLACK);
        
        commonLocationsPanel.add(commonLabel, BorderLayout.NORTH);
        commonLocationsPanel.add(commonScrollPane, BorderLayout.CENTER);
        
        // Set up button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        buttonPanel.setBackground(Color.BLACK);
        buttonPanel.add(cancelButton);
        buttonPanel.add(addButton);
        
        // Add components to main panel
        JPanel centerPanel = new JPanel(new GridLayout(2, 1, 10, 10));
        centerPanel.setBackground(Color.BLACK);
        centerPanel.add(resultsPanel);
        centerPanel.add(commonLocationsPanel);
        
        add(searchPanel, BorderLayout.NORTH);
        add(centerPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
        
        // Set the global background
        getContentPane().setBackground(Color.BLACK);
    }
    
    private void setupListeners() {
        // Search field listener
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                performSearch();
            }
            
            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                performSearch();
            }
            
            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                performSearch();
            }
        });
        
        // Add button listener
        addButton.addActionListener(e -> {
            collectSelectedTimeZones();
            selectionMade = true;
            dispose();
        });
        
        // Cancel button listener
        cancelButton.addActionListener(e -> dispose());
    }
    
    private void performSearch() {
        String query = searchField.getText().toLowerCase();
        resultsModel.clear();
        
        if (query.length() < 2) return;
        
        // Search through available time zones
        ZoneId.getAvailableZoneIds().stream()
            .filter(zoneId -> zoneId.toLowerCase().contains(query) || 
                    zoneId.replace('_', ' ').toLowerCase().contains(query))
            .map(zoneId -> new TimeZoneItem(formatZoneId(zoneId), zoneId))
            .sorted()
            .limit(20)  // Limit to 20 results
            .forEach(resultsModel::addElement);
    }
    
    private String formatZoneId(String zoneId) {
        // Format the zone ID to be more user-friendly
        return zoneId.replace("_", " ")
                     .replace("/", " - ");
    }
    
    private void collectSelectedTimeZones() {
        selectedTimeZones.clear();
        
        // Add selected items from the list
        for (TimeZoneItem item : resultsList.getSelectedValuesList()) {
            selectedTimeZones.add(item);
        }
        
        // Add checked common locations
        int i = 0;
        for (Map.Entry<String, String> entry : COMMON_CITIES.entrySet()) {
            if (commonLocations[i].isSelected()) {
                selectedTimeZones.add(new TimeZoneItem(entry.getKey(), entry.getValue()));
            }
            i++;
        }
    }
    
    public List<TimeZoneItem> getSelectedTimeZones() {
        return selectedTimeZones;
    }
    
    public boolean isSelectionMade() {
        return selectionMade;
    }
    
    // TimeZoneItem class to store name and zone ID
    public static class TimeZoneItem implements Comparable<TimeZoneItem> {
        private String name;
        private String zoneId;
        
        public TimeZoneItem(String name, String zoneId) {
            this.name = name;
            this.zoneId = zoneId;
        }
        
        public String getName() {
            return name;
        }
        
        public String getZoneId() {
            return zoneId;
        }
        
        @Override
        public String toString() {
            return name;
        }
        
        @Override
        public int compareTo(TimeZoneItem other) {
            return name.compareTo(other.name);
        }
    }
    
    // Custom renderer for time zone items
    private class TimeZoneRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, 
                int index, boolean isSelected, boolean cellHasFocus) {
            
            Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            
            if (value instanceof TimeZoneItem) {
                setText(((TimeZoneItem) value).getName());
                
                if (isSelected) {
                    setBackground(new Color(0, 150, 136));
                    setForeground(Color.WHITE);
                } else {
                    setBackground(Color.BLACK);
                    setForeground(Color.WHITE);
                }
            }
            
            setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
            return c;
        }
    }
}
