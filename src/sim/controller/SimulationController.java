package sim.controller;

import javafx.application.Platform;
import sim.model.Car;
import sim.model.Intersection;
import sim.model.SimulationState;
import sim.model.TrafficLightColor;
import sim.service.CarService;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Controller class that manages the traffic simulation lifecycle.
 * Uses a scheduled executor for simulation ticks (20 Hz) and coordinates
 * with the UI thread for rendering.
 */
public class SimulationController {
    
    private static final int SIMULATION_HZ = 20; // 20 ticks per second
    private static final long TICK_INTERVAL_MS = 1000 / SIMULATION_HZ;
    
    private final SimulationState state;
    private final ReentrantReadWriteLock stateLock;
    private final CarService carService;
    
    private ScheduledExecutorService executor;
    private final AtomicBoolean running;
    private final AtomicBoolean paused;
    
    private long simulationTick;
    private long lastTickTime;
    
    public SimulationController(SimulationState state) {
        this.state = state;
        this.stateLock = new ReentrantReadWriteLock();
        this.carService = new CarService(state);
        this.running = new AtomicBoolean(false);
        this.paused = new AtomicBoolean(false);
        this.simulationTick = 0;
        this.lastTickTime = System.currentTimeMillis();
    }
    
    /**
     * Start the simulation. Creates a new scheduled executor service.
     */
    public void start() {
        if (running.get()) {
            return; // Already running
        }
        
        running.set(true);
        paused.set(false);
        simulationTick = 0;
        lastTickTime = System.currentTimeMillis();
        
        executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "SimulationThread");
            t.setDaemon(true);
            return t;
        });
        
        // Schedule simulation ticks at fixed rate
        executor.scheduleAtFixedRate(
            this::simulationTick,
            0,
            TICK_INTERVAL_MS,
            TimeUnit.MILLISECONDS
        );
        
        System.out.println("Simulation started at " + SIMULATION_HZ + " Hz");
    }
    
    /**
     * Pause the simulation. Simulation thread continues but updates are skipped.
     */
    public void pause() {
        if (!running.get() || paused.get()) {
            return;
        }
        paused.set(true);
        System.out.println("Simulation paused at tick " + simulationTick);
    }
    
    /**
     * Continue the simulation from paused state.
     */
    public void continueSimulation() {
        if (!running.get() || !paused.get()) {
            return;
        }
        paused.set(false);
        lastTickTime = System.currentTimeMillis();
        System.out.println("Simulation continued at tick " + simulationTick);
    }
    
    /**
     * Stop the simulation completely. Shuts down the executor service.
     */
    public void stop() {
        if (!running.get()) {
            return;
        }
        
        running.set(false);
        paused.set(false);
        
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
        
        System.out.println("Simulation stopped at tick " + simulationTick);
    }
    
    /**
     * Main simulation tick. Updates model state only, no UI operations.
     * Runs on the simulation thread.
     */
    private void simulationTick() {
        if (paused.get()) {
            return; // Skip updates when paused
        }
        
        long currentTime = System.currentTimeMillis();
        double deltaTime = (currentTime - lastTickTime) / 1000.0; // seconds
        lastTickTime = currentTime;
        
        // Acquire write lock for state updates
        stateLock.writeLock().lock();
        try {
            // Update cars
            updateCars(deltaTime);
            
            // Update traffic lights
            updateTrafficLights(deltaTime);
            
            simulationTick++;
            
        } finally {
            stateLock.writeLock().unlock();
        }
    }
    
    /**
     * Update car positions using CarService.
     * Handles traffic light interactions and stopping logic.
     * Runs on simulation thread with write lock held.
     */
    private void updateCars(double deltaTime) {
        carService.tick(deltaTime);
    }
    
    /**
     * Update traffic light colors based on their timing.
     * NOTE: Traffic light cycling is now handled by TrafficLightService.
     * This method is kept for potential future use.
     */
    private void updateTrafficLights(double deltaTime) {
        // Traffic lights are now managed by TrafficLightService
        // Each intersection has its own background thread cycling through colors
    }
    
    /**
     * Get a read lock for safe concurrent access to simulation state.
     * Use this when rendering to ensure consistent state.
     */
    public ReentrantReadWriteLock.ReadLock getReadLock() {
        return stateLock.readLock();
    }
    
    /**
     * Check if simulation is currently running.
     */
    public boolean isRunning() {
        return running.get();
    }
    
    /**
     * Check if simulation is currently paused.
     */
    public boolean isPaused() {
        return paused.get();
    }
    
    /**
     * Get the current simulation tick count.
     */
    public long getSimulationTick() {
        return simulationTick;
    }
    
    /**
     * Get the simulation state.
     */
    public SimulationState getState() {
        return state;
    }
}
