package sim.model;

import javafx.beans.property.*;
import java.time.Duration;

/**
 * Model class representing an intersection in the traffic simulation.
 * Uses JavaFX properties for TableView binding.
 */
public class Intersection {
    private final IntegerProperty id;
    private final DoubleProperty x;
    private final DoubleProperty distanceToNext;
    private final ObjectProperty<TrafficLightColor> color;
    private final ObjectProperty<Duration> greenDuration;
    private final ObjectProperty<Duration> yellowDuration;
    private final ObjectProperty<Duration> redDuration;
    
    public Intersection(int id, double x) {
        this.id = new SimpleIntegerProperty(id);
        this.x = new SimpleDoubleProperty(x);
        this.distanceToNext = new SimpleDoubleProperty(1000.0);
        this.color = new SimpleObjectProperty<>(TrafficLightColor.RED);
        this.greenDuration = new SimpleObjectProperty<>(Duration.ofSeconds(10));
        this.yellowDuration = new SimpleObjectProperty<>(Duration.ofSeconds(2));
        this.redDuration = new SimpleObjectProperty<>(Duration.ofSeconds(12));
    }
    
    public Intersection(int id, double x, double distanceToNext) {
        this.id = new SimpleIntegerProperty(id);
        this.x = new SimpleDoubleProperty(x);
        this.distanceToNext = new SimpleDoubleProperty(distanceToNext);
        this.color = new SimpleObjectProperty<>(TrafficLightColor.RED);
        this.greenDuration = new SimpleObjectProperty<>(Duration.ofSeconds(10));
        this.yellowDuration = new SimpleObjectProperty<>(Duration.ofSeconds(2));
        this.redDuration = new SimpleObjectProperty<>(Duration.ofSeconds(12));
    }
    
    public Intersection(int id, double x, double distanceToNext, TrafficLightColor color) {
        this.id = new SimpleIntegerProperty(id);
        this.x = new SimpleDoubleProperty(x);
        this.distanceToNext = new SimpleDoubleProperty(distanceToNext);
        this.color = new SimpleObjectProperty<>(color);
        this.greenDuration = new SimpleObjectProperty<>(Duration.ofSeconds(10));
        this.yellowDuration = new SimpleObjectProperty<>(Duration.ofSeconds(2));
        this.redDuration = new SimpleObjectProperty<>(Duration.ofSeconds(12));
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
    
    // Distance to next property
    public DoubleProperty distanceToNextProperty() {
        return distanceToNext;
    }
    
    public double getDistanceToNext() {
        return distanceToNext.get();
    }
    
    public void setDistanceToNext(double distanceToNext) {
        this.distanceToNext.set(distanceToNext);
    }
    
    // Color property
    public ObjectProperty<TrafficLightColor> colorProperty() {
        return color;
    }
    
    public TrafficLightColor getColor() {
        return color.get();
    }
    
    public void setColor(TrafficLightColor color) {
        this.color.set(color);
    }
    
    // Green duration property
    public ObjectProperty<Duration> greenDurationProperty() {
        return greenDuration;
    }
    
    public Duration getGreenDuration() {
        return greenDuration.get();
    }
    
    public void setGreenDuration(Duration greenDuration) {
        this.greenDuration.set(greenDuration);
    }
    
    // Yellow duration property
    public ObjectProperty<Duration> yellowDurationProperty() {
        return yellowDuration;
    }
    
    public Duration getYellowDuration() {
        return yellowDuration.get();
    }
    
    public void setYellowDuration(Duration yellowDuration) {
        this.yellowDuration.set(yellowDuration);
    }
    
    // Red duration property
    public ObjectProperty<Duration> redDurationProperty() {
        return redDuration;
    }
    
    public Duration getRedDuration() {
        return redDuration.get();
    }
    
    public void setRedDuration(Duration redDuration) {
        this.redDuration.set(redDuration);
    }
    
    @Override
    public String toString() {
        return String.format("Intersection[id=%d, x=%.2f, distanceToNext=%.2f, color=%s]", 
            getId(), getX(), getDistanceToNext(), getColor());
    }
}
