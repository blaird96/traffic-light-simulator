package sim.service;

import javafx.application.Platform;
import sim.model.Car;
import sim.model.Intersection;
import sim.model.SimulationState;
import sim.model.TrafficLightColor;

import java.util.List;

/**
 * Service that manages car movement and traffic light interactions.
 * Updates car positions based on speed, handles stopping at red lights,
 * and manages progression through intersections.
 */
public class CarService {
    
    private static final double STOPPING_THRESHOLD = 5.0; // meters before intersection
    private static final double PASSING_THRESHOLD = 2.0;  // meters past intersection to consider "passed"
    
    private final SimulationState state;
    
    public CarService(SimulationState state) {
        this.state = state;
    }
    
    /**
     * Update all cars for one simulation tick.
     * Advances positions, checks for red lights, and handles intersection progression.
     * 
     * @param deltaTime Time elapsed since last tick in seconds
     */
    public void tick(double deltaTime) {
        List<Intersection> intersections = state.getIntersections();
        List<Car> cars = state.getCars();
        
        for (Car car : cars) {
            updateCar(car, intersections, deltaTime);
        }
    }
    
    /**
     * Update a single car's position and handle intersection logic.
     * 
     * @param car The car to update
     * @param intersections List of all intersections
     * @param deltaTime Time elapsed in seconds
     */
    private void updateCar(Car car, List<Intersection> intersections, double deltaTime) {
        if (intersections.isEmpty()) {
            // No intersections, just move forward
            advanceCarPosition(car, deltaTime);
            return;
        }
        
        // Get current target intersection
        int targetIndex = car.getTargetIntersectionIndex();
        if (targetIndex >= intersections.size()) {
            // Invalid index, wrap to 0
            Platform.runLater(() -> car.setTargetIntersectionIndex(0));
            targetIndex = 0;
        }
        
        Intersection targetIntersection = intersections.get(targetIndex);
        double intersectionX = targetIntersection.getX();
        double currentX = car.getX();
        
        // Calculate desired new position
        double desiredX = currentX + (car.getSpeedMps() * deltaTime);
        
        // Check if we're approaching the intersection
        boolean isApproaching = currentX < intersectionX && desiredX >= (intersectionX - STOPPING_THRESHOLD);
        boolean hasPassedIntersection = currentX > intersectionX + PASSING_THRESHOLD;
        
        if (hasPassedIntersection) {
            // Car has passed the current target intersection, advance to next
            int nextIndex = (targetIndex + 1) % intersections.size();
            Platform.runLater(() -> car.setTargetIntersectionIndex(nextIndex));
            
            // Continue moving
            final double finalX = desiredX;
            Platform.runLater(() -> car.setX(finalX));
            
        } else if (isApproaching) {
            // Approaching intersection - check light color
            TrafficLightColor lightColor = targetIntersection.getColor();
            
            if (lightColor == TrafficLightColor.RED) {
                // RED light - stop just before intersection
                double stoppingPoint = intersectionX - STOPPING_THRESHOLD;
                
                if (desiredX >= stoppingPoint) {
                    // Would overshoot, clamp to stopping point ("stop on a dime")
                    final double clampedX = stoppingPoint;
                    Platform.runLater(() -> car.setX(clampedX));
                } else {
                    // Haven't reached stopping point yet, continue normally
                    final double finalX = desiredX;
                    Platform.runLater(() -> car.setX(finalX));
                }
                
            } else {
                // GREEN or YELLOW - pass through
                final double finalX = desiredX;
                Platform.runLater(() -> car.setX(finalX));
            }
            
        } else {
            // Not near intersection, move normally
            final double finalX = desiredX;
            Platform.runLater(() -> car.setX(finalX));
        }
        
        // Keep y at 0 (cars stay on horizontal line)
        if (car.getY() != 0.0) {
            Platform.runLater(() -> car.setY(0.0));
        }
    }
    
    /**
     * Advance car position without intersection checks.
     * 
     * @param car The car to advance
     * @param deltaTime Time elapsed in seconds
     */
    private void advanceCarPosition(Car car, double deltaTime) {
        double newX = car.getX() + (car.getSpeedMps() * deltaTime);
        
        // Simple wrapping at world boundary
        if (newX > 1000) {
            newX = newX - 1000;
        }
        
        final double finalX = newX;
        Platform.runLater(() -> {
            car.setX(finalX);
            if (car.getY() != 0.0) {
                car.setY(0.0);
            }
        });
    }
}
