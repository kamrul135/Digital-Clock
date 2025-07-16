import javax.sound.sampled.*;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * SoundManager class to handle sound playback for alarms and notifications
 */
public class SoundManager {

    /**
     * Plays a sound from the given resource path.
     * The sound file should be located in a directory that is part of the classpath.
     * For example, if soundResourcePath is "/sounds/alarm1.wav",
     * there should be a "sounds" folder at the root of the classpath
     * containing "alarm1.wav".
     *
     * @param soundResourcePath The path to the sound resource (e.g., "/sounds/alarm1.wav").
     */
    public static void playSound(String soundResourcePath) {
        try {
            // Ensure the path starts with a '/' to indicate it's relative to the classpath root.
            if (!soundResourcePath.startsWith("/")) {
                soundResourcePath = "/" + soundResourcePath;
            }
            URL soundURL = SoundManager.class.getResource(soundResourcePath);

            if (soundURL == null) {
                System.err.println("Sound resource not found: " + soundResourcePath +
                                   ". Ensure the 'sounds' folder (e.g., containing 'alarm1.wav') is in your classpath.");
                // Fallback to system beep if resource not found
                java.awt.Toolkit.getDefaultToolkit().beep();
                return;
            }

            Clip clip = AudioSystem.getClip();

            // AudioInputStream and other input streams are closed after clip.open()
            // because Clip reads all audio data into memory at that point.
            try (InputStream audioSrc = soundURL.openStream();
                 InputStream bufferedIn = new BufferedInputStream(audioSrc);
                 AudioInputStream audioIn = AudioSystem.getAudioInputStream(bufferedIn)) {
                
                clip.open(audioIn); // Clip loads all data from audioIn here
            } 
            // audioIn, bufferedIn, and audioSrc are automatically closed here by try-with-resources.

            // Add a listener to close the clip itself once it stops playing.
            // This releases the audio line resource.
            clip.addLineListener(event -> {
                if (event.getType() == LineEvent.Type.STOP) {
                    Clip sourceClip = (Clip) event.getSource();
                    sourceClip.close();
                }
            });
            
            clip.start();

        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            System.err.println("Error playing sound '" + soundResourcePath + "': " + e.getMessage());
            e.printStackTrace();
            // Fallback to system beep on error
            java.awt.Toolkit.getDefaultToolkit().beep();
        }
    }

    /**
     * Plays a default alarm sound.
     * This method expects a sound file (e.g., "alarm1.wav") to be present in a "sounds"
     * directory located at the root of the application's classpath.
     */
    public static void playAlarmSoundDefault() {
        playSound("/sounds/alarm1.wav"); // Assumes alarm1.wav is in "sounds" dir in classpath
    }

    /**
     * Plays a specific alarm sound by number (1-8)
     * @param alarmNumber The alarm sound number (1-8)
     */
    public static void playAlarmSound(int alarmNumber) {
        if (alarmNumber < 1 || alarmNumber > 8) {
            alarmNumber = 1; // Default to alarm1 if invalid number
        }
        playSound("/sounds/alarm" + alarmNumber + ".wav");
    }
}
