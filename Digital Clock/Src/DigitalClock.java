import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Digital Clock Application - Lab 2
 * 
 * This application demonstrates Java thread lifecycle and concurrent programming
 * by implementing a real-time digital clock using Swing GUI and threading.
 * 
 * Key Threading Concepts Demonstrated:
 * 1. Thread creation and lifecycle management
 * 2. Thread synchronization using volatile variables
 * 3. Proper thread termination
 * 4. EDT (Event Dispatch Thread) interaction with worker threads
 */
public class DigitalClock extends JFrame {
    
    // GUI Components
    private JLabel timeLabel;
    private JLabel dateLabel;
    private JButton startButton;
    private JButton stopButton;
    private JButton exitButton;
    private JLabel statusLabel;
    private JButton largeSizeButton;
    private JButton mediumSizeButton;
    
    // Thread management
    private ClockThread clockThread;
    private volatile boolean isRunning = false;
    
    // Display size settings
    private enum DisplaySize { MEDIUM, LARGE }
    private DisplaySize currentSize = DisplaySize.LARGE;
    
    // Time formatting
    private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
    private SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, MMMM dd, yyyy");
    
    public DigitalClock() {
        initializeGUI();
        setupEventHandlers();
    }
    
    /**
     * Initialize the GUI components and layout
     */
    private void initializeGUI() {
        setTitle("Digital Clock - Thread Lifecycle Demo");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        
        // Create main panel with border layout
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        mainPanel.setBackground(Color.BLACK);
        
        // Create time display panel
        JPanel timePanel = createTimePanel();
        
        // Create control panel
        JPanel controlPanel = createControlPanel();
        
        // Create size control panel
        JPanel sizePanel = createSizePanel();
        
        // Create status panel
        JPanel statusPanel = createStatusPanel();
        
        // Create bottom panel to hold both control and size panels
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBackground(Color.BLACK);
        bottomPanel.add(controlPanel, BorderLayout.CENTER);
        bottomPanel.add(sizePanel, BorderLayout.SOUTH);
        
        // Add panels to main panel
        mainPanel.add(timePanel, BorderLayout.CENTER);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);
        mainPanel.add(statusPanel, BorderLayout.NORTH);
        
        add(mainPanel);
        pack();
        setLocationRelativeTo(null); // Center the window
    }
    
    /**
     * Create the time display panel
     */
    private JPanel createTimePanel() {
        JPanel timePanel = new JPanel(new GridLayout(2, 1, 5, 5));
        timePanel.setBackground(Color.BLACK);
        timePanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createRaisedBevelBorder(),
            BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));
        
        // Time label
        timeLabel = new JLabel("--:--:--", SwingConstants.CENTER);
        updateDisplaySize(); // Set initial size
        timeLabel.setForeground(Color.GREEN);
        timeLabel.setBackground(Color.BLACK);
        timeLabel.setOpaque(true);
        
        // Date label
        dateLabel = new JLabel("Clock Stopped", SwingConstants.CENTER);
        dateLabel.setFont(new Font("Arial", Font.BOLD, 16));
        dateLabel.setForeground(Color.CYAN);
        dateLabel.setBackground(Color.BLACK);
        dateLabel.setOpaque(true);
        
        timePanel.add(timeLabel);
        timePanel.add(dateLabel);
        
        return timePanel;
    }
    
    /**
     * Create the control buttons panel
     */
    private JPanel createControlPanel() {
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        controlPanel.setBackground(Color.BLACK);
        
        // Start button
        startButton = new JButton("Start Clock");
        startButton.setPreferredSize(new Dimension(120, 30));
        startButton.setBackground(Color.GREEN);
        startButton.setForeground(Color.BLACK);
        startButton.setFont(new Font("Arial", Font.BOLD, 12));
        
        // Stop button
        stopButton = new JButton("Stop Clock");
        stopButton.setPreferredSize(new Dimension(120, 30));
        stopButton.setBackground(Color.RED);
        stopButton.setForeground(Color.WHITE);
        stopButton.setFont(new Font("Arial", Font.BOLD, 12));
        stopButton.setEnabled(false);
        
        // Exit button
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
        statusPanel.setBackground(Color.BLACK);
        
        statusLabel = new JLabel("Status: Clock Stopped");
        statusLabel.setForeground(Color.YELLOW);
        statusLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        
        statusPanel.add(statusLabel);
        
        return statusPanel;
    }
    
    /**
     * Create the size control panel
     */
    private JPanel createSizePanel() {
        JPanel sizePanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        sizePanel.setBackground(Color.BLACK);
        sizePanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Color.CYAN), 
            "Display Size", 
            0, 0, new Font("Arial", Font.BOLD, 10), Color.CYAN));
        
        // Large size button
        largeSizeButton = new JButton("Large");
        largeSizeButton.setPreferredSize(new Dimension(80, 25));
        largeSizeButton.setBackground(Color.BLUE);
        largeSizeButton.setForeground(Color.WHITE);
        largeSizeButton.setFont(new Font("Arial", Font.BOLD, 10));
        
        // Medium size button
        mediumSizeButton = new JButton("Medium");
        mediumSizeButton.setPreferredSize(new Dimension(80, 25));
        mediumSizeButton.setBackground(Color.DARK_GRAY);
        mediumSizeButton.setForeground(Color.WHITE);
        mediumSizeButton.setFont(new Font("Arial", Font.BOLD, 10));
        
        sizePanel.add(largeSizeButton);
        sizePanel.add(mediumSizeButton);
        
        return sizePanel;
    }
    
    /**
     * Update display size based on current size setting
     */
    private void updateDisplaySize() {
        int timeSize, dateSize;
        
        switch (currentSize) {
            case LARGE:
                timeSize = 48;
                dateSize = 16;
                largeSizeButton.setBackground(Color.BLUE);
                mediumSizeButton.setBackground(Color.DARK_GRAY);
                break;
            case MEDIUM:
                timeSize = 32;
                dateSize = 12;
                largeSizeButton.setBackground(Color.DARK_GRAY);
                mediumSizeButton.setBackground(Color.BLUE);
                break;
            default:
                timeSize = 48;
                dateSize = 16;
        }
        
        timeLabel.setFont(new Font(Font.MONOSPACED, Font.BOLD, timeSize));
        dateLabel.setFont(new Font("Arial", Font.BOLD, dateSize));
        
        // Repack the window to adjust size
        pack();
    }
    
    /**
     * Set the display size and update the GUI
     */
    private void setDisplaySize(DisplaySize size) {
        currentSize = size;
        updateDisplaySize();
    }
    
    /**
     * Setup event handlers for buttons
     */
    private void setupEventHandlers() {
        startButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                startClock();
            }
        });
        
        stopButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                stopClock();
            }
        });
        
        exitButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                exitApplication();
            }
        });
        
        largeSizeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setDisplaySize(DisplaySize.LARGE);
            }
        });
        
        mediumSizeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setDisplaySize(DisplaySize.MEDIUM);
            }
        });
    }
    
    /**
     * Start the clock thread
     * Demonstrates thread creation and lifecycle management
     */
    private void startClock() {
        if (!isRunning) {
            isRunning = true;
            clockThread = new ClockThread();
            clockThread.start(); // Thread enters RUNNABLE state
            
            // Update UI state
            startButton.setEnabled(false);
            stopButton.setEnabled(true);
            statusLabel.setText("Status: Clock Running - Thread State: " + clockThread.getState());
            
            System.out.println("Clock thread started. Thread Name: " + clockThread.getName());
        }
    }
    
    /**
     * Stop the clock thread
     * Demonstrates proper thread termination
     */
    private void stopClock() {
        if (isRunning) {
            isRunning = false; // Signal thread to stop
            
            // Interrupt and wait for thread to finish (demonstrates thread joining)
            if (clockThread != null) {
                try {
                    clockThread.interrupt(); // Properly interrupt the thread
                    clockThread.join(1000); // Wait up to 1 second for thread to finish
                    System.out.println("Clock thread stopped. Final state: " + clockThread.getState());
                } catch (InterruptedException e) {
                    System.err.println("Thread interruption during join: " + e.getMessage());
                    Thread.currentThread().interrupt(); // Restore interrupt status
                }
            }
            
            // Update UI state
            startButton.setEnabled(true);
            stopButton.setEnabled(false);
            timeLabel.setText("--:--:--");
            dateLabel.setText("Clock Stopped");
            statusLabel.setText("Status: Clock Stopped");
        }
    }
    
    /**
     * Clean exit with proper thread cleanup
     */
    private void exitApplication() {
        if (isRunning) {
            stopClock();
        }
        System.out.println("Application exiting...");
        System.exit(0);
    }
    
    /**
     * Inner class representing the clock thread
     * Demonstrates thread lifecycle and concurrent programming concepts
     */
    private class ClockThread extends Thread {
        
        public ClockThread() {
            super("ClockThread"); // Give thread a meaningful name
            setDaemon(false); // Ensure this is not a daemon thread
        }
        
        @Override
        public void run() {
            System.out.println("Clock thread entering RUN state. Thread: " + getName());
            
            while (isRunning) {
                try {
                    // Get current time
                    Date now = new Date();
                    final String timeString = timeFormat.format(now);
                    final String dateString = dateFormat.format(now);
                    
                    // Update GUI on Event Dispatch Thread (EDT)
                    // This demonstrates proper thread synchronization with Swing
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            timeLabel.setText(timeString);
                            dateLabel.setText(dateString);
                            if (isRunning) {
                                statusLabel.setText("Status: Clock Running - EDT State: " + 
                                    Thread.currentThread().getState() + " | Clock Thread State: " + 
                                    ClockThread.this.getState());
                            }
                        }
                    });
                    
                    // Sleep for 1 second (thread enters TIMED_WAITING state)
                    Thread.sleep(1000);
                    
                } catch (InterruptedException e) {
                    System.out.println("Clock thread interrupted: " + e.getMessage());
                    Thread.currentThread().interrupt(); // Restore interrupt status
                    break; // Exit loop if interrupted
                }
            }
            
            System.out.println("Clock thread exiting RUN state. Thread: " + getName());
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
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                System.out.println("Starting Digital Clock Application...");
                System.out.println("Main thread: " + Thread.currentThread().getName());
                
                DigitalClock clock = new DigitalClock();
                clock.setVisible(true);
                
                System.out.println("GUI initialized on EDT: " + SwingUtilities.isEventDispatchThread());
            }
        });
    }
}
