import javax.swing.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Calendar;

/**
 * Analog Clock Application - Lab 2 Extended
 * 
 * This application demonstrates Java threading concepts through a visual analog clock:
 * - Custom graphics rendering with Java 2D
 * - Thread-based animation and smooth updates
 * - Thread synchronization with GUI updates
 * - Multiple threading approaches (Timer vs Thread)
 * - Thread lifecycle management
 */
public class AnalogClock extends JFrame {
    
    // GUI Components
    private ClockPanel clockPanel;
    private JButton startButton;
    private JButton stopButton;
    private JButton exitButton;
    private JLabel statusLabel;
    private JLabel digitalTimeLabel;
    private JButton smoothModeButton;
    private JButton normalModeButton;
    private JButton smallSizeButton;
    private JButton mediumSizeButton;
    private JButton largeSizeButton;
    private JButton extraLargeSizeButton;
    
    // Thread management
    private ClockThread clockThread;
    private volatile boolean isRunning = false;
    private volatile boolean smoothMode = false;
    
    // Display size settings
    private enum DisplaySize { SMALL, MEDIUM, LARGE, EXTRA_LARGE }
    private DisplaySize currentSize = DisplaySize.LARGE;
    
    // Time formatting
    private SimpleDateFormat digitalFormat = new SimpleDateFormat("HH:mm:ss");
    
    // Clock styling - dynamic sizing
    private int clockSize = 300;
    private static final Color BACKGROUND_COLOR = Color.BLACK;
    private static final Color CLOCK_FACE_COLOR = new Color(240, 240, 240);
    private static final Color HOUR_HAND_COLOR = Color.BLACK;
    private static final Color MINUTE_HAND_COLOR = Color.BLUE;
    private static final Color SECOND_HAND_COLOR = Color.RED;
    private static final Color NUMBERS_COLOR = Color.BLACK;
    
    public AnalogClock() {
        initializeGUI();
        setupEventHandlers();
    }
    
    /**
     * Initialize the GUI components and layout
     */
    private void initializeGUI() {
        setTitle("Analog Clock - Threading with Graphics Demo");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        
        // Create main panel
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        mainPanel.setBackground(BACKGROUND_COLOR);
        
        // Create clock panel
        clockPanel = new ClockPanel();
        updateClockSize(); // Set initial size
        clockPanel.setBackground(BACKGROUND_COLOR);
        
        // Create control panel
        JPanel controlPanel = createControlPanel();
        
        // Create status panel
        JPanel statusPanel = createStatusPanel();
        
        // Create mode panel
        JPanel modePanel = createModePanel();
        
        // Create size control panel
        JPanel sizePanel = createSizePanel();
        
        // Create info panel for digital time
        JPanel infoPanel = createInfoPanel();
        
        // Arrange components
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(BACKGROUND_COLOR);
        topPanel.add(statusPanel, BorderLayout.NORTH);
        topPanel.add(infoPanel, BorderLayout.SOUTH);
        
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBackground(BACKGROUND_COLOR);
        bottomPanel.add(controlPanel, BorderLayout.CENTER);
        
        JPanel modeAndSizePanel = new JPanel(new BorderLayout());
        modeAndSizePanel.setBackground(BACKGROUND_COLOR);
        modeAndSizePanel.add(modePanel, BorderLayout.NORTH);
        modeAndSizePanel.add(sizePanel, BorderLayout.SOUTH);
        
        bottomPanel.add(modeAndSizePanel, BorderLayout.SOUTH);
        
        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(clockPanel, BorderLayout.CENTER);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);
        
        add(mainPanel);
        pack();
        setLocationRelativeTo(null);
    }
    
    /**
     * Create the control buttons panel
     */
    private JPanel createControlPanel() {
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        controlPanel.setBackground(BACKGROUND_COLOR);
        
        startButton = new JButton("Start Clock");
        startButton.setPreferredSize(new Dimension(120, 30));
        startButton.setBackground(Color.GREEN);
        startButton.setForeground(Color.BLACK);
        startButton.setFont(new Font("Arial", Font.BOLD, 12));
        
        stopButton = new JButton("Stop Clock");
        stopButton.setPreferredSize(new Dimension(120, 30));
        stopButton.setBackground(Color.RED);
        stopButton.setForeground(Color.WHITE);
        stopButton.setFont(new Font("Arial", Font.BOLD, 12));
        stopButton.setEnabled(false);
        
        exitButton = new JButton("Exit");
        exitButton.setPreferredSize(new Dimension(120, 30));
        exitButton.setBackground(Color.GRAY);
        exitButton.setForeground(Color.WHITE);
        exitButton.setFont(new Font("Arial", Font.BOLD, 12));
        
        controlPanel.add(startButton);
        controlPanel.add(stopButton);
        controlPanel.add(exitButton);
        
        return controlPanel;
    }
    
    /**
     * Create the status panel
     */
    private JPanel createStatusPanel() {
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusPanel.setBackground(BACKGROUND_COLOR);
        
        statusLabel = new JLabel("Status: Clock Stopped");
        statusLabel.setForeground(Color.YELLOW);
        statusLabel.setFont(new Font("Arial", Font.BOLD, 12));
        
        statusPanel.add(statusLabel);
        
        return statusPanel;
    }
    
    /**
     * Create the info panel with digital time display
     */
    private JPanel createInfoPanel() {
        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        infoPanel.setBackground(BACKGROUND_COLOR);
        
        digitalTimeLabel = new JLabel("--:--:--");
        digitalTimeLabel.setForeground(Color.WHITE);
        digitalTimeLabel.setFont(new Font("Courier New", Font.BOLD, 16));
        
        infoPanel.add(new JLabel("Digital Time: ") {{
            setForeground(Color.CYAN);
            setFont(new Font("Arial", Font.PLAIN, 12));
        }});
        infoPanel.add(digitalTimeLabel);
        
        return infoPanel;
    }
    
    /**
     * Create the mode selection panel
     */
    private JPanel createModePanel() {
        JPanel modePanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        modePanel.setBackground(BACKGROUND_COLOR);
        modePanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Color.CYAN), 
            "Update Mode", 
            0, 0, new Font("Arial", Font.BOLD, 10), Color.CYAN));
        
        normalModeButton = new JButton("Normal (1s)");
        normalModeButton.setPreferredSize(new Dimension(100, 25));
        normalModeButton.setBackground(Color.BLUE);
        normalModeButton.setForeground(Color.WHITE);
        normalModeButton.setFont(new Font("Arial", Font.BOLD, 10));
        
        smoothModeButton = new JButton("Smooth (100ms)");
        smoothModeButton.setPreferredSize(new Dimension(100, 25));
        smoothModeButton.setBackground(Color.DARK_GRAY);
        smoothModeButton.setForeground(Color.WHITE);
        smoothModeButton.setFont(new Font("Arial", Font.BOLD, 10));
        
        modePanel.add(normalModeButton);
        modePanel.add(smoothModeButton);
        
        return modePanel;
    }
    
    /**
     * Create the size control panel
     */
    private JPanel createSizePanel() {
        JPanel sizePanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));
        sizePanel.setBackground(BACKGROUND_COLOR);
        sizePanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Color.CYAN), 
            "Display Size", 
            0, 0, new Font("Arial", Font.BOLD, 10), Color.CYAN));
        
        smallSizeButton = new JButton("Small");
        smallSizeButton.setPreferredSize(new Dimension(70, 25));
        smallSizeButton.setBackground(Color.DARK_GRAY);
        smallSizeButton.setForeground(Color.WHITE);
        smallSizeButton.setFont(new Font("Arial", Font.BOLD, 9));
        
        mediumSizeButton = new JButton("Medium");
        mediumSizeButton.setPreferredSize(new Dimension(70, 25));
        mediumSizeButton.setBackground(Color.DARK_GRAY);
        mediumSizeButton.setForeground(Color.WHITE);
        mediumSizeButton.setFont(new Font("Arial", Font.BOLD, 9));
        
        largeSizeButton = new JButton("Large");
        largeSizeButton.setPreferredSize(new Dimension(70, 25));
        largeSizeButton.setBackground(Color.BLUE);
        largeSizeButton.setForeground(Color.WHITE);
        largeSizeButton.setFont(new Font("Arial", Font.BOLD, 9));
        
        extraLargeSizeButton = new JButton("X-Large");
        extraLargeSizeButton.setPreferredSize(new Dimension(70, 25));
        extraLargeSizeButton.setBackground(Color.DARK_GRAY);
        extraLargeSizeButton.setForeground(Color.WHITE);
        extraLargeSizeButton.setFont(new Font("Arial", Font.BOLD, 9));
        
        sizePanel.add(smallSizeButton);
        sizePanel.add(mediumSizeButton);
        sizePanel.add(largeSizeButton);
        sizePanel.add(extraLargeSizeButton);
        
        return sizePanel;
    }
    
    /**
     * Update clock size based on current size setting
     */
    private void updateClockSize() {
        switch (currentSize) {
            case SMALL:
                clockSize = 200;
                smallSizeButton.setBackground(Color.BLUE);
                mediumSizeButton.setBackground(Color.DARK_GRAY);
                largeSizeButton.setBackground(Color.DARK_GRAY);
                extraLargeSizeButton.setBackground(Color.DARK_GRAY);
                break;
            case MEDIUM:
                clockSize = 250;
                smallSizeButton.setBackground(Color.DARK_GRAY);
                mediumSizeButton.setBackground(Color.BLUE);
                largeSizeButton.setBackground(Color.DARK_GRAY);
                extraLargeSizeButton.setBackground(Color.DARK_GRAY);
                break;
            case LARGE:
                clockSize = 300;
                smallSizeButton.setBackground(Color.DARK_GRAY);
                mediumSizeButton.setBackground(Color.DARK_GRAY);
                largeSizeButton.setBackground(Color.BLUE);
                extraLargeSizeButton.setBackground(Color.DARK_GRAY);
                break;
            case EXTRA_LARGE:
                clockSize = 400;
                smallSizeButton.setBackground(Color.DARK_GRAY);
                mediumSizeButton.setBackground(Color.DARK_GRAY);
                largeSizeButton.setBackground(Color.DARK_GRAY);
                extraLargeSizeButton.setBackground(Color.BLUE);
                break;
        }
        
        if (clockPanel != null) {
            clockPanel.setPreferredSize(new Dimension(clockSize, clockSize));
            // Repack the window to adjust size
            pack();
        }
    }
    
    /**
     * Set the display size and update the clock
     */
    private void setDisplaySize(DisplaySize size) {
        currentSize = size;
        updateClockSize();
    }
    
    /**
     * Setup event handlers for buttons
     */
    private void setupEventHandlers() {
        startButton.addActionListener(_ -> startClock());
        stopButton.addActionListener(_ -> stopClock());
        exitButton.addActionListener(_ -> exitApplication());
        
        normalModeButton.addActionListener(_ -> setUpdateMode(false));
        smoothModeButton.addActionListener(_ -> setUpdateMode(true));
        
        smallSizeButton.addActionListener(_ -> setDisplaySize(DisplaySize.SMALL));
        mediumSizeButton.addActionListener(_ -> setDisplaySize(DisplaySize.MEDIUM));
        largeSizeButton.addActionListener(_ -> setDisplaySize(DisplaySize.LARGE));
        extraLargeSizeButton.addActionListener(_ -> setDisplaySize(DisplaySize.EXTRA_LARGE));
    }
    
    /**
     * Set the update mode (normal vs smooth)
     */
    private void setUpdateMode(boolean smooth) {
        smoothMode = smooth;
        if (smooth) {
            smoothModeButton.setBackground(Color.BLUE);
            normalModeButton.setBackground(Color.DARK_GRAY);
            statusLabel.setText("Status: " + (isRunning ? "Running" : "Stopped") + " - Smooth Mode (100ms updates)");
        } else {
            normalModeButton.setBackground(Color.BLUE);
            smoothModeButton.setBackground(Color.DARK_GRAY);
            statusLabel.setText("Status: " + (isRunning ? "Running" : "Stopped") + " - Normal Mode (1s updates)");
        }
    }
    
    /**
     * Start the clock thread
     */
    private void startClock() {
        if (!isRunning) {
            isRunning = true;
            clockThread = new ClockThread();
            clockThread.start();
            
            startButton.setEnabled(false);
            stopButton.setEnabled(true);
            
            String mode = smoothMode ? "Smooth Mode (100ms)" : "Normal Mode (1s)";
            statusLabel.setText("Status: Running - " + mode);
            
            System.out.println("Analog clock thread started: " + clockThread.getName());
        }
    }
    
    /**
     * Stop the clock thread
     */
    private void stopClock() {
        if (isRunning) {
            isRunning = false;
            
            if (clockThread != null) {
                try {
                    clockThread.interrupt();
                    clockThread.join(1000);
                    System.out.println("Analog clock thread stopped: " + clockThread.getState());
                } catch (InterruptedException e) {
                    System.err.println("Thread interruption during join: " + e.getMessage());
                    Thread.currentThread().interrupt();
                }
            }
            
            startButton.setEnabled(true);
            stopButton.setEnabled(false);
            digitalTimeLabel.setText("--:--:--");
            
            String mode = smoothMode ? "Smooth Mode" : "Normal Mode";
            statusLabel.setText("Status: Stopped - " + mode);
        }
    }
    
    /**
     * Clean exit with proper thread cleanup
     */
    private void exitApplication() {
        if (isRunning) {
            stopClock();
        }
        System.out.println("Analog clock application exiting...");
        System.exit(0);
    }
    
    /**
     * Custom panel for drawing the analog clock
     */
    private class ClockPanel extends JPanel {
        
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g.create();
            
            // Enable anti-aliasing for smooth graphics
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            int width = getWidth();
            int height = getHeight();
            int centerX = width / 2;
            int centerY = height / 2;
            int radius = Math.min(width, height) / 2 - 20;
            
            // Draw clock face
            drawClockFace(g2d, centerX, centerY, radius);
            
            // Draw numbers
            drawNumbers(g2d, centerX, centerY, radius);
            
            // Draw hands if clock is running
            if (isRunning) {
                Calendar cal = Calendar.getInstance();
                int hours = cal.get(Calendar.HOUR);
                int minutes = cal.get(Calendar.MINUTE);
                int seconds = cal.get(Calendar.SECOND);
                
                drawHands(g2d, centerX, centerY, radius, hours, minutes, seconds);
            }
            
            // Draw center dot
            g2d.setColor(Color.BLACK);
            g2d.fillOval(centerX - 5, centerY - 5, 10, 10);
            
            g2d.dispose();
        }
        
        /**
         * Draw the clock face (circle and markings)
         */
        private void drawClockFace(Graphics2D g2d, int centerX, int centerY, int radius) {
            // Draw outer circle
            g2d.setColor(CLOCK_FACE_COLOR);
            g2d.fillOval(centerX - radius, centerY - radius, 2 * radius, 2 * radius);
            
            // Draw border
            g2d.setColor(Color.BLACK);
            g2d.setStroke(new BasicStroke(3));
            g2d.drawOval(centerX - radius, centerY - radius, 2 * radius, 2 * radius);
            
            // Draw hour markings
            g2d.setStroke(new BasicStroke(2));
            for (int i = 0; i < 12; i++) {
                double angle = Math.toRadians(i * 30 - 90);
                int x1 = centerX + (int) ((radius - 15) * Math.cos(angle));
                int y1 = centerY + (int) ((radius - 15) * Math.sin(angle));
                int x2 = centerX + (int) ((radius - 5) * Math.cos(angle));
                int y2 = centerY + (int) ((radius - 5) * Math.sin(angle));
                g2d.drawLine(x1, y1, x2, y2);
            }
            
            // Draw minute markings
            g2d.setStroke(new BasicStroke(1));
            for (int i = 0; i < 60; i++) {
                if (i % 5 != 0) { // Skip hour markings
                    double angle = Math.toRadians(i * 6 - 90);
                    int x1 = centerX + (int) ((radius - 10) * Math.cos(angle));
                    int y1 = centerY + (int) ((radius - 10) * Math.sin(angle));
                    int x2 = centerX + (int) ((radius - 5) * Math.cos(angle));
                    int y2 = centerY + (int) ((radius - 5) * Math.sin(angle));
                    g2d.drawLine(x1, y1, x2, y2);
                }
            }
        }
        
        /**
         * Draw clock numbers
         */
        private void drawNumbers(Graphics2D g2d, int centerX, int centerY, int radius) {
            g2d.setColor(NUMBERS_COLOR);
            g2d.setFont(new Font("Arial", Font.BOLD, 16));
            FontMetrics fm = g2d.getFontMetrics();
            
            for (int i = 1; i <= 12; i++) {
                double angle = Math.toRadians(i * 30 - 90);
                int x = centerX + (int) ((radius - 30) * Math.cos(angle));
                int y = centerY + (int) ((radius - 30) * Math.sin(angle));
                
                String number = String.valueOf(i);
                int stringWidth = fm.stringWidth(number);
                int stringHeight = fm.getAscent();
                
                g2d.drawString(number, x - stringWidth / 2, y + stringHeight / 2);
            }
        }
        
        /**
         * Draw clock hands
         */
        private void drawHands(Graphics2D g2d, int centerX, int centerY, int radius, 
                              int hours, int minutes, int seconds) {
            
            // Calculate angles (0 degrees is at 12 o'clock)
            double hourAngle = Math.toRadians((hours % 12) * 30 + minutes * 0.5 - 90);
            double minuteAngle = Math.toRadians(minutes * 6 + seconds * 0.1 - 90);
            double secondAngle = Math.toRadians(seconds * 6 - 90);
            
            // Draw hour hand
            g2d.setColor(HOUR_HAND_COLOR);
            g2d.setStroke(new BasicStroke(4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int hourLength = radius / 2;
            int hourX = centerX + (int) (hourLength * Math.cos(hourAngle));
            int hourY = centerY + (int) (hourLength * Math.sin(hourAngle));
            g2d.drawLine(centerX, centerY, hourX, hourY);
            
            // Draw minute hand
            g2d.setColor(MINUTE_HAND_COLOR);
            g2d.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int minuteLength = (int) (radius * 0.7);
            int minuteX = centerX + (int) (minuteLength * Math.cos(minuteAngle));
            int minuteY = centerY + (int) (minuteLength * Math.sin(minuteAngle));
            g2d.drawLine(centerX, centerY, minuteX, minuteY);
            
            // Draw second hand
            g2d.setColor(SECOND_HAND_COLOR);
            g2d.setStroke(new BasicStroke(1, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int secondLength = (int) (radius * 0.8);
            int secondX = centerX + (int) (secondLength * Math.cos(secondAngle));
            int secondY = centerY + (int) (secondLength * Math.sin(secondAngle));
            g2d.drawLine(centerX, centerY, secondX, secondY);
        }
    }
    
    /**
     * Clock thread for updating the display
     */
    private class ClockThread extends Thread {
        
        public ClockThread() {
            super("AnalogClockThread");
            setDaemon(false);
        }
        
        @Override
        public void run() {
            System.out.println("Analog clock thread started: " + getName());
            
            while (isRunning && !Thread.currentThread().isInterrupted()) {
                try {
                    // Update digital time display and repaint clock
                    SwingUtilities.invokeLater(() -> {
                        if (isRunning) {
                            Date now = new Date();
                            digitalTimeLabel.setText(digitalFormat.format(now));
                            clockPanel.repaint();
                        }
                    });
                    
                    // Sleep based on mode
                    int sleepTime = smoothMode ? 100 : 1000;
                    Thread.sleep(sleepTime);
                    
                } catch (InterruptedException e) {
                    System.out.println("Analog clock thread interrupted: " + e.getMessage());
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            
            System.out.println("Analog clock thread terminated: " + getName());
        }
    }
    
    /**
     * Main method - application entry point
     */
    public static void main(String[] args) {
        // Set system look and feel
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Windows".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
            System.err.println("Could not set system look and feel: " + e.getMessage());
        }
        
        // Create and show GUI on Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            System.out.println("Starting Analog Clock Application...");
            System.out.println("Main thread: " + Thread.currentThread().getName());
            
            AnalogClock clock = new AnalogClock();
            clock.setVisible(true);
            
            System.out.println("Analog clock GUI initialized on EDT: " + SwingUtilities.isEventDispatchThread());
        });
    }
}
