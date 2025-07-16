import javax.swing.*;
import javax.swing.Timer;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


/**
 * Advanced Digital Clock Application - Lab 2 Extended
 * 
 * This enhanced version demonstrates additional threading concepts:
 * - ExecutorService for thread pool management
 * - Future objects for thread result handling
 * - Multiple concurrent threads
 * - Thread priority management
 * - Thread interruption handling
 */
public class AdvancedDigitalClock extends JFrame {
    
    // GUI Components
    private JLabel digitalClockLabel;
    private JLabel dateLabel;
    private JLabel millisecondLabel;
    private JLabel alarmStatusLabel;
    private JButton startButton;
    private JButton stopButton;
    private JButton pauseButton;
    private JButton exitButton;
    private JButton alarmButton;
    private JButton stopwatchButton;
    private ClockPanel analogClockPanel;
    
    // World clock components
    private JPanel worldClockPanel;
    private JScrollPane worldScrollPane;
    private Map<String, CityClockDisplay> cityClocks = new LinkedHashMap<>();
    private Future<?> worldClockTaskHandle;
    
    // Alarm components
    private List<AlarmTime> alarms = new ArrayList<>();
    private volatile boolean isAlarmRinging = false;
    
    // Stopwatch components
    private JLabel stopwatchLabel;
    private JButton stopwatchStartButton;
    private JButton stopwatchStopButton;
    private JButton stopwatchResetButton;
    private JButton stopwatchLapButton;
    private JList<String> lapTimesList;
    private DefaultListModel<String> lapTimesModel;
    private volatile boolean stopwatchRunning = false;
    private long stopwatchStartTime = 0;
    private long stopwatchElapsedTime = 0;
    private List<Long> lapTimes = new ArrayList<>();
    private Future<?> stopwatchTask;
    
    // Weather components
    private JLabel weatherLocationLabel;
    private JLabel weatherTemperatureLabel;
    private JLabel weatherDescriptionLabel;
    private JLabel weatherHumidityLabel;
    private JButton weatherRefreshButton;
    private JTextField weatherCityField;
    private String currentWeatherCity = "London"; // Default city
    private Future<?> weatherTask;
    private static final String WEATHER_API_KEY = "your-api-key"; // Replace with actual API key
    
    // Thread management with ExecutorService
    private ScheduledExecutorService executorService;
    private Future<?> clockTask;
    private Future<?> millisTask;
    
    private volatile boolean isRunning = false;
    private volatile boolean isPaused = false;
    private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
    private SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, MMMM d, yyyy");
    private SimpleDateFormat millisecondFormat = new SimpleDateFormat("");
    
    public AdvancedDigitalClock() {
        setTitle("🌟 Modern Digital Clock Dashboard 🌟");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                exitApplication();
            }
        });

        initializeThreadPool();
        initializeGUI(); // This method should set up all UI components
        updateAlarmStatus(); // Update alarm status label

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }
    
    private void initializeThreadPool() {
        executorService = Executors.newScheduledThreadPool(4); // Added weather task
        System.out.println("Thread pool initialized.");
    }
    
    private void initializeGUI() {
        setTitle("🌟 Modern Digital Clock Dashboard 🌟");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(true);
        
        // Create main panel with modern gradient background
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Create beautiful gradient background from dark blue to black
                GradientPaint gradient = new GradientPaint(
                    0, 0, new Color(25, 25, 112),           // Midnight Blue
                    0, getHeight(), new Color(8, 8, 16)     // Almost Black
                );
                g2d.setPaint(gradient);
                g2d.fillRect(0, 0, getWidth(), getHeight());
                
                // Add subtle pattern overlay
                g2d.setColor(new Color(255, 255, 255, 5));
                for (int i = 0; i < getWidth(); i += 50) {
                    g2d.drawLine(i, 0, i, getHeight());
                }
                for (int i = 0; i < getHeight(); i += 50) {
                    g2d.drawLine(0, i, getWidth(), i);
                }
            }
        };
        mainPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        
        // Create top panel (kept for layout consistency)
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);
        
        // Create display panel with both digital and analog clocks
        JPanel displayPanel = createDisplayPanel();
        
        // Create control panel
        JPanel controlPanel = createControlPanel();
        
        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(displayPanel, BorderLayout.CENTER);
        mainPanel.add(controlPanel, BorderLayout.SOUTH);
        
        add(mainPanel);
        setSize(1200, 750); // Increased from 1000x600 to 1200x750 for bigger enhanced display
        setLocationRelativeTo(null);
        
        setupEventHandlers(); // ADDED CALL HERE
        updateButtonStates(true, false, false);
    }
    
    private JPanel createDisplayPanel() {
        JPanel displayPanel = new JPanel(new BorderLayout(8, 8));
        displayPanel.setOpaque(false);
        // Add border to display panel
        displayPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(100, 149, 237, 150), 2),
            "✨ Clock Display Dashboard ✨",
            javax.swing.border.TitledBorder.CENTER,
            javax.swing.border.TitledBorder.TOP,
            new Font("Segoe UI", Font.BOLD, 14),
            new Color(176, 196, 222)
        ));
        
        // Create analog clock panel with bigger size
        analogClockPanel = new ClockPanel();
        analogClockPanel.setPreferredSize(new Dimension(400, 400));
        analogClockPanel.setOpaque(false);
        
        // Create digital display panel (center top) with enhanced spacing for larger display
        JPanel digitalPanel = new JPanel(new GridLayout(5, 1, 15, 15)); // Increased spacing from 12 to 15
        digitalPanel.setOpaque(false);
        digitalPanel.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30)); // Increased padding from 25 to 30
        digitalPanel.setPreferredSize(new Dimension(520, 700)); // Increased from 450x600 to 520x700 for bigger display
        
        // Main time display with advanced pulsing glow animation
        digitalClockLabel = new JLabel("--:--:--", SwingConstants.CENTER) {
            private Timer glowTimer;
            private float glowIntensity = 0.0f;
            private boolean glowIncreasing = true;
            
            {
                // Initialize glow animation timer
                glowTimer = new Timer(50, e -> {
                    if (glowIncreasing) {
                        glowIntensity += 0.03f;
                        if (glowIntensity >= 1.0f) {
                            glowIntensity = 1.0f;
                            glowIncreasing = false;
                        }
                    } else {
                        glowIntensity -= 0.03f;
                        if (glowIntensity <= 0.3f) {
                            glowIntensity = 0.3f;
                            glowIncreasing = true;
                        }
                    }
                    repaint();
                });
                glowTimer.start();
            }
            
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                
                // Paint enhanced rounded background with animated gradient
                int arc = 20;
                float bgOpacity = 0.8f + (glowIntensity * 0.2f);
                Color bg1 = new Color(25, 25, 60, (int)(180 * bgOpacity));
                Color bg2 = new Color(15, 15, 40, (int)(220 * bgOpacity));
                GradientPaint bgGradient = new GradientPaint(0, 0, bg1, 0, getHeight(), bg2);
                g2d.setPaint(bgGradient);
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
                
                // Multiple animated glowing borders with different intensities
                for (int i = 0; i < 3; i++) {
                    int alpha = (int)(60 * glowIntensity) - (i * 15);
                    if (alpha > 0) {
                        g2d.setColor(new Color(0, 255, 127, alpha));
                        g2d.setStroke(new BasicStroke(2 + i));
                        g2d.drawRoundRect(i, i, getWidth()-(2*i)-1, getHeight()-(2*i)-1, arc-i, arc-i);
                    }
                }
                
                // Create advanced text glow effect with multiple layers
                String text = getText();
                FontMetrics fm = g2d.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(text)) / 2;
                int y = (getHeight() + fm.getAscent()) / 2;
                
                // Outer glow with animation
                int glowRadius = (int)(15 * glowIntensity);
                for (int i = glowRadius; i > 0; i--) {
                    int alpha = Math.max(0, (int)(30 * glowIntensity) - (i * 2));
                    g2d.setColor(new Color(0, 255, 127, alpha));
                    for (int dx = -1; dx <= 1; dx++) {
                        for (int dy = -1; dy <= 1; dy++) {
                            if (dx != 0 || dy != 0) {
                                g2d.drawString(text, x + (dx * i), y + (dy * i));
                            }
                        }
                    }
                }
                
                // Inner glow for depth
                for (int i = 3; i > 0; i--) {
                    int alpha = (int)(80 * glowIntensity);
                    g2d.setColor(new Color(100, 255, 200, alpha));
                    g2d.drawString(text, x + i, y);
                    g2d.drawString(text, x - i, y);
                    g2d.drawString(text, x, y + i);
                    g2d.drawString(text, x, y - i);
                }
                
                // Draw holographic scan lines for cyber effect
                for (int i = 0; i < getHeight(); i += 3) {
                    int alpha = 10 + (int)(5 * glowIntensity);
                    if (i % 6 == 0) alpha = 25 + (int)(10 * glowIntensity);
                    g2d.setColor(new Color(0, 255, 200, alpha));
                    g2d.drawLine(0, i, getWidth(), i);
                }
                
                // Main text with enhanced gradient and glow
                // Create a metallic gradient for the text
                LinearGradientPaint textGradient = new LinearGradientPaint(
                    x, y - fm.getHeight(),
                    x, y,
                    new float[]{0.0f, 0.3f, 0.7f, 1.0f},
                    new Color[]{
                        new Color(200, 255, 200), // Top: lighter cyan-green
                        new Color(0, 255, 200),   // Upper mid: bright cyan
                        new Color(0, 220, 180),   // Lower mid: medium cyan
                        new Color(0, 180, 160)    // Bottom: deeper cyan
                    }
                );
                
                g2d.setPaint(textGradient);
                g2d.drawString(text, x, y);
                
                // Additional high-tech digital artifacts
                if (Math.random() < 0.05) { // Occasionally show digital artifacts
                    g2d.setColor(new Color(0, 255, 255, 180));
                    int glitchX = (int)(Math.random() * getWidth());
                    int glitchWidth = (int)(Math.random() * 20) + 5;
                    int glitchHeight = (int)(Math.random() * 3) + 1;
                    g2d.fillRect(glitchX, (int)(Math.random() * getHeight()), 
                                glitchWidth, glitchHeight);
                }
                
                // Highlight effect on top with animated glow
                g2d.setColor(new Color(255, 255, 255, (int)(120 * glowIntensity)));
                g2d.drawString(text, x, y - 1);
            }
        };
        digitalClockLabel.setFont(new Font("Orbitron", Font.BOLD, 84)); // Changed to Orbitron for more tech look
        digitalClockLabel.setForeground(new Color(0, 255, 200)); // Changed to bright cyan
        digitalClockLabel.setOpaque(false);
        digitalClockLabel.setPreferredSize(new Dimension(0, 120)); // Increased height to accommodate larger font and effects
        
        // Date display with enhanced modern styling and bigger font
        dateLabel = createStyledLabel("Clock Stopped", new Color(135, 206, 250), 28); // Increased from 24 to 28
        
        // Milliseconds display with golden glow - bigger font (kept for potential future use)
        millisecondLabel = createStyledLabel("", new Color(255, 215, 0), 24); // Increased from 20 to 24
        
        // Alarm status with enhanced orange glow and bigger font
        alarmStatusLabel = createStyledLabel("No alarms set", new Color(255, 165, 0), 22); // Increased from 18 to 22
        
        // Stopwatch display with enhanced magenta glow and bigger font
        stopwatchLabel = new JLabel("00:00:00.000", SwingConstants.CENTER);
        stopwatchLabel.setFont(new Font("Segoe UI", Font.BOLD, 32)); // Increased from 28 to 32
        stopwatchLabel.setForeground(new Color(255, 20, 147));
        stopwatchLabel.setOpaque(false);
        
        // Weather display panel with modern styling
        JPanel weatherPanel = createWeatherPanel();
        
        digitalPanel.add(digitalClockLabel);
        digitalPanel.add(dateLabel);
        // digitalPanel.add(millisecondLabel); // Milliseconds display removed
        digitalPanel.add(alarmStatusLabel);
        digitalPanel.add(stopwatchLabel);
        digitalPanel.add(weatherPanel);
        
        // Create world clock panel
        worldScrollPane = createWorldClockPanel();
        
        // Create center panel to hold analog clock on top
        JPanel centerPanel = new JPanel(new BorderLayout(6, 8));
        centerPanel.setOpaque(false);
        centerPanel.add(analogClockPanel, BorderLayout.CENTER);
        
        // Add components to main display panel - Digital clock on left, analog clock in center, world clocks on right
        displayPanel.add(digitalPanel, BorderLayout.WEST);
        displayPanel.add(centerPanel, BorderLayout.CENTER);
        displayPanel.add(worldScrollPane, BorderLayout.EAST);
        
        return displayPanel;
    }
    
    /**
     * Create the world clock panel showing time in different cities
     */
    private JScrollPane createWorldClockPanel() {
        // Main container panel with BorderLayout
        JPanel worldContainer = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Modern gradient background
                GradientPaint gradient = new GradientPaint(
                    0, 0, new Color(30, 30, 60, 120),
                    0, getHeight(), new Color(15, 15, 30, 150)
                );
                g2d.setPaint(gradient);
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        worldContainer.setOpaque(false);
        
        // Create the cities panel with more compact layout
        JPanel worldPanel = new JPanel();
        worldPanel.setOpaque(false);
        worldPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 8, 6));
        
        // Store the reference to worldClockPanel
        worldClockPanel = worldPanel;
        
        // Add the default cities
        addCity("London", ZoneId.of("Europe/London"));
        addCity("New York", ZoneId.of("America/New_York"));
        addCity("Tokyo", ZoneId.of("Asia/Tokyo"));
        
        // Create the modern "+" button
        JButton addCityButton = new JButton("+") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Create gradient background
                GradientPaint gradient = new GradientPaint(
                    0, 0, new Color(70, 130, 180),
                    0, getHeight(), new Color(30, 90, 140)
                );
                g2d.setPaint(gradient);
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                
                // Add glowing border
                g2d.setColor(new Color(100, 149, 237, 150));
                g2d.setStroke(new BasicStroke(2));
                g2d.drawRoundRect(1, 1, getWidth()-2, getHeight()-2, 20, 20);
                
                super.paintComponent(g);
            }
        };
        addCityButton.setFont(new Font("Segoe UI", Font.BOLD, 24));
        addCityButton.setForeground(Color.WHITE);
        addCityButton.setFocusPainted(false);
        addCityButton.setBorderPainted(false);
        addCityButton.setContentAreaFilled(false);
        addCityButton.setPreferredSize(new Dimension(50, 50));
        addCityButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        addCityButton.setToolTipText("Add new city");
        
        // Add hover effect to the button
        addCityButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                addCityButton.repaint();
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                addCityButton.repaint();
            }
        });
        
        // Add functionality to add new cities using TimeZoneSelector
        addCityButton.addActionListener(_ -> {
            TimeZoneSelector selector = new TimeZoneSelector(AdvancedDigitalClock.this);
            selector.setVisible(true);
            
            // Process the selected time zones if any
            if (selector.isSelectionMade()) {
                for (TimeZoneSelector.TimeZoneItem item : selector.getSelectedTimeZones()) {
                    addCity(item.getName(), ZoneId.of(item.getZoneId()));
                }
                // Update layout after adding cities
                worldClockPanel.revalidate();
                worldClockPanel.repaint();
            }
        });
        
        // Create button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 15));
        buttonPanel.setOpaque(false);
        buttonPanel.add(addCityButton);
        
        // Add panels to container
        worldContainer.add(worldPanel, BorderLayout.CENTER);
        worldContainer.add(buttonPanel, BorderLayout.EAST);
        
        // Wrap the container in a scroll pane
        worldScrollPane = new JScrollPane(worldContainer);
        worldScrollPane.setBorder(null);
        worldScrollPane.setOpaque(false);
        worldScrollPane.getViewport().setOpaque(false);
        worldScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        worldScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        
        return worldScrollPane;
    }
    
    /**
     * Add a new city clock to the world panel
     */
    private void addCity(String cityName, ZoneId zoneId) {
        CityClockDisplay cityDisplay = new CityClockDisplay(cityName, zoneId);
        cityClocks.put(cityName, cityDisplay);
        worldClockPanel.add(cityDisplay.getPanel());
    }
    
    /**
     * Create the weather display panel
     */
    private JPanel createWeatherPanel() {
        JPanel weatherPanel = new JPanel(new GridLayout(2, 2, 4, 4)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Modern gradient background
                GradientPaint gradient = new GradientPaint(
                    0, 0, new Color(70, 130, 180, 100),
                    0, getHeight(), new Color(30, 90, 140, 130)
                );
                g2d.setPaint(gradient);
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                
                // Add subtle border
                g2d.setColor(new Color(100, 149, 237, 80));
                g2d.setStroke(new BasicStroke(1f));
                g2d.drawRoundRect(1, 1, getWidth()-2, getHeight()-2, 10, 10);
            }
        };
        weatherPanel.setOpaque(false);
        
        // Weather location and temperature with more compact styling
        weatherLocationLabel = new JLabel(currentWeatherCity, SwingConstants.CENTER);
        weatherLocationLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        weatherLocationLabel.setForeground(new Color(176, 196, 222));
        weatherLocationLabel.setOpaque(false);
        
        weatherTemperatureLabel = new JLabel("--°C", SwingConstants.CENTER);
        weatherTemperatureLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        weatherTemperatureLabel.setForeground(new Color(135, 206, 250));
        weatherTemperatureLabel.setOpaque(false);
        
        // Weather description and humidity with softer colors and smaller fonts
        weatherDescriptionLabel = new JLabel("Loading...", SwingConstants.CENTER);
        weatherDescriptionLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        weatherDescriptionLabel.setForeground(new Color(200, 200, 220));
        weatherDescriptionLabel.setOpaque(false);
        
        weatherHumidityLabel = new JLabel("--% humidity", SwingConstants.CENTER);
        weatherHumidityLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        weatherHumidityLabel.setForeground(new Color(200, 200, 220));
        weatherHumidityLabel.setOpaque(false);
        
        weatherPanel.add(weatherLocationLabel);
        weatherPanel.add(weatherTemperatureLabel);
        weatherPanel.add(weatherDescriptionLabel);
        weatherPanel.add(weatherHumidityLabel);
        
        return weatherPanel;
    }
    
    private JPanel createControlPanel() {
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 6));
        controlPanel.setBackground(Color.BLACK);
        controlPanel.setBorder(BorderFactory.createEmptyBorder(6, 0, 4, 0));
        
        // Start button with gradient green
        startButton = new JButton("START");
        styleButton(startButton, new Color(0, 230, 118), new Color(0, 200, 83), Color.WHITE, 16);
        
        // Pause button with gradient amber
        pauseButton = new JButton("PAUSE");
        styleButton(pauseButton, new Color(255, 213, 79), new Color(255, 179, 0), Color.BLACK, 16);
        pauseButton.setEnabled(false);
        
        // Stop button with gradient red
        stopButton = new JButton("STOP");
        styleButton(stopButton, new Color(255, 82, 82), new Color(213, 0, 0), Color.WHITE, 16);
        stopButton.setEnabled(false);
        
        // Alarm button with gradient purple
        alarmButton = new JButton("ALARM");
        styleButton(alarmButton, new Color(156, 39, 176), new Color(123, 31, 162), Color.WHITE, 16);
        
        // Stopwatch button with gradient orange
        stopwatchButton = new JButton("STOPWATCH");
        styleButton(stopwatchButton, new Color(255, 152, 0), new Color(230, 126, 34), Color.WHITE, 16);
        
        // Exit button with gradient blue-gray
        exitButton = new JButton("EXIT");
        styleButton(exitButton, new Color(129, 212, 250), new Color(3, 155, 229), Color.WHITE, 16);
        
        // Weather refresh button with gradient blue
        weatherRefreshButton = new JButton("WEATHER");
        styleButton(weatherRefreshButton, new Color(100, 149, 237), new Color(70, 130, 180), Color.WHITE, 16);
        
        controlPanel.add(startButton);
        controlPanel.add(pauseButton);
        controlPanel.add(stopButton);
        controlPanel.add(alarmButton);
        controlPanel.add(stopwatchButton);
        controlPanel.add(weatherRefreshButton);
        controlPanel.add(exitButton);
        
        return controlPanel;
    }
    
    private void setupEventHandlers() {
        startButton.addActionListener(_ -> startClock());
        pauseButton.addActionListener(_ -> pauseClock());
        stopButton.addActionListener(_ -> stopClock());
        alarmButton.addActionListener(_ -> showAlarmDialog());
        stopwatchButton.addActionListener(_ -> showStopwatchDialog());
        weatherRefreshButton.addActionListener(_ -> showWeatherDialog());
        exitButton.addActionListener(_ -> exitApplication());
    }
    
    private void startClock() {
        if (executorService == null || executorService.isShutdown()) {
            initializeThreadPool(); // Re-initialize if it was shut down
        }
        
        if (!isRunning) {
            isRunning = true;
            isPaused = false;
            
            // Schedule the main clock task, 1-second interval
            clockTask = executorService.scheduleAtFixedRate(
                new ClockTask(), 0, 1, TimeUnit.SECONDS);
            
            // Millisecond task removed to simplify display
            
            // Start world clock updates
            worldClockTaskHandle = executorService.scheduleAtFixedRate(
                this::updateWorldClocks, 0, 1, TimeUnit.SECONDS);
            
            // Start weather updates (every 10 minutes)
            weatherTask = executorService.scheduleAtFixedRate(
                this::updateWeather, 0, 10, TimeUnit.MINUTES);
            
            updateButtonStates(false, true, true);
        } else if (isPaused) {
            isPaused = false; // Tasks will resume checking this flag
            System.out.println("Clock resumed from pause");
        }
        updateButtonStates(false, true, true);
        if (analogClockPanel != null) {
            analogClockPanel.repaint(); // Ensure analog clock face updates if it was static
        }
    }
    
    private void pauseClock() {
        if (isRunning && !isPaused) {
            isPaused = true;
            // Tasks will see isPaused = true and skip their main logic
            System.out.println("Clock paused");
            updateButtonStates(true, false, true);
        }
    }
    
    private void stopClock() {
        if (isRunning) {
            isRunning = false;
            isPaused = false;
            
            // Cancel all running tasks
            if (clockTask != null) {
                clockTask.cancel(true);
                clockTask = null;
            }
            
            // Millisecond task removed
            
            if (worldClockTaskHandle != null) {
                worldClockTaskHandle.cancel(true);
                worldClockTaskHandle = null;
            }
            
            if (weatherTask != null) {
                weatherTask.cancel(true);
                weatherTask = null;
            }
            
            // Reset display
            digitalClockLabel.setText("--:--:--");
            dateLabel.setText("Clock Stopped");
            // millisecondLabel.setText(""); // Millisecond display removed
            analogClockPanel.repaint();
            
            updateButtonStates(true, false, false);
        }
    }
    
    /**
     * Update all world clock displays
     */
    private void updateWorldClocks() {
        if (!isRunning || isPaused) return;
        
        for (CityClockDisplay cityDisplay : cityClocks.values()) {
            cityDisplay.update();
        }
    }
    
    private void updateButtonStates(boolean start, boolean pause, boolean stop) {
        SwingUtilities.invokeLater(() -> {
            startButton.setEnabled(start);
            pauseButton.setEnabled(pause);
            stopButton.setEnabled(stop);
        });
    }
    
    /**
     * Apply a modern button style with gradient background
     * @param button The button to style
     * @param topColor The top color for gradient
     * @param bottomColor The bottom color for gradient
     * @param textColor The text color
     * @param fontSize The font size
     */
    private void styleButton(JButton button, Color topColor, Color bottomColor, Color textColor, int fontSize) {
        button.setFont(new Font("Arial", Font.BOLD, fontSize));
        button.setForeground(textColor);
        button.setFocusPainted(false);
        button.setBorderPainted(true);
        button.setContentAreaFilled(false);
        button.setOpaque(false);
        
        // Create a custom painted button with gradient
        button.setUI(new BasicButtonUI() {
            @Override
            public void paint(Graphics g, JComponent c) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                int w = c.getWidth();
                int h = c.getHeight();
                
                // Create gradient paint
                GradientPaint gp = new GradientPaint(
                    0, 0, topColor,
                    0, h, bottomColor);
                
                g2d.setPaint(gp);
                g2d.fillRoundRect(0, 0, w, h, 12, 12);
                
                // Draw border
                g2d.setColor(bottomColor.darker());
                g2d.setStroke(new BasicStroke(2));
                g2d.drawRoundRect(0, 0, w - 1, h - 1, 12, 12);
                
                // Add shine effect
                g2d.setColor(new Color(255, 255, 255, 50));
                g2d.fillRoundRect(2, 2, w - 4, h / 2 - 2, 10, 10);
                
                g2d.dispose();
                
                super.paint(g, c);
            }
        });
        
        // Add hover effect
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(bottomColor.brighter(), 2),
                    BorderFactory.createEmptyBorder(8, 15, 8, 15)));
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                button.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(bottomColor, 2),
                    BorderFactory.createEmptyBorder(8, 15, 8, 15)));
            }
        });
        
        // Set margins and border
        button.setMargin(new Insets(8, 15, 8, 15));
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(bottomColor, 2),
            BorderFactory.createEmptyBorder(8, 15, 8, 15)));
    }
    
    /**
     * Helper method to create styled labels with enhanced glowing backgrounds and borders
     */
    private JLabel createStyledLabel(String text, Color glowColor, int fontSize) {
        JLabel label = new JLabel(text, SwingConstants.CENTER) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                
                // Enhanced rounded background with deeper gradient
                int arc = 12;
                GradientPaint bgGradient = new GradientPaint(
                    0, 0, new Color(25, 25, 50, 150),
                    0, getHeight(), new Color(15, 15, 35, 180)
                );
                g2d.setPaint(bgGradient);
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
                
                // Add enhanced glowing border with multiple layers
                for (int i = 0; i < 3; i++) {
                    int alpha = 80 - (i * 25);
                    g2d.setColor(new Color(glowColor.getRed(), glowColor.getGreen(), glowColor.getBlue(), alpha));
                    g2d.setStroke(new BasicStroke(2 + i, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2d.drawRoundRect(i, i, getWidth()-(2*i)-1, getHeight()-(2*i)-1, arc-i, arc-i);
                }
                
                // Add inner highlight for depth
                g2d.setColor(new Color(255, 255, 255, 20));
                g2d.setStroke(new BasicStroke(1));
                g2d.drawRoundRect(2, 2, getWidth()-4, getHeight()-4, arc-2, arc-2);
                
                super.paintComponent(g);
            }
        };
        
        label.setFont(new Font("Segoe UI", Font.BOLD, fontSize));
        label.setForeground(glowColor);
        label.setOpaque(false);
        label.setPreferredSize(new Dimension(0, 50)); // Increased height from 40 to 50 for bigger enhanced labels
        
        return label;
    }

    // ...existing code...
    
    private void exitApplication() {
        stopClock(); // Stop tasks before exiting
        
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(2, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                    System.out.println("Force shutdown of thread pool");
                } else {
                    System.out.println("Thread pool shutdown gracefully");
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        // Dispose the JFrame
        dispose();
        System.exit(0);
    }
    
    private String formatTime(ZonedDateTime zonedDateTime) {
        return timeFormat.format(Date.from(zonedDateTime.toInstant()));
    }

    private String formatDate(ZonedDateTime zonedDateTime) {
        return dateFormat.format(Date.from(zonedDateTime.toInstant()));
    }

    private String formatMillisecond(ZonedDateTime zonedDateTime) {
        // Get milliseconds from the ZonedDateTime
        int millis = zonedDateTime.getNano() / 1_000_000;
        return String.format(".%03d", millis);
    }
    
    /**
     * Update weather information from API
     */
    private void updateWeather() {
        if (!isRunning || isPaused) return;
        
        executorService.submit(() -> {
            try {
                String weatherData = fetchWeatherData(currentWeatherCity);
                if (weatherData != null) {
                    parseAndDisplayWeather(weatherData);
                }
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    weatherDescriptionLabel.setText("Weather unavailable");
                    weatherTemperatureLabel.setText("--°C");
                    weatherHumidityLabel.setText("--% humidity");
                });
                System.err.println("Weather update failed: " + e.getMessage());
            }
        });
    }
    
    /**
     * Fetch weather data from OpenWeatherMap API
     */
    private String fetchWeatherData(String city) {
        try {
            // Note: Replace "your-api-key" with an actual OpenWeatherMap API key
            // For demo purposes, we'll simulate weather data
            return simulateWeatherData(city);
            
            /* Uncomment this section when you have a real API key:
            String apiUrl = "http://api.openweathermap.org/data/2.5/weather?q=" + 
                           city + "&appid=" + WEATHER_API_KEY + "&units=metric";
            
            URI uri = URI.create(apiUrl);
            URL url = uri.toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                
                return response.toString();
            } else {
                System.err.println("Weather API error: " + responseCode);
                return null;
            }
            */
        } catch (Exception e) {
            System.err.println("Error fetching weather data: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Simulate weather data for demo purposes
     */
    private String simulateWeatherData(String city) {
        // Simulate different weather for different cities
        double temp = 15 + (city.hashCode() % 20); // Temperature between 15-35°C
        int humidity = 40 + (Math.abs(city.hashCode()) % 40); // Humidity between 40-80%
        String[] conditions = {"Clear Sky", "Few Clouds", "Scattered Clouds", "Broken Clouds", "Light Rain", "Sunny"};
        String condition = conditions[Math.abs(city.hashCode()) % conditions.length];
        
        return String.format("CITY:%s|TEMP:%.1f|HUMIDITY:%d|DESC:%s", 
                           city, temp, humidity, condition);
    }
    
    /**
     * Parse weather data and update display (simplified format)
     */
    private void parseAndDisplayWeather(String weatherData) {
        try {
            if (weatherData.startsWith("CITY:")) {
                // Parse our simulated format: CITY:London|TEMP:22.5|HUMIDITY:65|DESC:Clear Sky
                String[] parts = weatherData.split("\\|");
                String cityName = parts[0].substring(5); // Remove "CITY:"
                double temperature = Double.parseDouble(parts[1].substring(5)); // Remove "TEMP:"
                int humidity = Integer.parseInt(parts[2].substring(9)); // Remove "HUMIDITY:"
                String description = parts[3].substring(5); // Remove "DESC:"
                
                // Update UI on EDT
                SwingUtilities.invokeLater(() -> {
                    weatherLocationLabel.setText(cityName);
                    weatherTemperatureLabel.setText(String.format("%.1f°C", temperature));
                    weatherDescriptionLabel.setText(description);
                    weatherHumidityLabel.setText(humidity + "% humidity");
                });
            }
            
            /* For real JSON data, you would use a JSON library:
            JSONObject json = new JSONObject(jsonData);
            JSONObject main = json.getJSONObject("main");
            JSONObject weather = json.getJSONArray("weather").getJSONObject(0);
            // ... parse as before
            */
            
        } catch (Exception e) {
            System.err.println("Error parsing weather data: " + e.getMessage());
            SwingUtilities.invokeLater(() -> {
                weatherDescriptionLabel.setText("Data error");
            });
        }
    }
    
    /**
     * Utility method to capitalize words
     */
    private String capitalizeWords(String text) {
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = true;
        
        for (char c : text.toCharArray()) {
            if (Character.isWhitespace(c)) {
                capitalizeNext = true;
                result.append(c);
            } else if (capitalizeNext) {
                result.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                result.append(c);
            }
        }
        
        return result.toString();
    }
    
    /**
     * Show weather configuration dialog
     */
    private void showWeatherDialog() {
        JDialog weatherDialog = new JDialog(this, "Weather Settings", true);
        weatherDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        weatherDialog.setSize(400, 200);
        weatherDialog.setLocationRelativeTo(this);
        weatherDialog.getContentPane().setBackground(Color.BLACK);
        
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.BLACK);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        
        // City input
        JLabel cityLabel = new JLabel("Enter City:");
        cityLabel.setForeground(Color.WHITE);
        cityLabel.setFont(new Font("Arial", Font.BOLD, 14));
        
        weatherCityField = new JTextField(currentWeatherCity, 15);
        weatherCityField.setBackground(new Color(40, 40, 40));
        weatherCityField.setForeground(Color.WHITE);
        weatherCityField.setCaretColor(Color.WHITE);
        weatherCityField.setBorder(BorderFactory.createLineBorder(new Color(100, 149, 237), 2));
        
        // Buttons
        JButton updateButton = new JButton("Update Weather");
        styleButton(updateButton, new Color(100, 149, 237), new Color(70, 130, 180), Color.WHITE, 12);
        
        JButton cancelButton = new JButton("Cancel");
        styleButton(cancelButton, new Color(128, 128, 128), new Color(96, 96, 96), Color.WHITE, 12);
        
        // Layout
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(cityLabel, gbc);
        
        gbc.gridx = 1;
        panel.add(weatherCityField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(updateButton, gbc);
        
        gbc.gridx = 1;
        panel.add(cancelButton, gbc);
        
        // Event handlers
        updateButton.addActionListener(_ -> {
            String newCity = weatherCityField.getText().trim();
            if (!newCity.isEmpty()) {
                currentWeatherCity = newCity;
                updateWeather(); // Immediate update
                weatherDialog.dispose();
            }
        });
        
        cancelButton.addActionListener(_ -> weatherDialog.dispose());
        
        // Enter key support
        weatherCityField.addActionListener(_ -> updateButton.doClick());
        
        weatherDialog.add(panel);
        weatherDialog.setVisible(true);
    }
    
    /**
     * Show alarm dialog for setting alarms - Ultra Modern Cyber Design
     */
    private void showAlarmDialog() {
        JDialog alarmDialog = new JDialog(this, "⏰ Cyber Alarm Command Center", true);
        alarmDialog.setLayout(new BorderLayout());
        alarmDialog.setSize(650, 800);
        alarmDialog.setLocationRelativeTo(this);
        alarmDialog.setResizable(false);
        alarmDialog.setUndecorated(true);
        alarmDialog.getRootPane().setWindowDecorationStyle(JRootPane.NONE);
        
        // Create animated main panel with cyber gradient background
        JPanel mainPanel = new JPanel(new BorderLayout(20, 20)) {
            private float animationPhase = 0.0f;
            private Timer animationTimer;
            
            {
                // Start background animation
                animationTimer = new Timer(50, e -> {
                    animationPhase += 0.02f;
                    if (animationPhase > 2 * Math.PI) animationPhase = 0.0f;
                    repaint();
                });
                animationTimer.start();
            }
            
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                
                // Animated cyber gradient background
                float intensity = 0.3f + 0.1f * (float)Math.sin(animationPhase);
                RadialGradientPaint cyberGradient = new RadialGradientPaint(
                    getWidth() / 2, getHeight() / 3, Math.max(getWidth(), getHeight()),
                    new float[]{0.0f, 0.4f, 0.7f, 1.0f},
                    new Color[]{
                        new Color(10, 25, 47),           // Deep space blue
                        new Color(15, 15, 35),           // Dark cyber
                        new Color(25, 15, 45),           // Purple edge
                        new Color(5, 5, 15)              // Almost black
                    }
                );
                g2d.setPaint(cyberGradient);
                g2d.fillRect(0, 0, getWidth(), getHeight());
                
                // Add animated cyber grid pattern
                g2d.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                for (int i = 0; i < getWidth(); i += 30) {
                    float alpha = 0.1f + 0.05f * (float)Math.sin(animationPhase + i * 0.01f);
                    g2d.setColor(new Color(0, 255, 255, (int)(alpha * 255)));
                    g2d.drawLine(i, 0, i, getHeight());
                }
                for (int i = 0; i < getHeight(); i += 30) {
                    float alpha = 0.1f + 0.05f * (float)Math.sin(animationPhase + i * 0.01f);
                    g2d.setColor(new Color(0, 255, 255, (int)(alpha * 255)));
                    g2d.drawLine(0, i, getWidth(), i);
                }
                
                // Animated border glow
                float glowIntensity = 0.6f + 0.3f * (float)Math.sin(animationPhase * 2);
                for (int i = 0; i < 5; i++) {
                    g2d.setColor(new Color(0, 255, 255, (int)(glowIntensity * 30 / (i + 1))));
                    g2d.setStroke(new BasicStroke(3 + i, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2d.drawRoundRect(i, i, getWidth() - 2*i - 1, getHeight() - 2*i - 1, 20, 20);
                }
            }
        };
        mainPanel.setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));
        
        // Ultra Modern title panel with holographic effect
        JPanel titlePanel = createCyberPanel();
        titlePanel.setPreferredSize(new Dimension(600, 80));
        
        JLabel titleLabel = new JLabel("⚡ CYBER ALARM COMMAND CENTER ⚡", SwingConstants.CENTER) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                
                // Create glowing text effect
                String text = getText();
                FontMetrics fm = g2d.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(text)) / 2;
                int y = (getHeight() + fm.getAscent()) / 2;
                
                // Draw glow layers
                for (int i = 8; i > 0; i--) {
                    g2d.setColor(new Color(0, 255, 255, 20));
                    g2d.drawString(text, x - i/2, y - i/2);
                    g2d.drawString(text, x + i/2, y + i/2);
                }
                
                // Draw main text
                g2d.setColor(new Color(0, 255, 255));
                g2d.drawString(text, x, y);
                
                // Add scanlines effect
                g2d.setColor(new Color(0, 255, 255, 50));
                for (int i = 0; i < getHeight(); i += 4) {
                    g2d.drawLine(0, i, getWidth(), i);
                }
            }
        };
        titleLabel.setFont(new Font("Orbitron", Font.BOLD, 24));
        titleLabel.setOpaque(false);
        titlePanel.add(titleLabel, BorderLayout.CENTER);
        
        // Enhanced alarm list with cyber styling
        DefaultListModel<AlarmTime> listModel = new DefaultListModel<>();
        for (AlarmTime alarm : alarms) {
            listModel.addElement(alarm);
        }
        
        JList<AlarmTime> alarmList = new JList<>(listModel);
        alarmList.setBackground(new Color(5, 5, 15, 200));
        alarmList.setForeground(Color.WHITE);
        alarmList.setFont(new Font("Consolas", Font.BOLD, 14));
        alarmList.setSelectionBackground(new Color(0, 255, 255, 80));
        alarmList.setSelectionForeground(Color.WHITE);
        alarmList.setFixedCellHeight(45);
        alarmList.setBorder(BorderFactory.createLineBorder(new Color(0, 255, 255), 2));
        
        // Custom cell renderer for alarm list
        alarmList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                
                if (value instanceof AlarmTime) {
                    AlarmTime alarm = (AlarmTime) value;
                    String status = alarm.isEnabled() ? "🔔 ACTIVE" : "🔕 INACTIVE";
                    String repeatIcon = alarm.isRepeating() ? "🔄" : "📅";
                    
                    setText(String.format("<html><div style='padding:5px;'>" +
                            "<span style='color:#00FFFF; font-size:16px; font-weight:bold;'>%s</span><br/>" +
                            "<span style='color:#FFFFFF; font-size:12px;'>%s %s | %s</span>" +
                            "</div></html>", 
                            alarm.getTimeString(), alarm.getLabel(), repeatIcon, status));
                }
                
                if (isSelected) {
                    setBackground(new Color(0, 255, 255, 100));
                    setBorder(BorderFactory.createLineBorder(new Color(0, 255, 255), 2));
                } else {
                    setBackground(new Color(10, 10, 25, 150));
                    setBorder(BorderFactory.createLineBorder(new Color(0, 255, 255, 100), 1));
                }
                setOpaque(true);
                
                return this;
            }
        });
        
        JScrollPane scrollPane = new JScrollPane(alarmList);
        scrollPane.setPreferredSize(new Dimension(580, 250));
        scrollPane.setBackground(new Color(5, 5, 15));
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(0, 255, 255), 2));
        scrollPane.getViewport().setBackground(new Color(5, 5, 15));
        
        // Ultra-modern input panel with cyber styling
        JPanel inputPanel = createCyberPanel();
        inputPanel.setLayout(new GridBagLayout());
        inputPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(0, 255, 255), 2),
            "⚙️ NEW ALARM CONFIGURATION",
            javax.swing.border.TitledBorder.CENTER,
            javax.swing.border.TitledBorder.TOP,
            new Font("Orbitron", Font.BOLD, 16),
            new Color(0, 255, 255)
        ));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 15, 10, 15);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // Time input
        gbc.gridx = 0; gbc.gridy = 0;
        inputPanel.add(createCyberLabel("⏰ TIME (HH:MM):"), gbc);
        gbc.gridx = 1;
        JTextField timeField = createCyberTextField("07:00");
        inputPanel.add(timeField, gbc);
        
        // Label input
        gbc.gridx = 0; gbc.gridy = 1;
        inputPanel.add(createCyberLabel("🏷️ LABEL:"), gbc);
        gbc.gridx = 1;
        JTextField labelField = createCyberTextField("Wake Up");
        inputPanel.add(labelField, gbc);
        
        // Enabled checkbox
        gbc.gridx = 0; gbc.gridy = 2;
        inputPanel.add(createCyberLabel("🔔 ENABLED:"), gbc);
        gbc.gridx = 1;
        JCheckBox enabledBox = createCyberCheckBox(true);
        inputPanel.add(enabledBox, gbc);
        
        // Repeat checkbox
        gbc.gridx = 0; gbc.gridy = 3;
        inputPanel.add(createCyberLabel("🔄 REPEAT:"), gbc);
        gbc.gridx = 1;
        JCheckBox repeatBox = createCyberCheckBox(false);
        inputPanel.add(repeatBox, gbc);
        
        // Sound selection dropdown
        gbc.gridx = 0; gbc.gridy = 4;
        inputPanel.add(createCyberLabel("🔊 ALARM SOUND:"), gbc);
        gbc.gridx = 1;
        String[] soundOptions = {
            "Alarm 1 (Default)", "Alarm 2", "Alarm 3", "Alarm 4", 
            "Alarm 5", "Alarm 6", "Alarm 7", "Alarm 8"
        };
        JComboBox<String> soundComboBox = createCyberComboBox(soundOptions);
        inputPanel.add(soundComboBox, gbc);
        
        // Ultra-modern button panel with cyber effects
        JPanel buttonPanel = createCyberPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 20, 15));
        
        JButton addButton = createCyberButton("⚡ ADD ALARM", new Color(0, 255, 100));
        JButton removeButton = createCyberButton("🗑️ REMOVE", new Color(255, 50, 50));
        JButton closeButton = createCyberButton("❌ CLOSE", new Color(150, 150, 150));
        
        // Enhanced button functionality with visual feedback
        addButton.addActionListener(e -> {
            try {
                String timeStr = timeField.getText().trim();
                String label = labelField.getText().trim();
                if (label.isEmpty()) label = "Alarm";
                
                LocalTime time = LocalTime.parse(timeStr);
                int soundNumber = soundComboBox.getSelectedIndex() + 1; // Convert 0-based index to 1-based sound number
                AlarmTime newAlarm = new AlarmTime(time, label, enabledBox.isSelected(), repeatBox.isSelected(), soundNumber);
                alarms.add(newAlarm);
                listModel.addElement(newAlarm);
                updateAlarmStatus();
                
                // Reset form with animation
                timeField.setText("07:00");
                labelField.setText("Wake Up");
                enabledBox.setSelected(true);
                repeatBox.setSelected(false);
                soundComboBox.setSelectedIndex(0); // Reset to first alarm sound
                
                // Visual feedback
                addButton.setBackground(new Color(0, 255, 100, 200));
                Timer resetTimer = new Timer(200, evt -> addButton.setBackground(new Color(0, 255, 100)));
                resetTimer.setRepeats(false);
                resetTimer.start();
                
            } catch (Exception ex) {
                // Create custom cyber-styled error dialog
                showCyberErrorDialog(alarmDialog, "⚠️ INVALID TIME FORMAT", 
                    "Please use HH:mm format (e.g., 07:30)");
            }
        });
        
        removeButton.addActionListener(e -> {
            int selected = alarmList.getSelectedIndex();
            if (selected >= 0) {
                alarms.remove(selected);
                listModel.remove(selected);
                updateAlarmStatus();
                
                // Visual feedback
                removeButton.setBackground(new Color(255, 50, 50, 200));
                Timer resetTimer = new Timer(200, evt -> removeButton.setBackground(new Color(255, 50, 50)));
                resetTimer.setRepeats(false);
                resetTimer.start();
            }
        });
        
        closeButton.addActionListener(e -> {
            // Stop the animation timer to prevent memory leaks
            if (mainPanel instanceof JPanel) {
                Component[] components = mainPanel.getComponents();
                for (Component comp : components) {
                    if (comp instanceof JPanel) {
                        // Stop any running timers in the animated panel
                        try {
                            java.lang.reflect.Field[] fields = comp.getClass().getDeclaredFields();
                            for (java.lang.reflect.Field field : fields) {
                                if (field.getType() == Timer.class) {
                                    field.setAccessible(true);
                                    Timer timer = (Timer) field.get(comp);
                                    if (timer != null && timer.isRunning()) {
                                        timer.stop();
                                    }
                                }
                            }
                        } catch (Exception ignored) {
                            // Safely ignore reflection errors
                        }
                    }
                }
            }
            alarmDialog.dispose();
        });
        
        buttonPanel.add(addButton);
        buttonPanel.add(removeButton);
        buttonPanel.add(closeButton);
        
        // Assemble the dialog
        mainPanel.add(titlePanel, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        
        JPanel bottomContainer = createCyberPanel();
        bottomContainer.setLayout(new BorderLayout(10, 10));
        bottomContainer.add(inputPanel, BorderLayout.CENTER);
        bottomContainer.add(buttonPanel, BorderLayout.SOUTH);
        
        mainPanel.add(bottomContainer, BorderLayout.SOUTH);
        alarmDialog.add(mainPanel);
        
        // Set up proper window closing behavior
        alarmDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        alarmDialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                // Stop animation timer when window is closed
                try {
                    java.lang.reflect.Field animationTimerField = mainPanel.getClass().getDeclaredField("animationTimer");
                    animationTimerField.setAccessible(true);
                    Timer animationTimer = (Timer) animationTimerField.get(mainPanel);
                    if (animationTimer != null && animationTimer.isRunning()) {
                        animationTimer.stop();
                    }
                } catch (Exception ignored) {
                    // Safely ignore reflection errors
                }
                alarmDialog.dispose();
            }
        });
        
        alarmDialog.setOpacity(0.98f);
        alarmDialog.setVisible(true);
    }
    
    /**
     * Show stopwatch dialog - Modern Gaming Design
     */
    private void showStopwatchDialog() {
        JDialog stopwatchDialog = new JDialog(this, "⏱️ Precision Stopwatch", true);
        stopwatchDialog.setLayout(new BorderLayout());
        stopwatchDialog.setSize(500, 600);
        stopwatchDialog.setLocationRelativeTo(this);
        stopwatchDialog.setResizable(false);
        stopwatchDialog.setUndecorated(true);
        stopwatchDialog.getRootPane().setWindowDecorationStyle(JRootPane.NONE);
        
        // Main panel with gradient background
        JPanel mainPanel = new JPanel(new BorderLayout(15, 15)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Orange gradient background
                RadialGradientPaint gradient = new RadialGradientPaint(
                    getWidth() / 2, getHeight() / 3, Math.max(getWidth(), getHeight()),
                    new float[]{0.0f, 0.6f, 1.0f},
                    new Color[]{
                        new Color(40, 25, 15),      // Dark orange center
                        new Color(25, 15, 10),      // Darker mid
                        new Color(15, 10, 5)        // Almost black edge
                    }
                );
                g2d.setPaint(gradient);
                g2d.fillRect(0, 0, getWidth(), getHeight());
                
                // Border glow
                g2d.setColor(new Color(255, 165, 0, 100));
                g2d.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2d.drawRoundRect(3, 3, getWidth() - 6, getHeight() - 6, 15, 15);
            }
        };
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Title panel
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setOpaque(false);
        JLabel titleLabel = new JLabel("⏱️ PRECISION STOPWATCH ⏱️", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Impact", Font.BOLD, 20));
        titleLabel.setForeground(new Color(255, 165, 0));
        titlePanel.add(titleLabel, BorderLayout.CENTER);
        
        // Large stopwatch display
        JLabel displayLabel = new JLabel("00:00:00.000", SwingConstants.CENTER);
        displayLabel.setFont(new Font("Consolas", Font.BOLD, 36));
        displayLabel.setForeground(Color.WHITE);
        displayLabel.setOpaque(true);
        displayLabel.setBackground(new Color(20, 20, 20));
        displayLabel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(255, 165, 0), 3),
            BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));
        
        // Control buttons panel
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        controlPanel.setOpaque(false);
        
        stopwatchStartButton = createStopwatchButton("▶ START", new Color(0, 200, 0));
        stopwatchStopButton = createStopwatchButton("⏸ STOP", new Color(200, 0, 0));
        stopwatchResetButton = createStopwatchButton("↻ RESET", new Color(150, 150, 150));
        stopwatchLapButton = createStopwatchButton("⏲ LAP", new Color(0, 150, 200));
        
        stopwatchStopButton.setEnabled(false);
        stopwatchLapButton.setEnabled(false);
        
        controlPanel.add(stopwatchStartButton);
        controlPanel.add(stopwatchStopButton);
        controlPanel.add(stopwatchResetButton);
        controlPanel.add(stopwatchLapButton);
        
        // Lap times panel
        JPanel lapPanel = new JPanel(new BorderLayout(5, 5));
        lapPanel.setOpaque(false);
        lapPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(255, 165, 0), 2),
            "LAP TIMES",
            javax.swing.border.TitledBorder.CENTER,
            javax.swing.border.TitledBorder.TOP,
            new Font("Impact", Font.BOLD, 14),
            new Color(255, 165, 0)
        ));
        
        lapTimesModel = new DefaultListModel<>();
        lapTimesList = new JList<>(lapTimesModel);
        lapTimesList.setFont(new Font("Consolas", Font.PLAIN, 14));
        lapTimesList.setBackground(new Color(20, 20, 20));
        lapTimesList.setForeground(new Color(255, 165, 0));
        lapTimesList.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        
        JScrollPane lapScrollPane = new JScrollPane(lapTimesList);
        lapScrollPane.setPreferredSize(new Dimension(400, 150));
        lapScrollPane.setBorder(BorderFactory.createLineBorder(new Color(255, 165, 0), 1));
        lapScrollPane.getViewport().setBackground(new Color(20, 20, 20));
        
        lapPanel.add(lapScrollPane, BorderLayout.CENTER);
        
        // Close button
        JButton closeButton = createStopwatchButton("❌ CLOSE", new Color(100, 100, 100));
        JPanel closePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        closePanel.setOpaque(false);
        closePanel.add(closeButton);
        
        // Event handlers
        stopwatchStartButton.addActionListener(_ -> startStopwatch(displayLabel));
        stopwatchStopButton.addActionListener(_ -> stopStopwatch());
        stopwatchResetButton.addActionListener(_ -> resetStopwatch(displayLabel));
        stopwatchLapButton.addActionListener(_ -> recordLap());
        closeButton.addActionListener(_ -> {
            stopStopwatch();
            stopwatchDialog.dispose();
        });
        
        // Assemble dialog
        mainPanel.add(titlePanel, BorderLayout.NORTH);
        
        JPanel centerContent = new JPanel(new BorderLayout(10, 10));
        centerContent.setOpaque(false);
        centerContent.add(displayLabel, BorderLayout.NORTH);
        centerContent.add(controlPanel, BorderLayout.CENTER);
        centerContent.add(lapPanel, BorderLayout.SOUTH);
        
        mainPanel.add(centerContent, BorderLayout.CENTER);
        mainPanel.add(closePanel, BorderLayout.SOUTH);
        
        stopwatchDialog.add(mainPanel);
        stopwatchDialog.setOpacity(0.98f);
        stopwatchDialog.setVisible(true);
        
        // Start updating the display if stopwatch is already running
        if (stopwatchRunning) {
            startStopwatchDisplayUpdate(displayLabel);
        } else {
            updateStopwatchDisplay(displayLabel);
        }
    }
    
    /**
     * Create a styled stopwatch button with gradient effects
     */
    private JButton createStopwatchButton(String text, Color baseColor) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Create gradient background
                GradientPaint gradient = new GradientPaint(
                    0, 0, baseColor.brighter(),
                    0, getHeight(), baseColor.darker()
                );
                g2d.setPaint(gradient);
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                
                // Add subtle glow effect
                g2d.setColor(new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), 80));
                g2d.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2d.drawRoundRect(1, 1, getWidth()-2, getHeight()-2, 12, 12);
                
                super.paintComponent(g);
            }
        };
        
        button.setFont(new Font("Arial", Font.BOLD, 12));
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setBackground(baseColor);
        button.setPreferredSize(new Dimension(100, 35));
        
        // Add hover effects
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(baseColor.brighter());
                button.repaint();
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(baseColor);
                button.repaint();
            }
        });
        
        return button;
    }
    
    /**
     * Start the stopwatch
     */
    private void startStopwatch(JLabel displayLabel) {
        if (!stopwatchRunning) {
            stopwatchRunning = true;
            stopwatchStartTime = System.currentTimeMillis() - stopwatchElapsedTime;
            
            // Update button states
            stopwatchStartButton.setEnabled(false);
            stopwatchStopButton.setEnabled(true);
            stopwatchLapButton.setEnabled(true);
            
            // Start the display update task
            startStopwatchDisplayUpdate(displayLabel);
            
            System.out.println("Stopwatch started");
        }
    }
    
    /**
     * Stop the stopwatch
     */
    private void stopStopwatch() {
        if (stopwatchRunning) {
            stopwatchRunning = false;
            stopwatchElapsedTime = System.currentTimeMillis() - stopwatchStartTime;
            
            // Cancel the update task
            if (stopwatchTask != null) {
                stopwatchTask.cancel(true);
                stopwatchTask = null;
            }
            
            // Update button states
            stopwatchStartButton.setEnabled(true);
            stopwatchStopButton.setEnabled(false);
            stopwatchLapButton.setEnabled(false);
            
            System.out.println("Stopwatch stopped");
        }
    }
    
    /**
     * Reset the stopwatch
     */
    private void resetStopwatch(JLabel displayLabel) {
        stopStopwatch(); // Stop if running
        
        stopwatchElapsedTime = 0;
        stopwatchStartTime = 0;
        lapTimes.clear();
        
        // Clear lap times display
        if (lapTimesModel != null) {
            lapTimesModel.clear();
        }
        
        // Update display
        updateStopwatchDisplay(displayLabel);
        
        // Reset button states
        stopwatchStartButton.setEnabled(true);
        stopwatchStopButton.setEnabled(false);
        stopwatchLapButton.setEnabled(false);
        
        System.out.println("Stopwatch reset");
    }
    
    /**
     * Record a lap time
     */
    private void recordLap() {
        if (stopwatchRunning) {
            long currentTime = System.currentTimeMillis() - stopwatchStartTime;
            lapTimes.add(currentTime);
            
            // Add to display list
            if (lapTimesModel != null) {
                int lapNumber = lapTimes.size();
                String lapTimeStr = formatStopwatchTime(currentTime);
                String lapDisplay = String.format("Lap %d: %s", lapNumber, lapTimeStr);
                lapTimesModel.addElement(lapDisplay);
                
                // Auto-scroll to the latest lap
                if (lapTimesList != null) {
                    lapTimesList.ensureIndexIsVisible(lapTimesModel.getSize() - 1);
                }
            }
            
            System.out.println("Lap recorded: " + formatStopwatchTime(currentTime));
        }
    }
    
    /**
     * Start the stopwatch display update task
     */
    private void startStopwatchDisplayUpdate(JLabel displayLabel) {
        if (executorService != null && !executorService.isShutdown()) {
            stopwatchTask = executorService.scheduleAtFixedRate(() -> {
                if (stopwatchRunning) {
                    updateStopwatchDisplay(displayLabel);
                }
            }, 0, 10, TimeUnit.MILLISECONDS); // Update every 10ms for smooth display
        }
    }
    
    /**
     * Update the stopwatch display
     */
    private void updateStopwatchDisplay(JLabel displayLabel) {
        long currentElapsed;
        if (stopwatchRunning) {
            currentElapsed = System.currentTimeMillis() - stopwatchStartTime;
        } else {
            currentElapsed = stopwatchElapsedTime;
        }
        
        String timeString = formatStopwatchTime(currentElapsed);
        
        SwingUtilities.invokeLater(() -> {
            if (displayLabel != null) {
                displayLabel.setText(timeString);
            }
            
            // Also update the main stopwatch label on the main window
            if (stopwatchLabel != null) {
                stopwatchLabel.setText(timeString);
            }
        });
    }
    
    /**
     * Format time in milliseconds to HH:MM:SS.mmm format
     */
    private String formatStopwatchTime(long timeInMillis) {
        long hours = timeInMillis / 3600000;
        long minutes = (timeInMillis % 3600000) / 60000;
        long seconds = (timeInMillis % 60000) / 1000;
        long millis = timeInMillis % 1000;
        
        return String.format("%02d:%02d:%02d.%03d", hours, minutes, seconds, millis);
    }
    
    /**
     * Helper methods for creating cyber-styled components
     */
    private JPanel createCyberPanel() {
        JPanel panel = new JPanel();
        panel.setBackground(new Color(10, 10, 25, 180));
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0, 255, 255, 150), 2),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        return panel;
    }
    
    private JLabel createCyberLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Orbitron", Font.BOLD, 14));
        label.setForeground(new Color(0, 255, 255));
        return label;
    }
    
    private JTextField createCyberTextField(String defaultText) {
        JTextField textField = new JTextField(defaultText);
        textField.setFont(new Font("Consolas", Font.BOLD, 14));
        textField.setBackground(new Color(5, 5, 20));
        textField.setForeground(new Color(0, 255, 255));
        textField.setCaretColor(new Color(0, 255, 255));
        textField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0, 255, 255), 2),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        return textField;
    }
    
    private JCheckBox createCyberCheckBox(boolean selected) {
        JCheckBox checkBox = new JCheckBox();
        checkBox.setSelected(selected);
        checkBox.setBackground(new Color(10, 10, 25, 0));
        checkBox.setForeground(new Color(0, 255, 255));
        checkBox.setFocusPainted(false);
        checkBox.setBorder(BorderFactory.createLineBorder(new Color(0, 255, 255), 1));
        return checkBox;
    }
    
    private JComboBox<String> createCyberComboBox(String[] options) {
        JComboBox<String> comboBox = new JComboBox<>(options);
        comboBox.setFont(new Font("Consolas", Font.BOLD, 12));
        comboBox.setBackground(new Color(5, 5, 20));
        comboBox.setForeground(new Color(0, 255, 255));
        comboBox.setBorder(BorderFactory.createLineBorder(new Color(0, 255, 255), 2));
        comboBox.setFocusable(false);
        
        // Style the dropdown arrow and list
        comboBox.setUI(new javax.swing.plaf.basic.BasicComboBoxUI() {
            @Override
            protected JButton createArrowButton() {
                JButton button = new JButton("▼");
                button.setBackground(new Color(0, 255, 255));
                button.setForeground(new Color(5, 5, 20));
                button.setBorder(BorderFactory.createEmptyBorder());
                button.setFocusPainted(false);
                return button;
            }
        });
        
        return comboBox;
    }
    
    private JButton createCyberButton(String text, Color baseColor) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Create gradient background
                GradientPaint gradient = new GradientPaint(
                    0, 0, baseColor.brighter(),
                    0, getHeight(), baseColor.darker()
                );
                g2d.setPaint(gradient);
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);
                
                // Add glow effect
                g2d.setColor(new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), 100));
                g2d.setStroke(new BasicStroke(3));
                g2d.drawRoundRect(1, 1, getWidth()-2, getHeight()-2, 15, 15);
                
                super.paintComponent(g);
            }
        };
        
        button.setFont(new Font("Orbitron", Font.BOLD, 12));
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setBackground(baseColor);
        button.setPreferredSize(new Dimension(140, 45));
        
        // Add hover effects
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(baseColor.brighter());
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(baseColor);
            }
        });
        
        return button;
    }
    
    private void showCyberErrorDialog(JDialog parent, String title, String message) {
        JDialog errorDialog = new JDialog(parent, title, true);
        errorDialog.setSize(400, 200);
        errorDialog.setLocationRelativeTo(parent);
        errorDialog.setUndecorated(true);
        
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(new Color(20, 5, 5));
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(255, 50, 50), 3),
            BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));
        
        JLabel titleLabel = new JLabel(title, SwingConstants.CENTER);
        titleLabel.setFont(new Font("Orbitron", Font.BOLD, 16));
        titleLabel.setForeground(new Color(255, 100, 100));
        
        JLabel messageLabel = new JLabel("<html><div style='text-align:center;'>" + message + "</div></html>", SwingConstants.CENTER);
        messageLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        messageLabel.setForeground(Color.WHITE);
        
        JButton okButton = createCyberButton("OK", new Color(255, 50, 50));
        okButton.addActionListener(e -> errorDialog.dispose());
        
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.setBackground(new Color(20, 5, 5));
        buttonPanel.add(okButton);
        
        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(messageLabel, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        errorDialog.add(panel);
        errorDialog.setVisible(true);
    }
    
    /**
     * Update alarm status display
     */
    private void updateAlarmStatus() {
        SwingUtilities.invokeLater(() -> {
            if (alarms.isEmpty()) {
                alarmStatusLabel.setText("No alarms set");
                alarmStatusLabel.setForeground(Color.ORANGE);
            } else {
                long activeAlarms = alarms.stream().filter(alarm -> alarm.isEnabled()).count();
                if (activeAlarms == 0) {
                    alarmStatusLabel.setText(alarms.size() + " alarm(s) - All disabled");
                    alarmStatusLabel.setForeground(Color.GRAY);
                } else {
                    AlarmTime nextAlarm = getNextAlarm();
                    if (nextAlarm != null) {
                        alarmStatusLabel.setText("Next: " + nextAlarm.getLabel() + " at " + nextAlarm.getTimeString());
                        alarmStatusLabel.setForeground(Color.GREEN);
                    } else {
                        alarmStatusLabel.setText(activeAlarms + " alarm(s) active");
                        alarmStatusLabel.setForeground(Color.GREEN);
                    }
                }
            }
        });
    }
    
    /**
     * Get the next alarm that will trigger
     */
    private AlarmTime getNextAlarm() {
        LocalTime now = LocalTime.now();
        return alarms.stream()
            .filter(AlarmTime::isEnabled)
            .min((a1, a2) -> {
                LocalTime t1 = a1.getTime();
                LocalTime t2 = a2.getTime();
                
                // Calculate minutes until each alarm
                int mins1 = t1.isAfter(now) ? 
                    (int)java.time.Duration.between(now, t1).toMinutes() :
                    (int)java.time.Duration.between(now, t1.plusHours(24)).toMinutes();
                    
                int mins2 = t2.isAfter(now) ? 
                    (int)java.time.Duration.between(now, t2).toMinutes() :
                    (int)java.time.Duration.between(now, t2.plusHours(24)).toMinutes();
                
                return Integer.compare(mins1, mins2);
            })
            .orElse(null);
    }
    
    /**
     * Check if any alarms should trigger
     */
    private void checkAlarms(ZonedDateTime now) {
        LocalTime currentTime = now.toLocalTime();
        LocalTime currentMinute = LocalTime.of(currentTime.getHour(), currentTime.getMinute());
        
        for (AlarmTime alarm : alarms) {
            if (alarm.isEnabled() && alarm.getTime().equals(currentMinute)) {
                triggerAlarm(alarm);
                if (!alarm.isRepeating()) {
                    alarm.setEnabled(false);
                    updateAlarmStatus();
                }
            }
        }
    }
    
    /**
     * Trigger an alarm
     */
    private void triggerAlarm(AlarmTime alarm) {
        isAlarmRinging = true;
        
        // Visual notification
        SwingUtilities.invokeLater(() -> {
            alarmStatusLabel.setText("ALARM: " + alarm.getLabel());
            alarmStatusLabel.setForeground(Color.RED);
        });
        
        // Show alarm dialog
        SwingUtilities.invokeLater(() -> {
            JDialog alarmNotification = new JDialog(this, "Alarm", true);
            alarmNotification.setSize(300, 150);
            alarmNotification.setLocationRelativeTo(this);
            alarmNotification.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
            
            JPanel panel = new JPanel(new BorderLayout(10, 10));
            panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
            panel.setBackground(Color.BLACK);
            
            JLabel messageLabel = new JLabel("ALARM: " + alarm.getLabel(), SwingConstants.CENTER);
            messageLabel.setFont(new Font("Arial", Font.BOLD, 16));
            messageLabel.setForeground(Color.RED);
            messageLabel.setOpaque(true);
            messageLabel.setBackground(Color.BLACK);
            
            JLabel timeLabel = new JLabel("Time: " + alarm.getTimeString(), SwingConstants.CENTER);
            timeLabel.setFont(new Font("Arial", Font.PLAIN, 14));
            timeLabel.setForeground(Color.WHITE);
            timeLabel.setOpaque(true);
            timeLabel.setBackground(Color.BLACK);
            
            JButton dismissButton = new JButton("Dismiss");
            dismissButton.setBackground(new Color(213, 0, 0));
            dismissButton.setForeground(Color.WHITE);
            dismissButton.setFocusPainted(false);
            dismissButton.addActionListener(e -> {
                isAlarmRinging = false;
                updateAlarmStatus();
                alarmNotification.dispose();
            });
            
            panel.add(messageLabel, BorderLayout.NORTH);
            panel.add(timeLabel, BorderLayout.CENTER);
            panel.add(dismissButton, BorderLayout.SOUTH);
            
            alarmNotification.add(panel);
            alarmNotification.setVisible(true);
        });
        
        // Play alarm sound in background
        executorService.submit(() -> playAlarmSound(alarm.getSoundNumber()));
    }
    
    /**
     * Play alarm sound using SoundManager
     */
    private void playAlarmSound() {
        try {
            // Use SoundManager to play alarm sound
            SoundManager.playAlarmSoundDefault();
        } catch (Exception e) {
            System.err.println("Error playing alarm sound: " + e.getMessage());
            // Fallback to system beep if sound file fails
            try {
                for (int i = 0; i < 3 && isAlarmRinging; i++) {
                    java.awt.Toolkit.getDefaultToolkit().beep();
                    Thread.sleep(500);
                }
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }
    }
    
    /**
     * Play specific alarm sound using SoundManager
     * @param alarmNumber The alarm sound number (1-8)
     */
    private void playAlarmSound(int alarmNumber) {
        try {
            SoundManager.playAlarmSound(alarmNumber);
        } catch (Exception e) {
            System.err.println("Error playing alarm sound " + alarmNumber + ": " + e.getMessage());
            // Fallback to default alarm sound
            playAlarmSound();
        }
    }

    /**
     * Inner class to represent an alarm time
     */
    private static class AlarmTime {
        private LocalTime time;
        private String label;
        private boolean enabled;
        private boolean repeating;
        private int soundNumber; // Sound number (1-8) for alarm sound selection
        
        public AlarmTime(LocalTime time, String label, boolean enabled, boolean repeating) {
            this.time = time;
            this.label = label;
            this.enabled = enabled;
            this.repeating = repeating;
            this.soundNumber = 1; // Default to alarm1.wav
        }
        
        public AlarmTime(LocalTime time, String label, boolean enabled, boolean repeating, int soundNumber) {
            this.time = time;
            this.label = label;
            this.enabled = enabled;
            this.repeating = repeating;
            this.soundNumber = (soundNumber >= 1 && soundNumber <= 8) ? soundNumber : 1; // Validate sound number
        }
        
        public LocalTime getTime() { return time; }
        public String getLabel() { return label; }
        public boolean isEnabled() { return enabled; }
        public boolean isRepeating() { return repeating; }
        public int getSoundNumber() { return soundNumber; }
        
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        
        public String getTimeString() {
            return String.format("%02d:%02d", time.getHour(), time.getMinute());
        }
        
        @Override
        public String toString() {
            return String.format("%s - %s %s%s", 
                getTimeString(), 
                label, 
                enabled ? "[ON]" : "[OFF]",
                repeating ? " (Repeat)" : "");
        }
    }
    
    /**
     * Main clock update task - runs every second
     */
    private class ClockTask implements Runnable {
        @Override
        public void run() {
            while (isRunning && !Thread.currentThread().isInterrupted()) {
                if (!isPaused) {
                    try {
                        ZonedDateTime now = ZonedDateTime.now(); // Uses system default time zone
                        String timeStr = AdvancedDigitalClock.this.formatTime(now);
                        String dateStr = AdvancedDigitalClock.this.formatDate(now);

                        SwingUtilities.invokeLater(() -> {
                            if (digitalClockLabel != null) digitalClockLabel.setText(timeStr);
                            if (dateLabel != null) dateLabel.setText(dateStr);
                            if (analogClockPanel != null) analogClockPanel.repaint();
                        });
                        
                        // Check for alarms
                        checkAlarms(now);
                        Thread.sleep(1000); 
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt(); // Preserve interrupt status
                        System.out.println("ClockTask interrupted during sleep or pause wait");
                        break; 
                    } catch (Exception e) {
                        System.err.println("Error in ClockTask: " + e.getMessage());
                        e.printStackTrace(); 
                    }
                } else {
                    // If paused, sleep for a short duration to make the loop non-blocking
                    try {
                        Thread.sleep(200); // Check pause state periodically
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        System.out.println("ClockTask interrupted during pause");
                        break;
                    }
                }
            }
            System.out.println("ClockTask terminated");
        }
    }

    /**
     * High-frequency millisecond update task
     */
    private class MillisecondTask implements Runnable {
        @Override
        public void run() {
            while (isRunning && !Thread.currentThread().isInterrupted()) {
                if (!isPaused) {
                    try {
                        ZonedDateTime now = ZonedDateTime.now(); // Uses system default time zone
                        String millisecondStr = AdvancedDigitalClock.this.formatMillisecond(now);
                        SwingUtilities.invokeLater(() -> {
                            if (millisecondLabel != null) millisecondLabel.setText(millisecondStr);
                        });
                        Thread.sleep(50); 
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        System.out.println("MillisecondTask interrupted during sleep or pause wait");
                        break;
                    } catch (Exception e) {
                        System.err.println("Error in MillisecondTask: " + e.getMessage());
                        e.printStackTrace();
                    }
                } else {
                    try {
                        Thread.sleep(200); 
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        System.out.println("MillisecondTask interrupted during pause");
                        break;
                    }
                }
            }
            System.out.println("MillisecondTask terminated");
        }
    }
    
    /**
     * Custom JPanel for analog clock display - Futuristic Gaming Design
     * Demonstrates graphics programming with threading
     * Enhanced with graphite gray and neon cyan gaming aesthetic
     */
    private class ClockPanel extends JPanel {
        private static final int CLOCK_SIZE = 400;
        
        // Futuristic Gaming color scheme - Graphite Gray & Neon Cyan
        private static final Color GRAPHITE_BACKGROUND = new Color(47, 47, 47);      // Graphite Gray #2F2F2F
        private static final Color GRAPHITE_DARK = new Color(35, 35, 35);            // Darker Graphite
        private static final Color GRAPHITE_LIGHT = new Color(65, 65, 65);           // Lighter Graphite
        private static final Color NEON_CYAN = new Color(0, 255, 255);               // Neon Cyan #00FFFF
        private static final Color NEON_CYAN_GLOW = new Color(0, 255, 255, 120);     // Cyan with transparency
        private static final Color ELECTRIC_BLUE = new Color(0, 119, 255);           // Electric Blue #0077FF
        private static final Color ELECTRIC_BLUE_GLOW = new Color(0, 119, 255, 150); // Blue with transparency
        private static final Color CYBER_ACCENT = new Color(0, 200, 255);            // Cyber Blue
        private static final Color DARK_GLOW = new Color(0, 0, 0, 180);              // Dark shadow
        private static final Color BRIGHT_CYAN = new Color(100, 255, 255);           // Bright Cyan
        private static final Color DEEP_GRAPHITE = new Color(20, 20, 20);            // Deep Graphite
        private static final Color SHADOW_COLOR = new Color(0, 0, 0, 100);
        
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            
            // Enable high-quality rendering for futuristic finish
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            
            int width = getWidth();
            int height = getHeight();
            int centerX = width / 2;
            int centerY = height / 2;
            int radius = Math.min(CLOCK_SIZE / 2, Math.min(centerX, centerY) - 15);
            
            // Draw futuristic gaming-style watch
            drawCyberBezel(g2d, centerX, centerY, radius);
            drawCyberDialFace(g2d, centerX, centerY, radius);
            drawCyberHourMarkers(g2d, centerX, centerY, radius);
            drawCyberNumbers(g2d, centerX, centerY, radius);
            drawCyberLogo(g2d, centerX, centerY, radius);
            
            // Draw hands only if clock is running
            if (isRunning) {
                drawCyberHands(g2d, centerX, centerY, radius);
            }
            
            // Draw center core
            drawCyberCenter(g2d, centerX, centerY);
        }
        
        
        private void drawCyberBezel(Graphics2D g2d, int centerX, int centerY, int radius) {
            // Draw outer cyber glow shadow with multiple layers for depth
            for (int i = 15; i >= 0; i--) {
                int alpha = (int) (30 - (i * 2));
                g2d.setColor(new Color(0, 255, 255, alpha));
                g2d.fillOval(centerX - radius - 10 - i, centerY - radius - 10 - i, 
                           (radius + 10 + i) * 2, (radius + 10 + i) * 2);
            }
            
            // Draw main bezel with premium metallic gradient
            RadialGradientPaint bezelGradient = new RadialGradientPaint(
                centerX, centerY, radius + 5,
                new float[]{0.0f, 0.3f, 0.6f, 0.8f, 1.0f},
                new Color[]{
                    GRAPHITE_LIGHT.brighter(),
                    GRAPHITE_BACKGROUND,
                    GRAPHITE_DARK,
                    DEEP_GRAPHITE,
                    new Color(10, 10, 10)
                }
            );
            
            g2d.setPaint(bezelGradient);
            g2d.fillOval(centerX - radius - 5, centerY - radius - 5, (radius + 5) * 2, (radius + 5) * 2);
            
            // Add glossy highlight on top edge
            GradientPaint glossyHighlight = new GradientPaint(
                centerX, centerY - radius - 5, new Color(255, 255, 255, 80),
                centerX, centerY - radius + 15, new Color(255, 255, 255, 0)
            );
            g2d.setPaint(glossyHighlight);
            g2d.fillArc(centerX - radius - 5, centerY - radius - 5, (radius + 5) * 2, (radius + 5) * 2, 
                       45, 90);
            
            // Draw neon cyan bezel markings with enhanced glow
            g2d.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (int i = 0; i < 60; i++) {
                double angle = Math.toRadians(i * 6);
                
                // Create glow effect for each marking
                for (int glow = 3; glow >= 0; glow--) {
                    if (i % 5 == 0) {
                        g2d.setColor(new Color(0, 255, 255, 120 - (glow * 30)));
                        g2d.setStroke(new BasicStroke(2 + glow, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    } else {
                        g2d.setColor(new Color(0, 255, 255, 80 - (glow * 20)));
                        g2d.setStroke(new BasicStroke(1 + glow, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    }
                    
                    int x1 = centerX + (int) ((radius + 3) * Math.cos(angle));
                    int y1 = centerY + (int) ((radius + 3) * Math.sin(angle));
                    int x2 = centerX + (int) ((radius - 3) * Math.cos(angle));
                    int y2 = centerY + (int) ((radius - 3) * Math.sin(angle));
                    g2d.drawLine(x1, y1, x2, y2);
                }
            }
        }
        
        private void drawCyberDialFace(Graphics2D g2d, int centerX, int centerY, int radius) {
            // Create premium layered glass effect with deeper gradients
            RadialGradientPaint outerGradient = new RadialGradientPaint(
                centerX, centerY - 30, radius,
                new float[]{0.0f, 0.4f, 0.8f, 1.0f},
                new Color[]{
                    new Color(47, 47, 47, 255),    // Solid graphite center
                    new Color(35, 35, 35, 240),    // Darker mid
                    new Color(25, 25, 25, 220),    // Deep graphite
                    new Color(15, 15, 15, 200)     // Dark edge with transparency
                }
            );
            
            g2d.setPaint(outerGradient);
            g2d.fillOval(centerX - radius + 10, centerY - radius + 10, (radius - 10) * 2, (radius - 10) * 2);
            
            // Add glossy glass highlight overlay
            RadialGradientPaint glassHighlight = new RadialGradientPaint(
                centerX - 30, centerY - 40, radius / 2,
                new float[]{0.0f, 0.6f, 1.0f},
                new Color[]{
                    new Color(255, 255, 255, 60),  // Bright white highlight
                    new Color(200, 230, 255, 25),  // Soft blue tint
                    new Color(255, 255, 255, 0)    // Fade to transparent
                }
            );
            
            g2d.setPaint(glassHighlight);
            g2d.fillOval(centerX - radius + 20, centerY - radius + 20, (radius - 20) * 2, (radius - 20) * 2);
            
            // Enhanced cyber circuit pattern with depth
            g2d.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (int i = 0; i < 360; i += 12) {
                double angle = Math.toRadians(i);
                
                // Circuit shadow for depth
                g2d.setColor(new Color(0, 0, 0, 80));
                int sx1 = centerX + (int) (35 * Math.cos(angle)) + 1;
                int sy1 = centerY + (int) (35 * Math.sin(angle)) + 1;
                int sx2 = centerX + (int) ((radius - 25) * Math.cos(angle)) + 1;
                int sy2 = centerY + (int) ((radius - 25) * Math.sin(angle)) + 1;
                g2d.drawLine(sx1, sy1, sx2, sy2);
                
                // Main circuit line with varying opacity
                Color circuitColor = new Color(0, 255, 255, 50 + (i % 3) * 20);
                g2d.setColor(circuitColor);
                
                int x1 = centerX + (int) (35 * Math.cos(angle));
                int y1 = centerY + (int) (35 * Math.sin(angle));
                int x2 = centerX + (int) ((radius - 25) * Math.cos(angle));
                int y2 = centerY + (int) ((radius - 25) * Math.sin(angle));
                g2d.drawLine(x1, y1, x2, y2);
            }
            
            // Enhanced glowing cyber rings with metallic effect
            g2d.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            
            // Outer ring with glow
            g2d.setColor(new Color(0, 255, 255, 30));
            g2d.drawOval(centerX - (radius / 2), centerY - (radius / 2), radius, radius);
            g2d.setColor(NEON_CYAN_GLOW);
            g2d.setStroke(new BasicStroke(2));
            g2d.drawOval(centerX - (radius / 2), centerY - (radius / 2), radius, radius);
            
            // Inner ring with stronger glow
            g2d.setStroke(new BasicStroke(2.5f));
            g2d.setColor(new Color(0, 255, 255, 60));
            g2d.drawOval(centerX - (radius / 3), centerY - (radius / 3), (radius / 3) * 2, (radius / 3) * 2);
            g2d.setColor(BRIGHT_CYAN);
            g2d.setStroke(new BasicStroke(1.5f));
            g2d.drawOval(centerX - (radius / 3), centerY - (radius / 3), (radius / 3) * 2, (radius / 3) * 2);
        }
        
        private void drawCyberHourMarkers(Graphics2D g2d, int centerX, int centerY, int radius) {
            // Draw premium futuristic neon hour markers with metallic texture
            for (int i = 1; i <= 12; i++) {
                double angle = Math.toRadians(i * 30 - 90);
                
                if (i % 3 == 0) {
                    // Draw enhanced metallic rectangular markers for 12, 3, 6, 9
                    int markerLength = 20;
                    int markerWidth = 6;
                    
                    int x1 = centerX + (int) ((radius - 30) * Math.cos(angle));
                    int y1 = centerY + (int) ((radius - 30) * Math.sin(angle));
                    int x2 = centerX + (int) ((radius - 50) * Math.cos(angle));
                    int y2 = centerY + (int) ((radius - 50) * Math.sin(angle));
                    
                    // Drop shadow for depth
                    g2d.setColor(new Color(0, 0, 0, 120));
                    g2d.setStroke(new BasicStroke(markerWidth + 2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2d.drawLine(x1 + 2, y1 + 2, x2 + 2, y2 + 2);
                    
                    // Metallic base with gradient effect
                    LinearGradientPaint metallicGradient = new LinearGradientPaint(
                        x1, y1, x2, y2,
                        new float[]{0.0f, 0.3f, 0.7f, 1.0f},
                        new Color[]{
                            CYBER_ACCENT,
                            NEON_CYAN,
                            BRIGHT_CYAN,
                            ELECTRIC_BLUE
                        }
                    );
                    g2d.setPaint(metallicGradient);
                    g2d.setStroke(new BasicStroke(markerWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2d.drawLine(x1, y1, x2, y2);
                    
                    // Bright neon glow effect (multiple layers)
                    for (int glow = 8; glow >= 0; glow -= 2) {
                        g2d.setColor(new Color(0, 255, 255, 80 - (glow * 10)));
                        g2d.setStroke(new BasicStroke(markerWidth + glow, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                        g2d.drawLine(x1, y1, x2, y2);
                    }
                    
                    // Final bright highlight
                    g2d.setColor(BRIGHT_CYAN);
                    g2d.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2d.drawLine(x1, y1, x2, y2);
                    
                } else {
                    // Enhanced smaller cyan markers for other hours
                    int x1 = centerX + (int) ((radius - 25) * Math.cos(angle));
                    int y1 = centerY + (int) ((radius - 25) * Math.sin(angle));
                    int x2 = centerX + (int) ((radius - 40) * Math.cos(angle));
                    int y2 = centerY + (int) ((radius - 40) * Math.sin(angle));
                    
                    // Shadow for depth
                    g2d.setColor(new Color(0, 0, 0, 100));
                    g2d.setStroke(new BasicStroke(5, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2d.drawLine(x1 + 1, y1 + 1, x2 + 1, y2 + 1);
                    
                    // Main marker with metallic appearance
                    g2d.setColor(NEON_CYAN);
                    g2d.setStroke(new BasicStroke(4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2d.drawLine(x1, y1, x2, y2);
                    
                    // Glow effect
                    g2d.setColor(NEON_CYAN_GLOW);
                    g2d.setStroke(new BasicStroke(7, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2d.drawLine(x1, y1, x2, y2);
                    
                    // Bright highlight core
                    g2d.setColor(BRIGHT_CYAN);
                    g2d.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2d.drawLine(x1, y1, x2, y2);
                }
            }
            
            // Enhanced cyber minute markers with better visibility
            g2d.setStroke(new BasicStroke(1.5f));
            for (int i = 0; i < 60; i++) {
                if (i % 5 != 0) {
                    double angle = Math.toRadians(i * 6 - 90);
                    int x1 = centerX + (int) ((radius - 20) * Math.cos(angle));
                    int y1 = centerY + (int) ((radius - 20) * Math.sin(angle));
                    int x2 = centerX + (int) ((radius - 28) * Math.cos(angle));
                    int y2 = centerY + (int) ((radius - 28) * Math.sin(angle));
                    
                    // Subtle glow for minute markers
                    g2d.setColor(new Color(0, 200, 255, 60));
                    g2d.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2d.drawLine(x1, y1, x2, y2);
                    
                    // Main minute marker
                    g2d.setColor(CYBER_ACCENT);
                    g2d.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2d.drawLine(x1, y1, x2, y2);
                }
            }
        }
        
        private void drawCyberNumbers(Graphics2D g2d, int centerX, int centerY, int radius) {
            // Draw premium futuristic neon cyan numbers with enhanced effects
            g2d.setFont(new Font("Impact", Font.BOLD, 22));
            FontMetrics fm = g2d.getFontMetrics();
            
            String[] numbers = {"XII", "III", "VI", "IX"};
            int[] positions = {12, 3, 6, 9};
            
            for (int i = 0; i < 4; i++) {
                double angle = Math.toRadians(positions[i] * 30 - 90);
                int x = centerX + (int) ((radius - 70) * Math.cos(angle));
                int y = centerY + (int) ((radius - 70) * Math.sin(angle));
                
                String number = numbers[i];
                int stringWidth = fm.stringWidth(number);
                int stringHeight = fm.getAscent();
                
                int textX = x - stringWidth / 2;
                int textY = y + stringHeight / 2;
                
                // Multiple layer drop shadow for premium depth effect
                for (int shadow = 5; shadow >= 1; shadow--) {
                    int shadowAlpha = 20 + (shadow * 15);
                    g2d.setColor(new Color(0, 0, 0, shadowAlpha));
                    g2d.drawString(number, textX + shadow, textY + shadow);
                }
                
                // Outer glow ring (largest)
                for (int ring = 6; ring >= 0; ring--) {
                    int glowAlpha = 15 + (ring * 8);
                    g2d.setColor(new Color(0, 255, 255, glowAlpha));
                    for (int dx = -ring; dx <= ring; dx++) {
                        for (int dy = -ring; dy <= ring; dy++) {
                            if (dx * dx + dy * dy <= ring * ring) {
                                g2d.drawString(number, textX + dx, textY + dy);
                            }
                        }
                    }
                }
                
                // Metallic base layer with gradient effect
                LinearGradientPaint metallicText = new LinearGradientPaint(
                    textX, textY - stringHeight/2, textX, textY + stringHeight/2,
                    new float[]{0.0f, 0.3f, 0.7f, 1.0f},
                    new Color[]{
                        CYBER_ACCENT,
                        NEON_CYAN,
                        BRIGHT_CYAN,
                        ELECTRIC_BLUE
                    }
                );
                g2d.setPaint(metallicText);
                g2d.drawString(number, textX, textY);
                
                // Inner bright glow
                g2d.setColor(new Color(100, 255, 255, 200));
                g2d.drawString(number, textX, textY);
                
                // Final bright highlight core
                g2d.setColor(new Color(200, 255, 255, 255));
                g2d.drawString(number, textX, textY);
                
                // Add premium glass reflection effect on top edge
                g2d.setColor(new Color(255, 255, 255, 120));
                g2d.setFont(new Font("Impact", Font.BOLD, 20));
                FontMetrics fmSmall = g2d.getFontMetrics();
                int smallWidth = fmSmall.stringWidth(number);
                g2d.drawString(number, x - smallWidth / 2, y + fmSmall.getAscent() / 2 - 2);
                
                // Reset font
                g2d.setFont(new Font("Impact", Font.BOLD, 22));
            }
        }
        
        private void drawCyberLogo(Graphics2D g2d, int centerX, int centerY, int radius) {
            // Draw futuristic gaming logo at 12 o'clock position
            g2d.setColor(ELECTRIC_BLUE);
            g2d.setFont(new Font("Impact", Font.BOLD, 14));
            FontMetrics fm = g2d.getFontMetrics();
            
            // Cyber gaming symbol (diamond with circuit)
            String cyberSymbol = "◊";
            int symbolWidth = fm.stringWidth(cyberSymbol);
            g2d.drawString(cyberSymbol, centerX - symbolWidth / 2, centerY - radius / 3);
            
            // Brand text with neon effect
            g2d.setFont(new Font("Impact", Font.BOLD, 11));
            fm = g2d.getFontMetrics();
            
            String brand = "CYBER";
            int brandWidth = fm.stringWidth(brand);
            g2d.setColor(NEON_CYAN);
            g2d.drawString(brand, centerX - brandWidth / 2, centerY - radius / 3 + 18);
            
            String model = "GAMING CHRONOGRAPH";
            g2d.setFont(new Font("Impact", Font.PLAIN, 9));
            fm = g2d.getFontMetrics();
            int modelWidth = fm.stringWidth(model);
            g2d.setColor(ELECTRIC_BLUE);
            g2d.drawString(model, centerX - modelWidth / 2, centerY + radius / 3 - 10);
            
            String tech = "NEON FUSION";
            int techWidth = fm.stringWidth(tech);
            g2d.setColor(CYBER_ACCENT);
            g2d.drawString(tech, centerX - techWidth / 2, centerY + radius / 3 + 5);
        }
        
        private void drawCyberHands(Graphics2D g2d, int centerX, int centerY, int radius) {
            // Get current time in the system's default time zone
            ZonedDateTime now = ZonedDateTime.now(); // MODIFIED HERE
            int hours = now.getHour() % 12;
            int minutes = now.getMinute();
            int seconds = now.getSecond();
            
            // Calculate angles
            double hourAngle = Math.toRadians(((hours % 12) * 30 + minutes * 0.5) - 90);
            double minuteAngle = Math.toRadians((minutes * 6) - 90);
            double secondAngle = Math.toRadians((seconds * 6) - 90);
            
            // Draw hour hand (Electric blue)
            drawCyberHourHand(g2d, centerX, centerY, hourAngle, radius - 80);
            
            // Draw minute hand (Electric blue)
            drawCyberMinuteHand(g2d, centerX, centerY, minuteAngle, radius - 45);
            
            // Draw second hand (Neon cyan)
            drawCyberSecondHand(g2d, centerX, centerY, secondAngle, radius - 30);
        }
        
        private void drawCyberHourHand(Graphics2D g2d, int centerX, int centerY, double angle, int length) {
            // Premium electric blue hour hand with enhanced depth and metallic finish
            int handX = centerX + (int) (length * Math.cos(angle));
            int handY = centerY + (int) (length * Math.sin(angle));
            
            // Multi-layer drop shadow for premium depth
            for (int shadow = 4; shadow >= 1; shadow--) {
                g2d.setColor(new Color(0, 0, 0, 60 - (shadow * 10)));
                g2d.setStroke(new BasicStroke(10 + shadow, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2d.drawLine(centerX + shadow, centerY + shadow, handX + shadow, handY + shadow);
            }
            
            // Outer glow effect (largest glow layer)
            g2d.setColor(new Color(0, 119, 255, 40));
            g2d.setStroke(new BasicStroke(16, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2d.drawLine(centerX, centerY, handX, handY);
            
            // Mid glow layer
            g2d.setColor(new Color(0, 119, 255, 80));
            g2d.setStroke(new BasicStroke(12, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2d.drawLine(centerX, centerY, handX, handY);
            
            // Metallic base with gradient
            LinearGradientPaint handGradient = new LinearGradientPaint(
                centerX, centerY, handX, handY,
                new float[]{0.0f, 0.4f, 0.8f, 1.0f},
                new Color[]{
                    ELECTRIC_BLUE.brighter(),
                    ELECTRIC_BLUE,
                    ELECTRIC_BLUE.darker(),
                    CYBER_ACCENT
                }
            );
            g2d.setPaint(handGradient);
            g2d.setStroke(new BasicStroke(8, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2d.drawLine(centerX, centerY, handX, handY);
            
            // Inner bright highlight
            g2d.setColor(new Color(100, 170, 255));
            g2d.setStroke(new BasicStroke(4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2d.drawLine(centerX, centerY, handX, handY);
            
            // Enhanced cyber diamond tip with premium effects
            int[] xPoints = {
                handX,
                handX - (int) (12 * Math.cos(angle - Math.PI/4)),
                handX - (int) (8 * Math.cos(angle)),
                handX - (int) (12 * Math.cos(angle + Math.PI/4))
            };
            
            int[] yPoints = {
                handY,
                handY - (int) (12 * Math.sin(angle - Math.PI/4)),
                handY - (int) (8 * Math.sin(angle)),
                handY - (int) (12 * Math.sin(angle + Math.PI/4))
            };
            
            // Bright core highlight
            g2d.setColor(new Color(200, 255, 255));
            g2d.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2d.drawLine(centerX, centerY, handX, handY);
            
            // Enhanced counterbalance with premium effects
            int counterX = centerX - (int) (35 * Math.cos(angle));
            int counterY = centerY - (int) (35 * Math.sin(angle));
            
            // Counterbalance shadow
            g2d.setColor(new Color(0, 0, 0, 80));
            g2d.setStroke(new BasicStroke(4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2d.drawLine(centerX + 1, centerY + 1, counterX + 1, counterY + 1);
            
            // Counterbalance glow
            g2d.setColor(new Color(0, 119, 255, 100));
            g2d.setStroke(new BasicStroke(6, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2d.drawLine(centerX, centerY, counterX, counterY);
            
            // Counterbalance main body
            g2d.setColor(ELECTRIC_BLUE);
            g2d.setStroke(new BasicStroke(4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2d.drawLine(centerX, centerY, counterX, counterY);
            
            // Premium cyber diamond tip with enhanced effects
            // Multiple glow layers for tip
            for (int glow = 8; glow >= 0; glow -= 2) {
                int glowAlpha = 120 - (glow * 15);
                g2d.setColor(new Color(0, 255, 255, glowAlpha));
                g2d.fillOval(handX - (5 + glow), handY - (5 + glow), (10 + glow * 2), (10 + glow * 2));
            }
            
            // Main tip with radial gradient
            RadialGradientPaint tipGradient = new RadialGradientPaint(
                handX, handY, 5,
                new float[]{0.0f, 0.4f, 0.8f, 1.0f},
                new Color[]{
                    new Color(255, 255, 255, 200),
                    BRIGHT_CYAN,
                    NEON_CYAN,
                    CYBER_ACCENT
                }
            );
            g2d.setPaint(tipGradient);
            g2d.fillOval(handX - 5, handY - 5, 10, 10);
            
            // Bright white core
            g2d.setColor(new Color(255, 255, 255, 220));
            g2d.fillOval(handX - 2, handY - 2, 4, 4);
            
            // Outer highlight ring
            g2d.setColor(BRIGHT_CYAN);
            g2d.setStroke(new BasicStroke(1.5f));
            g2d.drawOval(handX - 5, handY - 5, 10, 10);
        }
        
        private void drawCyberMinuteHand(Graphics2D g2d, int centerX, int centerY, double angle, int length) {
            // Premium electric blue minute hand with enhanced depth and metallic finish
            int handX = centerX + (int) (length * Math.cos(angle));
            int handY = centerY + (int) (length * Math.sin(angle));
            
            // Multi-layer drop shadow for premium depth
            for (int shadow = 3; shadow >= 1; shadow--) {
                g2d.setColor(new Color(0, 0, 0, 50 - (shadow * 10)));
                g2d.setStroke(new BasicStroke(8 + shadow, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2d.drawLine(centerX + shadow, centerY + shadow, handX + shadow, handY + shadow);
            }
            
            // Create premium metallic gradient for hand
            LinearGradientPaint gradient = new LinearGradientPaint(
                centerX, centerY, handX, handY,
                new float[]{0.0f, 0.5f, 1.0f},
                new Color[]{
                    ELECTRIC_BLUE,
                    new Color(100, 200, 255),
                    ELECTRIC_BLUE
                }
            );
            
            // Main hand body with premium metallic gradient
            g2d.setPaint(gradient);
            g2d.setStroke(new BasicStroke(6, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2d.drawLine(centerX, centerY, handX, handY);
            
            // Enhanced cyber diamond tip
            int[] xPoints = {
                handX,
                handX - (int) (10 * Math.cos(angle - Math.PI/4)),
                handX - (int) (6 * Math.cos(angle)),
                handX - (int) (10 * Math.cos(angle + Math.PI/4))
            };
            
            int[] yPoints = {
                handY,
                handY - (int) (10 * Math.sin(angle - Math.PI/4)),
                handY - (int) (6 * Math.sin(angle)),
                handY - (int) (10 * Math.sin(angle + Math.PI/4))
            };
            
            g2d.setColor(ELECTRIC_BLUE);
            g2d.fillPolygon(xPoints, yPoints, 4);
            
            // Edge highlight for premium look
            g2d.setColor(new Color(180, 220, 255));
            g2d.setStroke(new BasicStroke(1, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2d.drawLine(centerX, centerY, handX, handY);
            
            // Enhanced counterbalance with premium effects
            int counterX = centerX - (int) (25 * Math.cos(angle));
            int counterY = centerY - (int) (25 * Math.sin(angle));
            
            g2d.setColor(new Color(0, 80, 150, 150));
            g2d.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2d.drawLine(centerX, centerY, counterX, counterY);
            
            // Add glowing effect
            g2d.setColor(ELECTRIC_BLUE_GLOW);
            g2d.setStroke(new BasicStroke(9, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2d.drawLine(centerX, centerY, handX, handY);
        }
        
        private void drawCyberSecondHand(Graphics2D g2d, int centerX, int centerY, double angle, int length) {
            // Neon cyan second hand with enhanced effects
            int handX = centerX + (int) (length * Math.cos(angle));
            int handY = centerY + (int) (length * Math.sin(angle));
            
            // Multi-layer drop shadow for depth
            for (int shadow = 3; shadow >= 1; shadow--) {
                g2d.setColor(new Color(0, 0, 0, 40 - (shadow * 10)));
                g2d.setStroke(new BasicStroke(6 + shadow, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2d.drawLine(centerX + shadow, centerY + shadow, handX + shadow, handY + shadow);
            }
            
            // Main second hand line
            g2d.setColor(NEON_CYAN);
            g2d.setStroke(new BasicStroke(4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2d.drawLine(centerX, centerY, handX, handY);
            
            // Enhanced tip with glow
            for (int glow = 8; glow >= 0; glow -= 2) {
                int glowAlpha = 120 - (glow * 15);
                g2d.setColor(new Color(0, 255, 255, glowAlpha));
                g2d.fillOval(handX - (3 + glow), handY - (3 + glow), (6 + glow * 2), (6 + glow * 2));
            }
            
            // Main tip with radial gradient
            RadialGradientPaint tipGradient = new RadialGradientPaint(
                handX, handY, 3,
                new float[]{0.0f, 0.4f, 0.8f, 1.0f},
                new Color[]{
                    new Color(255, 255, 255, 220),
                    BRIGHT_CYAN,
                    NEON_CYAN,
                    CYBER_ACCENT
                }
            );
            g2d.setPaint(tipGradient);
            g2d.fillOval(handX - 3, handY - 3, 6, 6);
            
            // Bright white core
            g2d.setColor(new Color(255, 255, 255, 255));
            g2d.fillOval(handX - 1, handY - 1, 2, 2);
        }
        
        private void drawCyberCenter(Graphics2D g2d, int centerX, int centerY) {
            // Premium futuristic cyber center with enhanced energy core effects
            
            // Multiple layer drop shadow for depth
            for (int shadow = 3; shadow >= 1; shadow--) {
                g2d.setColor(new Color(0, 0, 0, 40 + (shadow * 20)));
                g2d.fillOval(centerX - 18 + shadow, centerY - 18 + shadow, 36, 36);
            }
            
            // Outer energy ring with enhanced gradient
            RadialGradientPaint outerRing = new RadialGradientPaint(
                centerX, centerY, 18,
                new float[]{0.0f, 0.3f, 0.7f, 1.0f},
                new Color[]{
                    new Color(0, 255, 255, 200),
                    NEON_CYAN,
                    ELECTRIC_BLUE,
                    GRAPHITE_DARK
                }
            );
            g2d.setPaint(outerRing);
            g2d.fillOval(centerX - 18, centerY - 18, 36, 36);
            
            // Mid energy core with metallic gradient
            RadialGradientPaint midCore = new RadialGradientPaint(
                centerX, centerY, 12,
                new float[]{0.0f, 0.4f, 0.8f, 1.0f},
                new Color[]{
                    BRIGHT_CYAN,
                    NEON_CYAN,
                    ELECTRIC_BLUE,
                    GRAPHITE_BACKGROUND
                }
            );
            g2d.setPaint(midCore);
            g2d.fillOval(centerX - 12, centerY - 12, 24, 24);
            
            // Inner graphite core with subtle gradient
            RadialGradientPaint innerCore = new RadialGradientPaint(
                centerX, centerY, 8,
                new float[]{0.0f, 0.6f, 1.0f},
                new Color[]{
                    GRAPHITE_LIGHT,
                    GRAPHITE_BACKGROUND,
                    GRAPHITE_DARK
                }
            );
            g2d.setPaint(innerCore);
            g2d.fillOval(centerX - 8, centerY - 8, 16, 16);
            
            // Central energy dot with pulsing effect
            RadialGradientPaint energyDot = new RadialGradientPaint(
                centerX, centerY, 4,
                new float[]{0.0f, 0.5f, 1.0f},
                new Color[]{
                    new Color(255, 255, 255, 220),
                    BRIGHT_CYAN,
                    NEON_CYAN
                }
            );
            g2d.setPaint(energyDot);
            g2d.fillOval(centerX - 4, centerY - 4, 8, 8);
            
            // Multiple highlight rings for premium effect
            g2d.setColor(ELECTRIC_BLUE);
            g2d.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2d.drawOval(centerX - 18, centerY - 18, 36, 36);
            
            g2d.setColor(BRIGHT_CYAN);
            g2d.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2d.drawOval(centerX - 12, centerY - 12, 24, 24);
            
            g2d.setColor(new Color(200, 255, 255));
            g2d.setStroke(new BasicStroke(1.5f));
            g2d.drawOval(centerX - 8, centerY - 8, 16, 16);
            
            // Add premium glass highlight on top
            GradientPaint glassHighlight = new GradientPaint(
                centerX, centerY - 18, new Color(255, 255, 255, 100),
                centerX, centerY - 8, new Color(255, 255, 255, 0)
            );
            g2d.setPaint(glassHighlight);
            g2d.fillArc(centerX - 18, centerY - 18, 36, 36, 45, 90);
        }
    }
    
    /**
     * Inner class for displaying individual city clocks in world clock panel
     */
    private class CityClockDisplay {
        private String cityName;
        private ZoneId zoneId;
        private JPanel panel;
        private JLabel cityLabel;
        private JLabel timeLabel;
        private JLabel dateLabel;
        private JLabel diffLabel;
        
        public CityClockDisplay(String cityName, ZoneId zoneId) {
            this.cityName = cityName;
            this.zoneId = zoneId;
            createPanel();
        }
        
        private void createPanel() {
            panel = new JPanel(new BorderLayout(4, 2));
            panel.setBackground(Color.BLACK);
            panel.setPreferredSize(new Dimension(155, 130));
            panel.setBorder(BorderFactory.createLineBorder(new Color(60, 60, 60, 120), 1));
            
            // City name at the top
            cityLabel = new JLabel(cityName, JLabel.CENTER);
            cityLabel.setForeground(Color.CYAN);
            cityLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
            
            // Time difference below city name - styled as a compact badge
            diffLabel = new JLabel("", JLabel.CENTER);
            diffLabel.setForeground(Color.WHITE);
            diffLabel.setFont(new Font("Segoe UI", Font.BOLD, 10));
            diffLabel.setOpaque(true);
            diffLabel.setBackground(new Color(70, 130, 180, 180)); // More transparent steel blue
            diffLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(100, 150, 200, 100), 1),
                BorderFactory.createEmptyBorder(1, 6, 1, 6)
            ));
            diffLabel.setPreferredSize(new Dimension(55, 18));
            diffLabel.setMaximumSize(new Dimension(55, 18));
            
            // Date in the middle
            dateLabel = new JLabel("", JLabel.CENTER);
            dateLabel.setForeground(Color.LIGHT_GRAY);
            dateLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            
            // Time at the bottom
            timeLabel = new JLabel("", JLabel.CENTER);
            timeLabel.setForeground(Color.WHITE);
            timeLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
            
            // Create top panel with city name and time difference badge
            JPanel topPanel = new JPanel(new BorderLayout());
            topPanel.setBackground(Color.BLACK);
            topPanel.add(cityLabel, BorderLayout.CENTER);
            
            // Create a wrapper panel to center the difference badge
            JPanel diffWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 1));
            diffWrapper.setBackground(Color.BLACK);
            diffWrapper.add(diffLabel);
            topPanel.add(diffWrapper, BorderLayout.SOUTH);
            
            panel.add(topPanel, BorderLayout.NORTH);
            panel.add(dateLabel, BorderLayout.CENTER);
            panel.add(timeLabel, BorderLayout.SOUTH);
            
            update(); // Initialize with current time
        }
        
        public void update() {
            ZonedDateTime localTime = ZonedDateTime.now();
            ZonedDateTime cityTime = ZonedDateTime.now(zoneId);
            
            // Format the time
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
            timeLabel.setText(cityTime.format(timeFormatter));
            
            // Format the date
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("EEEE, dd");
            dateLabel.setText(cityTime.format(dateFormatter));
            
            // Calculate time difference properly using zone offsets
            int localOffset = localTime.getOffset().getTotalSeconds() / 3600;
            int cityOffset = cityTime.getOffset().getTotalSeconds() / 3600;
            long hoursDiff = cityOffset - localOffset;
            
            String diffText;
            if (hoursDiff > 0) {
                diffText = "+" + hoursDiff + "h";
            } else if (hoursDiff < 0) {
                diffText = hoursDiff + "h";
            } else {
                diffText = "0h";
            }
            diffLabel.setText(diffText);
        }
        
        public JPanel getPanel() {
            return panel;
        }
    }
    
    // This is a helper method placeholder that has been merged with the other implementation
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                    if ("Windows".equals(info.getName())) {
                        UIManager.setLookAndFeel(info.getClassName());
                        break;
                    }
                }
            } catch (Exception e) {
                System.err.println("Could not set look and feel: " + e.getMessage());
            }
            
            new AdvancedDigitalClock().setVisible(true);
        });
    }
}
