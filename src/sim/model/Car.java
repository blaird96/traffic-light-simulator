package sim.model;

import javafx.beans.property.*;

/**
 * Model class representing a car in the traffic simulation.
 * Uses JavaFX properties for TableView binding.
 */
public class Car {
    private final IntegerProperty id;
    private final DoubleProperty x;
    private final DoubleProperty y;
    private final DoubleProperty speedMps;
    private final IntegerProperty targetIntersectionIndex;
    
    public Car(int id, double x) {
        this.id = new SimpleIntegerProperty(id);
        this.x = new SimpleDoubleProperty(x);
        this.y = new SimpleDoubleProperty(0.0);
        this.speedMps = new SimpleDoubleProperty(0.0);
        this.targetIntersectionIndex = new SimpleIntegerProperty(0);
    }
    
    public Car(int id, double x, double speedMps) {
        this.id = new SimpleIntegerProperty(id);
        this.x = new SimpleDoubleProperty(x);
        this.y = new SimpleDoubleProperty(0.0);
        this.speedMps = new SimpleDoubleProperty(speedMps);
        this.targetIntersectionIndex = new SimpleIntegerProperty(0);
    }
    
    public Car(int id, double x, double y, double speedMps) {
        this.id = new SimpleIntegerProperty(id);
        this.x = new SimpleDoubleProperty(x);
        this.y = new SimpleDoubleProperty(y);
        this.speedMps = new SimpleDoubleProperty(speedMps);
        this.targetIntersectionIndex = new SimpleIntegerProperty(0);
    }
    
    public Car(int id, double x, double y, double speedMps, int targetIntersectionIndex) {
        this.id = new SimpleIntegerProperty(id);
        this.x = new SimpleDoubleProperty(x);
        this.y = new SimpleDoubleProperty(y);
        this.speedMps = new SimpleDoubleProperty(speedMps);
        this.targetIntersectionIndex = new SimpleIntegerProperty(targetIntersectionIndex);
    }
    
    // ID property
    public IntegerProperty idProperty() {
        return id;
    }
    
    public int getId() {
        return id.get();
    }
    
    public void setId(int id) {
        this.id.set(id);
    }
    
    // X property
    public DoubleProperty xProperty() {
        return x;
    }
    
    public double getX() {
        return x.get();
    }
    
    public void setX(double x) {
        this.x.set(x);
    }
    
    // Y property
    public DoubleProperty yProperty() {
        return y;
    }
    
    public double getY() {
        return y.get();
    }
    
    public void setY(double y) {
        this.y.set(y);
    }
    
    // Speed (meters per second) property
    public DoubleProperty speedMpsProperty() {
        return speedMps;
    }
    
    public double getSpeedMps() {
        return speedMps.get();
    }
    
    public void setSpeedMps(double speedMps) {
        this.speedMps.set(speedMps);
    }
    
    // Target intersection index property
    public IntegerProperty targetIntersectionIndexProperty() {
        return targetIntersectionIndex;
    }
    
    public int getTargetIntersectionIndex() {
        return targetIntersectionIndex.get();
    }
    
    public void setTargetIntersectionIndex(int targetIntersectionIndex) {
        this.targetIntersectionIndex.set(targetIntersectionIndex);
    }
    
    @Override
    public String toString() {
        return String.format("Car[id=%d, x=%.2f, y=%.2f, speedMps=%.2f, targetIntersection=%d]", 
            getId(), getX(), getY(), getSpeedMps(), getTargetIntersectionIndex());
    }
}
