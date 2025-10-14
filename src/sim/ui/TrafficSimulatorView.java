package sim.ui;

import javafx.animation.AnimationTimer;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import sim.controller.SimulationController;
import sim.model.Car;
import sim.model.Intersection;
import sim.model.TrafficLightColor;
import sim.service.ClockService;

import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Main view for the traffic simulator with BorderPane layout.
 */
public class TrafficSimulatorView extends BorderPane {
    
    // Top controls
    private Button startButton;
    private Button pauseButton;
    private Button stopButton;
    private Button continueButton;
    private Button settingsButton;
    private Label clockLabel;
    
    // Center - World pane and tables
    private Pane worldPane;
    private TableView<Intersection> intersectionsTable;
    private TableView<Car> carsTable;
    
    // Bottom controls
    private TextField carIdField;
    private TextField carXField;
    private TextField carYField;
    private TextField carSpeedField;
    private Button addCarButton;
    
    private TextField intersectionIdField;
    private TextField intersectionXField;
    private TextField intersectionYField;
    private Button addIntersectionButton;
    
    private AnimationTimer renderTimer;
    private SimulationController controller;
    private ClockService clockService;
    
    public TrafficSimulatorView() {
        initializeComponents();
        layoutComponents();
    }
    
    private void initializeComponents() {
        // Top controls
        startButton = new Button("Start");
        pauseButton = new Button("Pause");
        stopButton = new Button("Stop");
        continueButton = new Button("Continue");
        settingsButton = new Button("Settings");
        clockLabel = new Label("--:--:--");
        
        startButton.getStyleClass().add("control-button");
        pauseButton.getStyleClass().add("control-button");
        stopButton.getStyleClass().add("control-button");
        continueButton.getStyleClass().add("control-button");
        settingsButton.getStyleClass().add("control-button");
        clockLabel.getStyleClass().add("clock-label");
        
        // Center - World pane
        worldPane = new Pane();
        worldPane.setStyle("-fx-background-color: #2a2a2a;");
        worldPane.setPrefSize(600, 600);
        worldPane.setMinSize(400, 400);
        
        // Center - Intersections table
        intersectionsTable = createIntersectionsTable();
        
        // Center - Cars table
        carsTable = createCarsTable();
        
        // Bottom controls - Car inputs
        carIdField = new TextField();
        carIdField.setPromptText("Car ID");
        carIdField.setPrefWidth(80);
        
        carXField = new TextField();
        carXField.setPromptText("X");
        carXField.setPrefWidth(60);
        
        carYField = new TextField();
        carYField.setPromptText("Y");
        carYField.setPrefWidth(60);
        
        carSpeedField = new TextField();
        carSpeedField.setPromptText("Speed");
        carSpeedField.setPrefWidth(60);
        
        addCarButton = new Button("Add Car");
        addCarButton.getStyleClass().add("add-button");
        
        // Bottom controls - Intersection inputs
        intersectionIdField = new TextField();
        intersectionIdField.setPromptText("Intersection ID");
        intersectionIdField.setPrefWidth(120);
        
        intersectionXField = new TextField();
        intersectionXField.setPromptText("X");
        intersectionXField.setPrefWidth(60);
        
        intersectionYField = new TextField();
        intersectionYField.setPromptText("Y");
        intersectionYField.setPrefWidth(60);
        
        addIntersectionButton = new Button("Add Intersection");
        addIntersectionButton.getStyleClass().add("add-button");
    }
    
    private TableView<Intersection> createIntersectionsTable() {
        TableView<Intersection> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        
        TableColumn<Intersection, Number> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(cellData -> cellData.getValue().idProperty());
        idCol.setPrefWidth(60);
        
        TableColumn<Intersection, Number> xCol = new TableColumn<>("X");
        xCol.setCellValueFactory(cellData -> cellData.getValue().xProperty());
        xCol.setPrefWidth(80);
        
        TableColumn<Intersection, TrafficLightColor> lightColorCol = new TableColumn<>("Light Color");
        lightColorCol.setCellValueFactory(cellData -> cellData.getValue().colorProperty());
        lightColorCol.setPrefWidth(100);
        
        table.getColumns().addAll(idCol, xCol, lightColorCol);
        return table;
    }
    
    private TableView<Car> createCarsTable() {
        TableView<Car> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        
        TableColumn<Car, Number> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(cellData -> cellData.getValue().idProperty());
        idCol.setPrefWidth(60);
        
        TableColumn<Car, Number> xCol = new TableColumn<>("X");
        xCol.setCellValueFactory(cellData -> cellData.getValue().xProperty());
        xCol.setPrefWidth(70);
        
        TableColumn<Car, Number> yCol = new TableColumn<>("Y");
        yCol.setCellValueFactory(cellData -> cellData.getValue().yProperty());
        yCol.setPrefWidth(70);
        
        TableColumn<Car, Number> speedCol = new TableColumn<>("Speed (m/s)");
        speedCol.setCellValueFactory(cellData -> cellData.getValue().speedMpsProperty());
        speedCol.setPrefWidth(90);
        
        table.getColumns().addAll(idCol, xCol, yCol, speedCol);
        return table;
    }
    
    private void layoutComponents() {
        // Top: HBox with controls and clock
        HBox topBox = new HBox(10);
        topBox.setPadding(new Insets(10));
        topBox.getStyleClass().add("top-controls");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        topBox.getChildren().addAll(
            startButton, pauseButton, stopButton, continueButton, settingsButton,
            spacer, clockLabel
        );
        setTop(topBox);
        
        // Center: SplitPane with world pane and tables
        SplitPane centerSplit = new SplitPane();
        centerSplit.setOrientation(Orientation.HORIZONTAL);
        centerSplit.setDividerPositions(0.6);
        
        // Left side: World pane
        StackPane worldContainer = new StackPane(worldPane);
        worldContainer.setPadding(new Insets(5));
        
        // Right side: VBox with two tables
        VBox tablesBox = new VBox(10);
        tablesBox.setPadding(new Insets(5));
        
        Label intersectionsLabel = new Label("Intersections");
        intersectionsLabel.getStyleClass().add("table-label");
        
        Label carsLabel = new Label("Cars");
        carsLabel.getStyleClass().add("table-label");
        
        VBox.setVgrow(intersectionsTable, Priority.ALWAYS);
        VBox.setVgrow(carsTable, Priority.ALWAYS);
        
        tablesBox.getChildren().addAll(
            intersectionsLabel, intersectionsTable,
            carsLabel, carsTable
        );
        
        centerSplit.getItems().addAll(worldContainer, tablesBox);
        setCenter(centerSplit);
        
        // Bottom: HBox with input controls
        HBox bottomBox = new HBox(15);
        bottomBox.setPadding(new Insets(10));
        bottomBox.getStyleClass().add("bottom-controls");
        
        // Car input section
        HBox carInputs = new HBox(5);
        carInputs.getChildren().addAll(
            new Label("Car:"), carIdField, carXField, carYField, carSpeedField, addCarButton
        );
        
        Separator separator = new Separator(Orientation.VERTICAL);
        
        // Intersection input section
        HBox intersectionInputs = new HBox(5);
        intersectionInputs.getChildren().addAll(
            new Label("Intersection:"), intersectionIdField, 
            intersectionXField, intersectionYField, addIntersectionButton
        );
        
        bottomBox.getChildren().addAll(carInputs, separator, intersectionInputs);
        setBottom(bottomBox);
    }
    
    /**
     * Set the clock service and bind the clock label to it.
     * @param clockService The clock service
     */
    public void setClockService(ClockService clockService) {
        this.clockService = clockService;
        // Bind the clock label text to the clock service's time property
        clockLabel.textProperty().bind(clockService.currentTimeProperty());
    }
    
    // Getters for components
    public Button getStartButton() { return startButton; }
    public Button getPauseButton() { return pauseButton; }
    public Button getStopButton() { return stopButton; }
    public Button getContinueButton() { return continueButton; }
    public Button getSettingsButton() { return settingsButton; }
    public Label getClockLabel() { return clockLabel; }
    public Pane getWorldPane() { return worldPane; }
    public TableView<Intersection> getIntersectionsTable() { return intersectionsTable; }
    public TableView<Car> getCarsTable() { return carsTable; }
    public TextField getCarIdField() { return carIdField; }
    public TextField getCarXField() { return carXField; }
    public TextField getCarYField() { return carYField; }
    public TextField getCarSpeedField() { return carSpeedField; }
    public Button getAddCarButton() { return addCarButton; }
    public TextField getIntersectionIdField() { return intersectionIdField; }
    public TextField getIntersectionXField() { return intersectionXField; }
    public TextField getIntersectionYField() { return intersectionYField; }
    public Button getAddIntersectionButton() { return addIntersectionButton; }
    
    /**
     * Set the simulation controller and start the render timer.
     * @param controller The simulation controller
     */
    public void setController(SimulationController controller) {
        this.controller = controller;
        startRenderTimer();
    }
    
    /**
     * Start the rendering AnimationTimer that runs at ~60 FPS.
     * Renders the current simulation state to the WorldPane.
     */
    private void startRenderTimer() {
        renderTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                renderWorld();
            }
        };
        renderTimer.start();
    }
    
    /**
     * Render the simulation world to the WorldPane.
     * Acquires a read lock to ensure consistent state during rendering.
     * Runs on JavaFX Application Thread at ~60 FPS.
     * 
     * Renders:
     * - Axis/ruler for reference
     * - Intersections as vertical lines
     * - Traffic lights as colored circles
     * - Cars as rectangles
     */
    private void renderWorld() {
        if (controller == null) {
            return;
        }
        
        // Get read lock for safe concurrent access
        ReentrantReadWriteLock.ReadLock readLock = controller.getReadLock();
        readLock.lock();
        try {
            // Clear the world pane (stateless rendering)
            worldPane.getChildren().clear();
            
            // Get pane dimensions
            double paneWidth = worldPane.getWidth();
            double paneHeight = worldPane.getHeight();
            
            // Calculate scale: world coordinates (0-1000 meters) to pane pixels
            double scale = paneWidth / 1000.0;
            double centerY = paneHeight / 2.0;
            
            // Render reference grid and ruler
            renderRuler(paneWidth, paneHeight, scale, centerY);
            
            // Render road line
            renderRoad(paneWidth, centerY);
            
            // Render intersections with traffic lights
            for (Intersection intersection : controller.getState().getIntersections()) {
                renderIntersection(intersection, scale, centerY);
            }
            
            // Render cars
            for (Car car : controller.getState().getCars()) {
                renderCar(car, scale, centerY);
            }
            
        } finally {
            readLock.unlock();
        }
    }
    
    /**
     * Render ruler marks and grid lines for reference.
     */
    private void renderRuler(double paneWidth, double paneHeight, double scale, double centerY) {
        // Draw grid lines every 100 meters
        for (int x = 0; x <= 1000; x += 100) {
            double pixelX = x * scale;
            
            // Vertical grid line
            javafx.scene.shape.Line gridLine = new javafx.scene.shape.Line(
                pixelX, 0, pixelX, paneHeight
            );
            gridLine.setStroke(Color.rgb(50, 50, 50));
            gridLine.setStrokeWidth(0.5);
            gridLine.getStrokeDashArray().addAll(5.0, 5.0);
            worldPane.getChildren().add(gridLine);
            
            // Ruler mark at top
            javafx.scene.shape.Line tick = new javafx.scene.shape.Line(
                pixelX, 0, pixelX, 10
            );
            tick.setStroke(Color.LIGHTGRAY);
            tick.setStrokeWidth(2);
            worldPane.getChildren().add(tick);
            
            // Label
            if (x % 200 == 0) {
                javafx.scene.text.Text label = new javafx.scene.text.Text(
                    pixelX - 10, 25, x + "m"
                );
                label.setFill(Color.LIGHTGRAY);
                label.setFont(javafx.scene.text.Font.font(10));
                worldPane.getChildren().add(label);
            }
        }
        
        // Draw horizontal center line
        javafx.scene.shape.Line centerLine = new javafx.scene.shape.Line(
            0, centerY, paneWidth, centerY
        );
        centerLine.setStroke(Color.rgb(60, 60, 60));
        centerLine.setStrokeWidth(1);
        worldPane.getChildren().add(centerLine);
    }
    
    /**
     * Render the road as a horizontal strip.
     */
    private void renderRoad(double paneWidth, double centerY) {
        Rectangle road = new Rectangle(0, centerY - 20, paneWidth, 40);
        road.setFill(Color.rgb(40, 40, 40));
        road.setStroke(Color.rgb(80, 80, 80));
        road.setStrokeWidth(1);
        worldPane.getChildren().add(road);
        
        // Road center dashed line
        javafx.scene.shape.Line centerDash = new javafx.scene.shape.Line(
            0, centerY, paneWidth, centerY
        );
        centerDash.setStroke(Color.YELLOW);
        centerDash.setStrokeWidth(2);
        centerDash.getStrokeDashArray().addAll(20.0, 10.0);
        worldPane.getChildren().add(centerDash);
    }
    
    /**
     * Render a single intersection as a vertical line with traffic light.
     * 
     * @param intersection The intersection to render
     * @param scale Pixels per meter
     * @param centerY Center Y coordinate of pane
     */
    private void renderIntersection(Intersection intersection, double scale, double centerY) {
        double x = intersection.getX() * scale;
        
        // Draw vertical line at intersection
        javafx.scene.shape.Line intersectionLine = new javafx.scene.shape.Line(
            x, centerY - 30, x, centerY + 30
        );
        intersectionLine.setStroke(Color.WHITE);
        intersectionLine.setStrokeWidth(3);
        worldPane.getChildren().add(intersectionLine);
        
        // Draw traffic light circle above the road
        Circle lightCircle = new Circle(x, centerY - 40, 8);
        
        // Set color based on traffic light state
        TrafficLightColor lightColor = intersection.getColor();
        switch (lightColor) {
            case RED:
                lightCircle.setFill(Color.RED);
                break;
            case YELLOW:
                lightCircle.setFill(Color.YELLOW);
                break;
            case GREEN:
                lightCircle.setFill(Color.LIMEGREEN);
                break;
        }
        
        lightCircle.setStroke(Color.BLACK);
        lightCircle.setStrokeWidth(2);
        worldPane.getChildren().add(lightCircle);
        
        // Draw intersection ID label
        javafx.scene.text.Text idLabel = new javafx.scene.text.Text(
            x - 5, centerY + 50, String.valueOf(intersection.getId())
        );
        idLabel.setFill(Color.WHITE);
        idLabel.setFont(javafx.scene.text.Font.font("Monospace", 10));
        worldPane.getChildren().add(idLabel);
    }
    
    /**
     * Render a single car as a rectangle on the road.
     * 
     * @param car The car to render
     * @param scale Pixels per meter
     * @param centerY Center Y coordinate of pane
     */
    private void renderCar(Car car, double scale, double centerY) {
        double x = car.getX() * scale;
        
        // Draw car as a rectangle on the road (y=0 in world space)
        Rectangle carRect = new Rectangle(x - 15, centerY - 6, 30, 12);
        carRect.setFill(Color.CYAN);
        carRect.setStroke(Color.DARKBLUE);
        carRect.setStrokeWidth(2);
        carRect.setArcWidth(4);
        carRect.setArcHeight(4);
        worldPane.getChildren().add(carRect);
        
        // Add car ID label
        javafx.scene.text.Text carIdLabel = new javafx.scene.text.Text(
            x - 5, centerY, String.valueOf(car.getId())
        );
        carIdLabel.setFill(Color.BLACK);
        carIdLabel.setFont(javafx.scene.text.Font.font("Monospace", javafx.scene.text.FontWeight.BOLD, 9));
        worldPane.getChildren().add(carIdLabel);
    }
    
    /**
     * Stop the render timer.
     */
    public void stopRenderTimer() {
        if (renderTimer != null) {
            renderTimer.stop();
        }
    }
}
