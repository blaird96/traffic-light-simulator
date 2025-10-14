# Traffic Simulator - Developer Guide

## Table of Contents
1. [Prerequisites](#prerequisites)
2. [Build & Run](#build--run)
3. [Project Structure](#project-structure)
4. [Threading Architecture](#threading-architecture)
5. [User Controls](#user-controls)
6. [Adding Cars & Intersections](#adding-cars--intersections)
7. [Testing](#testing)
8. [Known Limitations](#known-limitations)
9. [Attribution](#attribution)

---

## Prerequisites

### Required Software
- **Java Development Kit (JDK) 17+**
  - Download: [Oracle JDK](https://www.oracle.com/java/technologies/downloads/) or [OpenJDK](https://adoptium.net/)
  - Verify: `java -version` should show 17 or higher

- **Apache Maven 3.6+** (if using Maven)
  - Download: [Maven Downloads](https://maven.apache.org/download.cgi)
  - Verify: `mvn -version`

- **Gradle 7.0+** (if using Gradle)
  - Download: [Gradle Downloads](https://gradle.org/install/)
  - Verify: `gradle -version`

### Optional Tools
- **PlantUML** (for diagram generation)
  - Install: `apt install plantuml` (Linux) or `brew install plantuml` (macOS)
  - Used for architecture diagrams

### Platform Support
- **Linux** (tested on Ubuntu 20.04+)
- **Windows** (Windows 10/11)
- **macOS** (macOS 10.14+)

---

## Build & Run

### Using Maven

#### Compile
```bash
mvn clean compile
```

#### Run Application
```bash
mvn javafx:run
```

#### Run Tests
```bash
mvn test
```

#### Package JAR
```bash
mvn package
```
Produces: `target/javafx-modular-app-1.0-SNAPSHOT.jar`

#### Clean Build
```bash
mvn clean install
```

### Using Gradle

**Note**: This project is primarily configured for Maven. To use Gradle, you would need to create a `build.gradle` file:

```gradle
plugins {
    id 'java'
    id 'application'
    id 'org.openjfx.javafxplugin' version '0.0.13'
}

group = 'com.example'
version = '1.0-SNAPSHOT'
sourceCompatibility = '17'

repositories {
    mavenCentral()
}

dependencies {
    implementation 'org.openjfx:javafx-controls:21.0.1'
    implementation 'org.openjfx:javafx-fxml:21.0.1'
    implementation 'org.openjfx:javafx-graphics:21.0.1'
    
    testImplementation 'org.junit.jupiter:junit-jupiter:5.10.1'
}

javafx {
    version = '21.0.1'
    modules = ['javafx.controls', 'javafx.fxml', 'javafx.graphics']
}

application {
    mainModule = 'sim'
    mainClass = 'sim.MainApp'
}

test {
    useJUnitPlatform()
}
```

Then run with:
```bash
gradle run          # Run application
gradle test         # Run tests
gradle build        # Build project
```

---

## Project Structure

```
Week_8_Concurrent_Stacks/
├── pom.xml                          # Maven configuration
├── DEV_GUIDE.md                     # This file
├── README.md                        # Project overview
├── RUN_INSTRUCTIONS.md              # Quick start guide
├── TRAFFIC_SIMULATOR_GUIDE.md       # User guide
│
├── docs/
│   ├── class-diagram.puml           # PlantUML source
│   └── Traffic Simulator Class Diagram.png
│
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   ├── module-info.java     # Java module descriptor
│   │   │   └── sim/
│   │   │       ├── MainApp.java     # Application entry point
│   │   │       │
│   │   │       ├── model/           # Domain models
│   │   │       │   ├── Car.java
│   │   │       │   ├── Intersection.java
│   │   │       │   ├── SimulationState.java
│   │   │       │   └── TrafficLightColor.java
│   │   │       │
│   │   │       ├── controller/      # Simulation orchestration
│   │   │       │   └── SimulationController.java
│   │   │       │
│   │   │       ├── service/         # Background services
│   │   │       │   ├── ClockService.java
│   │   │       │   ├── TrafficLightService.java
│   │   │       │   └── CarService.java
│   │   │       │
│   │   │       └── ui/              # User interface
│   │   │           ├── TrafficSimulatorView.java
│   │   │           └── SampleController.java
│   │   │
│   │   └── resources/
│   │       └── styles.css           # UI styling
│   │
│   └── test/
│       └── java/
│           └── sim/
│               └── test/
│                   └── TrafficSimulatorTest.java
│
└── target/                          # Build output (generated)
    ├── classes/
    └── test-classes/
```

### Package Organization

- **sim** - Main application and entry point
- **sim.model** - Domain objects (Car, Intersection, State)
- **sim.controller** - Simulation orchestration
- **sim.service** - Background thread services
- **sim.ui** - JavaFX user interface components
- **sim.test** - JUnit test cases

---

## Threading Architecture

The simulator uses a multi-threaded architecture with careful synchronization to avoid race conditions.

### Thread Types

#### 1. JavaFX Application Thread (Main UI Thread)
**Purpose**: UI updates and user interactions

**Components**:
- All JavaFX UI controls
- Event handlers (button clicks)
- Property bindings

**Critical Rule**: Only this thread can modify UI components

#### 2. ClockService Thread (1 Hz)
**Implementation**: `ScheduledExecutorService` with daemon thread

```java
executor = Executors.newScheduledThreadPool(1, r -> {
    Thread t = new Thread(r, "ClockServiceThread");
    t.setDaemon(true);
    return t;
});

executor.scheduleAtFixedRate(
    this::updateTime,
    0, 1, TimeUnit.SECONDS  // 1 Hz
);
```

**Updates**: `Platform.runLater(() -> currentTime.set(...))`

#### 3. Traffic Light Service Threads (Per-Intersection)
**Implementation**: Independent `ScheduledExecutorService` per intersection

```java
ScheduledFuture<?> task = executor.scheduleAtFixedRate(() -> {
    cycleLight(intersection);
}, 0, 100, TimeUnit.MILLISECONDS);
```

**Cycle**: GREEN → YELLOW → RED (configurable durations)

**Pause Behavior**: 
- Controlled by `pauseLightsOnPause` setting (default: true)
- When paused, light cycle threads freeze at current color
- Wake every 100ms to check pause state
- Resume timing when unpaused

**Updates**: `Platform.runLater(() -> intersection.setColor(...))`

#### 4. Simulation Controller Thread (20 Hz)
**Implementation**: `ScheduledExecutorService` for physics updates

```java
executor.scheduleAtFixedRate(() -> {
    if (!paused.get()) {
        carService.tick(TICK_INTERVAL_SECONDS);
    }
}, 0, 50, TimeUnit.MILLISECONDS);  // 50ms = 20 Hz
```

**Thread Safety**: Uses `ReentrantReadWriteLock`
- Write lock for updates
- Read lock for rendering

#### 5. Rendering Thread (60 FPS)
**Implementation**: JavaFX `AnimationTimer`

```java
renderTimer = new AnimationTimer() {
    @Override
    public void handle(long now) {
        renderWorld();  // ~60 FPS
    }
};
```

**Access**: Acquires read lock for consistent state snapshot

### Platform.runLater Pattern

**Purpose**: Marshal updates from background threads to JavaFX thread

**Usage Example**:
```java
// Background thread
double newX = calculatePosition();

// Update UI property
Platform.runLater(() -> {
    car.setX(newX);  // Safe on FX thread
});
```

**Critical for**: Any JavaFX Property updates from non-FX threads

### Thread Synchronization

**SimulationController uses ReentrantReadWriteLock**:

```java
private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

// Writer (physics update)
lock.writeLock().lock();
try {
    carService.tick(deltaTime);
} finally {
    lock.writeLock().unlock();
}

// Reader (rendering)
lock.readLock().lock();
try {
    renderCars();
    renderIntersections();
} finally {
    lock.readLock().unlock();
}
```

**Benefits**:
- Multiple readers can access simultaneously
- Writers get exclusive access
- No race conditions

### Executor Lifecycle Management

**Startup** (in `start()` methods):
```java
if (running.compareAndSet(false, true)) {
    executor = Executors.newScheduledThreadPool(1);
    // Schedule tasks...
}
```

**Shutdown** (in `stop()` methods):
```java
if (running.compareAndSet(true, false)) {
    executor.shutdown();
    try {
        executor.awaitTermination(2, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
        executor.shutdownNow();
    }
}
```

**Idempotent**: Safe to call multiple times (using `AtomicBoolean`)

---

## User Controls

### Top Control Bar

| Button | Function | State Changes |
|--------|----------|---------------|
| **Start** | Begin simulation | Starts 20 Hz physics updates |
| **Pause** | Freeze car movement | Pauses car updates; lights pause/continue based on setting |
| **Stop** | Stop simulation | Stops executor, resets tick counter; lights continue cycling |
| **Continue** | Resume from pause | Resumes physics from paused state; resumes lights if paused |
| **Settings** | Open settings dialog | Configure pause behavior, seed, and other options |

### Clock Display
- **Location**: Top right corner
- **Format**: HH:MM:SS (24-hour)
- **Update Rate**: 1 Hz
- **Independence**: Runs separately from simulation

### Tables

#### Intersections Table
- **Columns**: ID, X Position, Light Color
- **Live Updates**: Colors change in real-time
- **Sorting**: Click column headers

#### Cars Table
- **Columns**: ID, X Position, Y Position, Speed (m/s)
- **Live Updates**: Positions update during simulation
- **Sorting**: Click column headers

### World View (Center Pane)
- **Visual Elements**:
  - Grid lines every 100m
  - Ruler with distance markers (0m, 200m, 400m...)
  - Dark road with yellow center line
  - Intersections as vertical white lines
  - Traffic lights as colored circles (red/yellow/green)
  - Cars as cyan rectangles with ID labels

- **Scale**: 0-1000 meters → pane width

---

## Settings Configuration

### General Settings Tab

**Access**: Click "Settings" button → "General" tab

**Available Settings**:

1. **Pause Traffic Lights When Simulation is Paused** (Checkbox)
   - **Default**: Enabled (checked)
   - **When Enabled**: Both cars AND traffic lights freeze during pause
   - **When Disabled**: Only cars freeze; lights continue cycling independently
   - **Use Case**: Disable to observe light behavior while simulation is paused

2. **Random Seed** (Number Field)
   - **Default**: 12345
   - **Purpose**: Determines initial car positions and speeds in demo scenario
   - **Usage**: Change seed for different starting configurations

3. **Default Intersection Distance** (Number Field)
   - **Default**: 1000 meters
   - **Note**: Currently informational; per-intersection overrides available when adding

4. **Render Scale/Zoom** (Slider)
   - **Range**: 0.5x to 2.0x
   - **Default**: 1.0x
   - **Note**: Currently informational; affects display scaling

5. **Load Demo Scenario** (Button)
   - Creates 3 intersections and 3 cars
   - Uses current seed value for deterministic positioning
   - See "Load Demo Scenario" section below for details

### Assumptions & Limitations Tab

**Access**: Click "Settings" button → "Assumptions & Limitations" tab

**Content**: Editable text area containing simulation assumptions and limitations. This documentation is included in the application for reference.

---

## Adding Cars & Intersections

### Add Car Dialog

**Access**: Click "Add Car" button

**Fields**:
1. **Car ID**: Auto-increments (can override)
2. **Starting X (m)**: Initial position
3. **Speed (m/s)**: Constant velocity

**Defaults**:
- ID: Next available (1, 2, 3...)
- X: 50.0m
- Speed: 15.0 m/s

**Process**:
1. Enter values (or use defaults)
2. Click OK
3. Car appears in table and world view
4. If simulation running, car starts moving immediately

### Add Intersection Dialog

**Access**: Click "Add Intersection" button

**Fields**:
1. **Intersection ID**: Auto-increments (can override)
2. **Position X (m)**: Location on road
3. **Green Duration (s)**: How long green light lasts
4. **Yellow Duration (s)**: How long yellow light lasts
5. **Red Duration (s)**: How long red light lasts

**Defaults**:
- ID: Next available (1, 2, 3...)
- X: 200.0m
- Green: 10s
- Yellow: 2s
- Red: 12s

**Process**:
1. Enter values (or use defaults)
2. Click OK
3. Intersection inserted in sorted order by X position
4. Traffic light starts cycling automatically
5. Appears in table with live color updates

### Load Demo Scenario

**Access**: Settings → General Tab → "Load Demo" button

**Creates**:
- 3 intersections at x = 200m, 500m, 800m
- 3 cars with seed-based positioning
- Custom durations per intersection

**Usage**:
1. Open Settings
2. Optionally change seed value
3. Click "Load Demo"
4. Click OK
5. Click "Start" to begin simulation

**Perfect for**: Quick testing and rubric requirements

---

## Testing

### Running Tests

```bash
# Run all tests
mvn test

# Run specific test
mvn test -Dtest=TrafficSimulatorTest

# With debug output
mvn test -X
```

### Test Suite Overview

**Location**: `src/test/java/sim/test/TrafficSimulatorTest.java`

**Tests** (7 total):

1. **testRedLightHaltsMovement**
   - Verifies cars stop 5m before RED lights
   - Confirms "stop on a dime" behavior

2. **testYellowLightAllowsMovement**
   - Ensures cars pass through YELLOW lights
   - No stopping at yellow

3. **testGreenLightAllowsMovement**
   - Confirms cars pass through GREEN lights
   - Continuous movement

4. **testClockTicksAtOneSecond**
   - Validates 1 Hz clock update rate
   - Time changes after ~2 seconds

5. **testPauseStopsMovement** ✅
   - Ensures pause freezes car positions
   - Position unchanged while paused
   - **Status**: PASSING

6. **testCarMovementCalculation**
   - Validates physics: distance = speed × time
   - 20 m/s × 0.1s = 2 meters

7. **testSimulationStateMaintainsEntities** ✅
   - Tests state management
   - Add/remove operations
   - **Status**: PASSING

### Known Test Issues

**JavaFX Toolkit Not Initialized**: 4 tests fail in headless Maven environment

**Affected Tests**:
- testRedLightHaltsMovement
- testYellowLightAllowsMovement
- testGreenLightAllowsMovement
- testCarMovementCalculation

**Root Cause**: `Platform.runLater()` requires JavaFX toolkit

**Solutions**:

1. **TestFX Framework** (Recommended):
```xml
<dependency>
    <groupId>org.testfx</groupId>
    <artifactId>testfx-junit5</artifactId>
    <version>4.0.17</version>
    <scope>test</scope>
</dependency>
```

2. **Headless Mode**:
```java
@BeforeAll
public static void initToolkit() {
    Platform.startup(() -> {});
}
```

3. **Xvfb** (Linux):
```bash
xvfb-run mvn test
```

### Manual Testing

**Recommended Test Scenarios**:

1. **Basic Flow**:
   - Load demo → Start → Observe cars stopping at red lights
   - Verify yellow/green pass-through

2. **Control Flow**:
   - Start → Pause → Verify cars freeze
   - Continue → Verify cars resume
   - Stop → Verify complete halt

3. **Entity Addition**:
   - Add car while running → Verify immediate inclusion
   - Add intersection → Verify sorted insertion & light start

4. **Edge Cases**:
   - Multiple cars at same intersection
   - Car faster than light cycle
   - Overlapping cars (no collision = expected)

---

## Known Limitations

### Geometry Constraints
1. **Single Dimension**: Cars only move on horizontal line (Y=0)
2. **No Lanes**: All cars on same track, can overlap
3. **No Turning**: Cars can't change direction
4. **Fixed World**: 0-1000 meter coordinate space

### Physics Model
1. **Instant Stopping**: No deceleration ("stop on a dime")
2. **Constant Speed**: No acceleration
3. **Fixed Threshold**: Always stop 5m before intersection
4. **Simple Progression**: Cars just move to next intersection in list

### Collision Detection
1. **No Car-to-Car**: Cars can overlap without collision
2. **Only Intersection**: Only checks traffic light state
3. **Binary Decision**: Stop or go, no gradual approach

### Traffic Light Logic
1. **No Yellow Caution**: Cars don't slow for yellow
2. **Independent Cycles**: No synchronized traffic flow
3. **No Turn Signals**: Since no turning exists
4. **Fixed Cycle**: GREEN → YELLOW → RED (no adaptive timing)

### Threading
1. **Fixed Rates**: 1 Hz (clock), 20 Hz (physics), 60 FPS (render)
2. **No Dynamic Adjustment**: Rates don't adapt to load
3. **Platform.runLater**: Queued updates may lag under heavy load

### Testing
1. **Headless Mode**: JavaFX tests require display or TestFX
2. **Timing Sensitive**: Clock tests can be flaky
3. **No Integration Tests**: Only unit tests provided

### UI/UX
1. **No Zoom**: Render scale slider is placeholder
2. **No Pan**: Fixed view of 0-1000m
3. **No Car Colors**: All cars cyan
4. **No Intersection Labels**: Only ID shown

### Scalability
1. **ObservableLists**: Can be slow with 100+ entities
2. **Stateless Rendering**: Rebuilds entire scene each frame
3. **No Spatial Indexing**: O(n) collision checks

---

## Attribution

### Development Information

**Course**: CMSC 335 - Object-Oriented and Concurrent Programming  
**Institution**: University of Maryland Global Campus (UMGC)  
**Semester**: Fall 2025  
**Student**: Laird, Brendan M.

### AI Assistance Acknowledgment

Per UMGC Academic Policy, this project acknowledges the use of AI assistance:

**AI Tool Used**: Cline (Claude-based AI coding assistant)  
**Assistance Provided**:
- Architecture design and threading patterns
- Documentation generation (this guide)
- Code review and optimization suggestions
- Troubleshooting assistance and advisement.

**Student Contribution**:
- Project requirements definition
- Project structure and initial code developement
- Design decisions and trade-offs
- Code verification and testing
- Understanding and explaining all implementations
- Integration and final assembly
- Quality assurance and bug fixes

**Compliance Statement**: All AI-generated code has been reviewed, understood, and verified by the student. The student takes full responsibility for the correctness and academic integrity of the submission.

### Technology Stack

- **Language**: Java 17
- **UI Framework**: JavaFX 21.0.1
- **Build Tool**: Apache Maven 3.8+
- **Testing**: JUnit 5.10.1
- **Module System**: Java Platform Module System (JPMS)
- **Concurrency**: java.util.concurrent (ExecutorService, ScheduledExecutorService)
- **Synchronization**: ReentrantReadWriteLock, AtomicBoolean, AtomicInteger
- **Property Binding**: JavaFX Properties (IntegerProperty, DoubleProperty, etc.)

### References

1. **JavaFX Documentation**  
   https://openjfx.io/javadoc/21/

2. **Java Concurrency Tutorial**  
   https://docs.oracle.com/javase/tutorial/essential/concurrency/

3. **Maven Documentation**  
   https://maven.apache.org/guides/

4. **JUnit 5 User Guide**  
   https://junit.org/junit5/docs/current/user-guide/

5. **PlantUML Documentation**  
   https://plantuml.com/class-diagram

### License

This is an academic project developed for educational purposes.  
Not licensed for commercial use or redistribution.

---

## Quick Reference

### Common Commands

```bash
# Development
mvn clean compile              # Compile code
mvn javafx:run                 # Run application
mvn test                       # Run tests
mvn package                    # Create JAR

# Diagrams
plantuml docs/class-diagram.puml -tpng

# Cleanup
mvn clean                      # Remove target/
rm -rf target/                 # Alternative cleanup
```

### Important Files

| File | Purpose |
|------|---------|
| `pom.xml` | Maven configuration |
| `module-info.java` | Java module descriptor |
| `MainApp.java` | Application entry point |
| `SimulationController.java` | Core orchestration |
| `TrafficSimulatorView.java` | Main UI |
| `TrafficSimulatorTest.java` | Test suite |

### Key Directories

| Directory | Contents |
|-----------|----------|
| `src/main/java/sim/` | Source code |
| `src/main/resources/` | CSS, assets |
| `src/test/java/sim/test/` | JUnit tests |
| `docs/` | Diagrams, documentation |
| `target/` | Build output |

---

**Last Updated**: October 3, 2025  
**Version**: 1.0
