package sim.model;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * Model class representing the state of the traffic simulation.
 * Contains observable lists for intersections and cars, and simulation units.
 */
public class SimulationState {
    private final ObservableList<Intersection> intersections;
    private final ObservableList<Car> cars;
    private String units;
    
    public SimulationState() {
        this.intersections = FXCollections.observableArrayList();
        this.cars = FXCollections.observableArrayList();
        this.units = "meters/second";
    }
    
    public SimulationState(String units) {
        this.intersections = FXCollections.observableArrayList();
        this.cars = FXCollections.observableArrayList();
        this.units = units;
    }
    
    // Intersections list
    public ObservableList<Intersection> getIntersections() {
        return intersections;
    }
    
    public void addIntersection(Intersection intersection) {
        intersections.add(intersection);
    }
    
    public void removeIntersection(Intersection intersection) {
        intersections.remove(intersection);
    }
    
    public void clearIntersections() {
        intersections.clear();
    }
    
    // Cars list
    public ObservableList<Car> getCars() {
        return cars;
    }
    
    public void addCar(Car car) {
        cars.add(car);
    }
    
    public void removeCar(Car car) {
        cars.remove(car);
    }
    
    public void clearCars() {
        cars.clear();
    }
    
    // Units
    public String getUnits() {
        return units;
    }
    
    public void setUnits(String units) {
        this.units = units;
    }
    
    // Utility methods
    public int getIntersectionCount() {
        return intersections.size();
    }
    
    public int getCarCount() {
        return cars.size();
    }
    
    public void reset() {
        intersections.clear();
        cars.clear();
    }
    
    @Override
    public String toString() {
        return String.format("SimulationState[intersections=%d, cars=%d, units=%s]",
            getIntersectionCount(), getCarCount(), units);
    }
}
