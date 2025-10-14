package sim.service;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Clock service that provides a live timestamp updated every second.
 * Uses a ScheduledExecutorService on a background thread and updates
 * a JavaFX StringProperty on the FX Application Thread for thread safety.
 */
public class ClockService {
    
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    
    private final StringProperty currentTime;
    private ScheduledExecutorService executor;
    private volatile boolean running;
    
    public ClockService() {
        this.currentTime = new SimpleStringProperty(getCurrentTimeFormatted());
        this.running = false;
    }
    
    /**
     * Start the clock service. Updates the time every second.
     */
    public void start() {
        if (running) {
            return; // Already running
        }
        
        running = true;
        
        // Create a daemon thread executor for clock updates
        executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "ClockServiceThread");
            t.setDaemon(true);
            return t;
        });
        
        // Schedule time updates at 1 Hz (every second)
        executor.scheduleAtFixedRate(
            this::updateTime,
            0,
            1,
            TimeUnit.SECONDS
        );
        
        System.out.println("ClockService started");
    }
    
    /**
     * Stop the clock service and shutdown the executor.
     */
    public void stop() {
        if (!running) {
            return;
        }
        
        running = false;
        
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(1, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        System.out.println("ClockService stopped");
    }
    
    /**
     * Update the current time property.
     * Runs on the clock service thread, but updates the JavaFX property
     * on the FX Application Thread via Platform.runLater.
     */
    private void updateTime() {
        String formattedTime = getCurrentTimeFormatted();
        
        // Update the JavaFX property on the FX Application Thread
        Platform.runLater(() -> currentTime.set(formattedTime));
    }
    
    /**
     * Get the current time as a formatted string.
     */
    private String getCurrentTimeFormatted() {
        return LocalTime.now().format(TIME_FORMATTER);
    }
    
    /**
     * Get the current time property. This property is updated every second
     * and can be bound to UI components.
     * 
     * @return StringProperty containing the current time (HH:mm:ss)
     */
    public StringProperty currentTimeProperty() {
        return currentTime;
    }
    
    /**
     * Get the current time value.
     * 
     * @return Current time as a string (HH:mm:ss)
     */
    public String getCurrentTime() {
        return currentTime.get();
    }
    
    /**
     * Check if the clock service is running.
     * 
     * @return true if running, false otherwise
     */
    public boolean isRunning() {
        return running;
    }
}
