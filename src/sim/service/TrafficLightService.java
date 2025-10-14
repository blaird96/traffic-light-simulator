package sim.service;

import javafx.application.Platform;
import sim.model.Intersection;
import sim.model.TrafficLightColor;

import java.util.Map;
import java.util.concurrent.*;

/**
 * Service that manages traffic light cycles for intersections.
 * Each intersection gets its own scheduled task that cycles through
 * GREEN → YELLOW → RED using the configured durations.
 * Updates are performed thread-safely via Platform.runLater.
 */
public class TrafficLightService {
    
    private final ScheduledExecutorService executor;
    private final Map<Intersection, Future<?>> lightTasks;
    private volatile boolean running;
    private volatile boolean paused;
    
    public TrafficLightService() {
        // Create thread pool for light cycle tasks
        this.executor = Executors.newScheduledThreadPool(
            4, // Allow up to 4 concurrent intersection tasks
            r -> {
                Thread t = new Thread(r, "TrafficLight-" + System.nanoTime());
                t.setDaemon(true);
                return t;
            }
        );
        this.lightTasks = new ConcurrentHashMap<>();
        this.running = false;
        this.paused = false;
    }
    
    /**
     * Start managing traffic lights for the given intersection.
     * Creates a background task that cycles through light colors.
     * 
     * @param intersection The intersection to manage
     */
    public void startLight(Intersection intersection) {
        if (!running || lightTasks.containsKey(intersection)) {
            return; // Already managing this intersection or service not running
        }
        
        // Create a cycling task for this intersection
        Future<?> future = executor.submit(new LightCycleTask(intersection));
        lightTasks.put(intersection, future);
        
        System.out.println("Started traffic light for intersection " + intersection.getId());
    }
    
    /**
     * Stop managing traffic lights for the given intersection.
     * 
     * @param intersection The intersection to stop managing
     */
    public void stopLight(Intersection intersection) {
        Future<?> future = lightTasks.remove(intersection);
        if (future != null) {
            future.cancel(false); // Don't interrupt if running
            System.out.println("Stopped traffic light for intersection " + intersection.getId());
        }
    }
    
    /**
     * Start the traffic light service.
     */
    public void start() {
        if (running) {
            return;
        }
        running = true;
        System.out.println("TrafficLightService started");
    }
    
    /**
     * Stop the traffic light service and cancel all light cycles.
     */
    public void stop() {
        if (!running) {
            return;
        }
        
        running = false;
        
        // Cancel all active light tasks
        for (Map.Entry<Intersection, Future<?>> entry : lightTasks.entrySet()) {
            entry.getValue().cancel(false);
        }
        lightTasks.clear();
        
        // Shutdown executor
        executor.shutdown();
        try {
            if (!executor.awaitTermination(2, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        System.out.println("TrafficLightService stopped");
    }
    
    /**
     * Pause all traffic light cycles.
     * Lights will freeze at their current color until resumed.
     */
    public void pause() {
        if (!running || paused) {
            return;
        }
        paused = true;
        System.out.println("TrafficLightService paused");
    }
    
    /**
     * Resume all traffic light cycles from paused state.
     */
    public void resume() {
        if (!running || !paused) {
            return;
        }
        paused = false;
        System.out.println("TrafficLightService resumed");
    }
    
    /**
     * Check if the service is running.
     * 
     * @return true if running, false otherwise
     */
    public boolean isRunning() {
        return running;
    }
    
    /**
     * Check if the service is paused.
     * 
     * @return true if paused, false otherwise
     */
    public boolean isPaused() {
        return paused;
    }
    
    /**
     * Task that cycles a traffic light through GREEN → YELLOW → RED.
     * Runs continuously until cancelled.
     */
    private class LightCycleTask implements Runnable {
        private final Intersection intersection;
        
        public LightCycleTask(Intersection intersection) {
            this.intersection = intersection;
        }
        
        @Override
        public void run() {
            try {
                while (running && !Thread.currentThread().isInterrupted()) {
                    // Cycle through: GREEN → YELLOW → RED
                    
                    // GREEN phase
                    setLightColor(TrafficLightColor.GREEN);
                    sleepWhileCheckingPause(intersection.getGreenDuration().toMillis());
                    
                    if (!running) break;
                    
                    // YELLOW phase
                    setLightColor(TrafficLightColor.YELLOW);
                    sleepWhileCheckingPause(intersection.getYellowDuration().toMillis());
                    
                    if (!running) break;
                    
                    // RED phase
                    setLightColor(TrafficLightColor.RED);
                    sleepWhileCheckingPause(intersection.getRedDuration().toMillis());
                }
            } catch (InterruptedException e) {
                // Task was cancelled, exit gracefully
                Thread.currentThread().interrupt();
            }
        }
        
        /**
         * Sleep for the specified duration, but wake up periodically to check
         * if we're paused. If paused, wait until resumed before continuing.
         * 
         * @param millis Duration to sleep in milliseconds
         * @throws InterruptedException if the thread is interrupted
         */
        private void sleepWhileCheckingPause(long millis) throws InterruptedException {
            long endTime = System.currentTimeMillis() + millis;
            
            while (System.currentTimeMillis() < endTime && running) {
                // While paused, spin-wait with small sleeps
                while (paused && running) {
                    Thread.sleep(100); // Check pause state every 100ms
                }
                
                if (!running) break;
                
                // Sleep for remaining time or 100ms, whichever is smaller
                long remaining = endTime - System.currentTimeMillis();
                if (remaining > 0) {
                    Thread.sleep(Math.min(remaining, 100));
                }
            }
        }
        
        /**
         * Set the traffic light color using Platform.runLater for thread safety.
         * 
         * @param color The new color
         */
        private void setLightColor(TrafficLightColor color) {
            Platform.runLater(() -> intersection.setColor(color));
        }
    }
}
