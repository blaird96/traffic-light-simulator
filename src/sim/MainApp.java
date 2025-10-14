package sim;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.geometry.Insets;
import sim.controller.SimulationController;
import sim.model.Car;
import sim.model.Intersection;
import sim.model.SimulationState;
import sim.model.TrafficLightColor;
import sim.service.ClockService;
import sim.service.TrafficLightService;
import sim.ui.TrafficSimulatorView;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Main application class that extends javafx.application.Application.
 * This class creates and displays the primary stage with a BorderPane root.
 */
public class MainApp extends Application {

    private SimulationController controller;
    private ClockService clockService;
    private TrafficLightService trafficLightService;
    private SimulationState state;
    private TrafficSimulatorView view;
    
    // Auto-increment counters for IDs
    private AtomicInteger nextCarId = new AtomicInteger(1);
    private AtomicInteger nextIntersectionId = new AtomicInteger(1);
    
    // Settings
    private double renderScale = 1.0;
    private boolean pauseLightsOnPause = true;
    private long seed = 12345L; // Seed for deterministic scenario
    private String assumptions =
        "TRAFFIC SIMULATOR ASSUMPTIONS & LIMITATIONS\n\n" +
        "1. Geometry:\n" +
        "   - All cars travel on a horizontal line (Y = 0)\n" +
        "   - World coordinates: 0 to 1000 meters on X axis\n" +
        "   - Intersections are vertical lines at specific X positions\n\n" +
        "2. Car Movement:\n" +
        "   - Cars move at constant speed (m/s) when not stopped\n" +
        "   - No acceleration/deceleration - instant stopping ('stop on a dime')\n" +
        "   - Cars stop 5 meters before intersection on RED light\n" +
        "   - Cars pass through on GREEN or YELLOW without slowing\n\n" +
        "3. Traffic Lights:\n" +
        "   - Each intersection has independent light cycle\n" +
        "   - Default cycle: GREEN (10s) → YELLOW (2s) → RED (12s)\n" +
        "   - Durations configurable per intersection\n" +
        "   - Lights cycle continuously on background threads\n\n" +
        "4. Collision Detection:\n" +
        "   - No car-to-car collision detection\n" +
        "   - Cars can overlap if at same position\n" +
        "   - Only intersection/light collision is checked\n\n" +
        "5. Threading:\n" +
        "   - Clock updates at 1 Hz\n" +
        "   - Traffic lights cycle independently per intersection\n" +
        "   - Simulation physics updates at 20 Hz\n" +
        "   - Rendering occurs at ~60 FPS\n\n" +
        "6. Limitations:\n" +
        "   - No multi-lane support\n" +
        "   - No turning or intersection crossing logic\n" +
        "   - Cars simply progress to next intersection in order\n" +
        "   - No realistic deceleration before stopping";

    @Override
    public void start(Stage primaryStage) {
        // Create the simulation state
        state = new SimulationState();
        
        // Create the simulation controller
        controller = new SimulationController(state);
        
        // Create the clock service
        clockService = new ClockService();
        clockService.start();
        
        // Create the traffic light service
        trafficLightService = new TrafficLightService();
        trafficLightService.start();
        
        // Create the traffic simulator view (BorderPane layout)
        view = new TrafficSimulatorView();
        
        // Wire the controller to the view
        view.setController(controller);
        
        // Wire the clock service to the view
        view.setClockService(clockService);
        
        // Bind tables to observable lists
        view.getIntersectionsTable().setItems(state.getIntersections());
        view.getCarsTable().setItems(state.getCars());
        
        // Wire button handlers with proper lifecycle management
        view.getStartButton().setOnAction(e -> handleStart());
        view.getPauseButton().setOnAction(e -> handlePause());
        view.getStopButton().setOnAction(e -> handleStop());
        view.getContinueButton().setOnAction(e -> handleContinue());
        
        // Wire add car button to dialog
        view.getAddCarButton().setOnAction(e -> showAddCarDialog());
        
        // Wire add intersection button to dialog
        view.getAddIntersectionButton().setOnAction(e -> showAddIntersectionDialog());
        
        // Wire settings button to dialog
        view.getSettingsButton().setOnAction(e -> showSettingsDialog());
        
        // Create the scene with size 1000x700
        Scene scene = new Scene(view, 1000, 700);
        
        // Load CSS stylesheet
        String css = getClass().getResource("/styles.css").toExternalForm();
        scene.getStylesheets().add(css);
        
        // Configure the primary stage
        primaryStage.setTitle("Traffic Simulator");
        primaryStage.setScene(scene);
        primaryStage.setResizable(true);
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(600);
        primaryStage.show();
        
        // Stop everything when window closes
        primaryStage.setOnCloseRequest(event -> {
            controller.stop();
            clockService.stop();
            trafficLightService.stop();
            view.stopRenderTimer();
        });
    }

    /**
     * Handle Start button click.
     * Starts simulation controller (AnimationTimer is already running from view init).
     * Idempotent - safe to call multiple times.
     */
    private void handleStart() {
        // Start simulation controller (20 Hz tick)
        controller.start();
        System.out.println("START button clicked - Simulation started");
    }
    
    /**
     * Handle Pause button click.
     * Pauses simulation updates but keeps services running.
     * Optionally pauses traffic lights based on pauseLightsOnPause setting.
     * Idempotent - safe to call multiple times.
     */
    private void handlePause() {
        // Pause simulation controller
        controller.pause();
        
        // Pause traffic lights if setting is enabled
        if (pauseLightsOnPause) {
            trafficLightService.pause();
        }
        
        System.out.println("PAUSE button clicked - Simulation paused (lights " + 
                         (pauseLightsOnPause ? "paused" : "continue cycling") + ")");
    }
    
    /**
     * Handle Stop button click.
     * Stops simulation controller. Traffic lights continue cycling
     * (they are stopped only on window close).
     * Idempotent - safe to call multiple times.
     */
    private void handleStop() {
        // Stop simulation controller
        controller.stop();
        
        // Note: Traffic lights continue cycling even when stopped
        // This allows observing light behavior without car movement
        // Lights are only stopped when the window is closed
        
        System.out.println("STOP button clicked - Simulation stopped (lights continue)");
    }
    
    /**
     * Handle Continue button click.
     * Resumes simulation from paused state without resetting.
     * Resumes traffic lights if they were paused.
     * Idempotent - safe to call multiple times.
     */
    private void handleContinue() {
        // Continue simulation controller
        controller.continueSimulation();
        
        // Resume traffic lights if they were paused
        if (trafficLightService.isPaused()) {
            trafficLightService.resume();
        }
        
        System.out.println("CONTINUE button clicked - Simulation resumed");
    }

    /**
     * Show dialog to add a new car with auto-incrementing ID or user-specified.
     */
    private void showAddCarDialog() {
        Dialog<Car> dialog = new Dialog<>();
        dialog.setTitle("Add Car");
        dialog.setHeaderText("Add a new car to the simulation");
        
        // Create dialog pane
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        // ID field with auto-increment default
        TextField idField = new TextField(String.valueOf(nextCarId.get()));
        TextField xField = new TextField("50.0");
        TextField speedField = new TextField("15.0");
        
        grid.add(new Label("Car ID:"), 0, 0);
        grid.add(idField, 1, 0);
        grid.add(new Label("Starting X (m):"), 0, 1);
        grid.add(xField, 1, 1);
        grid.add(new Label("Speed (m/s):"), 0, 2);
        grid.add(speedField, 1, 2);
        
        dialog.getDialogPane().setContent(grid);
        
        // Add OK and Cancel buttons
        ButtonType okButtonType = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okButtonType, ButtonType.CANCEL);
        
        // Convert result to Car when OK is clicked
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == okButtonType) {
                try {
                    int id = Integer.parseInt(idField.getText());
                    double x = Double.parseDouble(xField.getText());
                    double speed = Double.parseDouble(speedField.getText());
                    
                    // Update next ID if using auto-increment
                    if (id >= nextCarId.get()) {
                        nextCarId.set(id + 1);
                    }
                    
                    return new Car(id, x, 0.0, speed);
                } catch (NumberFormatException e) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Invalid Input");
                    alert.setHeaderText("Invalid number format");
                    alert.setContentText("Please enter valid numbers for all fields.");
                    alert.showAndWait();
                    return null;
                }
            }
            return null;
        });
        
        // Show dialog and add car if OK was clicked
        Optional<Car> result = dialog.showAndWait();
        result.ifPresent(car -> {
            state.addCar(car);
            System.out.println("Added car: " + car);
        });
    }
    
    /**
     * Show dialog to add a new intersection with sorted insertion.
     */
    private void showAddIntersectionDialog() {
        Dialog<Intersection> dialog = new Dialog<>();
        dialog.setTitle("Add Intersection");
        dialog.setHeaderText("Add a new intersection to the simulation");
        
        // Create dialog pane
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        // ID field with auto-increment default
        TextField idField = new TextField(String.valueOf(nextIntersectionId.get()));
        TextField xField = new TextField("200.0");
        TextField greenField = new TextField("10");
        TextField yellowField = new TextField("2");
        TextField redField = new TextField("12");
        
        grid.add(new Label("Intersection ID:"), 0, 0);
        grid.add(idField, 1, 0);
        grid.add(new Label("Position X (m):"), 0, 1);
        grid.add(xField, 1, 1);
        grid.add(new Label("Green Duration (s):"), 0, 2);
        grid.add(greenField, 1, 2);
        grid.add(new Label("Yellow Duration (s):"), 0, 3);
        grid.add(yellowField, 1, 3);
        grid.add(new Label("Red Duration (s):"), 0, 4);
        grid.add(redField, 1, 4);
        
        dialog.getDialogPane().setContent(grid);
        
        // Add OK and Cancel buttons
        ButtonType okButtonType = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okButtonType, ButtonType.CANCEL);
        
        // Convert result to Intersection when OK is clicked
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == okButtonType) {
                try {
                    int id = Integer.parseInt(idField.getText());
                    double x = Double.parseDouble(xField.getText());
                    int greenSec = Integer.parseInt(greenField.getText());
                    int yellowSec = Integer.parseInt(yellowField.getText());
                    int redSec = Integer.parseInt(redField.getText());
                    
                    // Update next ID if using auto-increment
                    if (id >= nextIntersectionId.get()) {
                        nextIntersectionId.set(id + 1);
                    }
                    
                    Intersection intersection = new Intersection(id, x);
                    intersection.setGreenDuration(Duration.ofSeconds(greenSec));
                    intersection.setYellowDuration(Duration.ofSeconds(yellowSec));
                    intersection.setRedDuration(Duration.ofSeconds(redSec));
                    
                    return intersection;
                } catch (NumberFormatException e) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Invalid Input");
                    alert.setHeaderText("Invalid number format");
                    alert.setContentText("Please enter valid numbers for all fields.");
                    alert.showAndWait();
                    return null;
                }
            }
            return null;
        });
        
        // Show dialog and add intersection if OK was clicked
        Optional<Intersection> result = dialog.showAndWait();
        result.ifPresent(intersection -> {
            // Insert in sorted order by x position
            int insertIndex = 0;
            for (int i = 0; i < state.getIntersections().size(); i++) {
                if (state.getIntersections().get(i).getX() > intersection.getX()) {
                    break;
                }
                insertIndex = i + 1;
            }
            state.getIntersections().add(insertIndex, intersection);
            
            // Start traffic light cycle if simulation is running
            if (controller.isRunning() || trafficLightService.isRunning()) {
                trafficLightService.startLight(intersection);
            }
            
            System.out.println("Added intersection at position " + insertIndex + ": " + intersection);
        });
    }

    /**
     * Show settings dialog with configuration options and assumptions.
     */
    private void showSettingsDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Settings");
        dialog.setHeaderText("Simulation Settings & Assumptions");
        
        // Create tabs for organized settings
        TabPane tabPane = new TabPane();
        
        // === General Settings Tab ===
        Tab generalTab = new Tab("General");
        generalTab.setClosable(false);
        GridPane generalGrid = new GridPane();
        generalGrid.setHgap(10);
        generalGrid.setVgap(10);
        generalGrid.setPadding(new Insets(20));
        
        // Units (read-only)
        Label unitsLabel = new Label("meters/second (m/s)");
        unitsLabel.setStyle("-fx-text-fill: gray;");
        generalGrid.add(new Label("Units:"), 0, 0);
        generalGrid.add(unitsLabel, 1, 0);
        
        // Default intersection distance
        TextField distanceField = new TextField("1000");
        distanceField.setTooltip(new Tooltip("Default distance between intersections (meters). Per-intersection overrides available when adding."));
        generalGrid.add(new Label("Default Intersection Distance (m):"), 0, 1);
        generalGrid.add(distanceField, 1, 1);
        
        // Pause lights toggle
        CheckBox pauseLightsCheckBox = new CheckBox("Pause traffic lights when simulation is paused");
        pauseLightsCheckBox.setSelected(pauseLightsOnPause);
        generalGrid.add(pauseLightsCheckBox, 0, 2, 2, 1);
        
        // Seed field
        TextField seedField = new TextField(String.valueOf(seed));
        seedField.setTooltip(new Tooltip("Random seed for deterministic scenarios. Used for initial car positions and speeds."));
        generalGrid.add(new Label("Random Seed:"), 0, 3);
        generalGrid.add(seedField, 1, 3);
        
        // Load Demo button
        Button loadDemoButton = new Button("Load Demo Scenario");
        loadDemoButton.setTooltip(new Tooltip("Loads 3 intersections and 3 cars for testing"));
        loadDemoButton.setOnAction(e -> {
            loadDemoScenario();
            dialog.close();
        });
        generalGrid.add(loadDemoButton, 0, 4, 2, 1);
        
        // Render scale slider
        Label scaleLabel = new Label("Render Scale: " + String.format("%.1f", renderScale) + "x");
        Slider scaleSlider = new Slider(0.5, 2.0, renderScale);
        scaleSlider.setShowTickLabels(true);
        scaleSlider.setShowTickMarks(true);
        scaleSlider.setMajorTickUnit(0.5);
        scaleSlider.setMinorTickCount(4);
        scaleSlider.setBlockIncrement(0.1);
        scaleSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            scaleLabel.setText("Render Scale: " + String.format("%.1f", newVal.doubleValue()) + "x");
        });
        
        generalGrid.add(new Label("Render Scale/Zoom:"), 0, 5);
        generalGrid.add(scaleSlider, 1, 5);
        generalGrid.add(scaleLabel, 1, 6);
        
        generalTab.setContent(generalGrid);
        
        // === Assumptions Tab ===
        Tab assumptionsTab = new Tab("Assumptions & Limitations");
        assumptionsTab.setClosable(false);
        VBox assumptionsBox = new VBox(10);
        assumptionsBox.setPadding(new Insets(20));
        
        Label assumptionsLabel = new Label("Edit the simulation assumptions and limitations:");
        assumptionsLabel.setStyle("-fx-font-weight: bold;");
        
        TextArea assumptionsArea = new TextArea(assumptions);
        assumptionsArea.setWrapText(true);
        assumptionsArea.setPrefRowCount(20);
        assumptionsArea.setPrefColumnCount(60);
        
        assumptionsBox.getChildren().addAll(assumptionsLabel, assumptionsArea);
        assumptionsTab.setContent(assumptionsBox);
        
        // Add tabs to pane
        tabPane.getTabs().addAll(generalTab, assumptionsTab);
        
        dialog.getDialogPane().setContent(tabPane);
        dialog.getDialogPane().setPrefSize(600, 500);
        
        // Add OK and Cancel buttons
        ButtonType okButtonType = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okButtonType, ButtonType.CANCEL);
        
        // Show dialog and apply settings if OK was clicked
        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == okButtonType) {
            // Apply settings
            pauseLightsOnPause = pauseLightsCheckBox.isSelected();
            renderScale = scaleSlider.getValue();
            assumptions = assumptionsArea.getText();
            
            // Update seed
            try {
                seed = Long.parseLong(seedField.getText());
            } catch (NumberFormatException e) {
                System.err.println("Invalid seed value, keeping current: " + seed);
            }
            
            System.out.println("Settings updated:");
            System.out.println("  Pause lights on pause: " + pauseLightsOnPause);
            System.out.println("  Render scale: " + renderScale);
            System.out.println("  Seed: " + seed);
            System.out.println("Settings saved successfully");
        }
    }

    /**
     * Load a demo scenario with 3 intersections and 3 cars.
     * Uses the current seed for deterministic positioning.
     */
    private void loadDemoScenario() {
        // Clear existing entities
        state.getIntersections().clear();
        state.getCars().clear();
        
        // Stop existing light cycles
        // (Note: In production, we'd want to stop individual lights)
        
        // Create 3 intersections at fixed positions
        Intersection int1 = new Intersection(1, 200.0);
        int1.setGreenDuration(Duration.ofSeconds(10));
        int1.setYellowDuration(Duration.ofSeconds(2));
        int1.setRedDuration(Duration.ofSeconds(12));
        state.addIntersection(int1);
        
        Intersection int2 = new Intersection(2, 500.0);
        int2.setGreenDuration(Duration.ofSeconds(8));
        int2.setYellowDuration(Duration.ofSeconds(2));
        int2.setRedDuration(Duration.ofSeconds(10));
        state.addIntersection(int2);
        
        Intersection int3 = new Intersection(3, 800.0);
        int3.setGreenDuration(Duration.ofSeconds(12));
        int3.setYellowDuration(Duration.ofSeconds(3));
        int3.setRedDuration(Duration.ofSeconds(15));
        state.addIntersection(int3);
        
        // Start traffic lights for all intersections
        if (trafficLightService.isRunning()) {
            trafficLightService.startLight(int1);
            trafficLightService.startLight(int2);
            trafficLightService.startLight(int3);
        }
        
        // Create 3 cars with deterministic positions based on seed
        java.util.Random random = new java.util.Random(seed);
        
        Car car1 = new Car(1, 50.0 + random.nextDouble() * 50, 0.0, 10.0 + random.nextDouble() * 5);
        state.addCar(car1);
        
        Car car2 = new Car(2, 300.0 + random.nextDouble() * 50, 0.0, 12.0 + random.nextDouble() * 4);
        state.addCar(car2);
        
        Car car3 = new Car(3, 600.0 + random.nextDouble() * 50, 0.0, 8.0 + random.nextDouble() * 6);
        state.addCar(car3);
        
        // Update ID counters
        nextIntersectionId.set(4);
        nextCarId.set(4);
        
        System.out.println("Demo scenario loaded:");
        System.out.println("  3 intersections at x=200, 500, 800");
        System.out.println("  3 cars with speeds based on seed " + seed);
        
        Alert info = new Alert(Alert.AlertType.INFORMATION);
        info.setTitle("Demo Loaded");
        info.setHeaderText("Demo Scenario Loaded Successfully");
        info.setContentText("3 intersections and 3 cars have been added.\nClick START to begin the simulation.");
        info.showAndWait();
    }

    /**
     * Main method to launch the JavaFX application.
     * @param args command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }
}
