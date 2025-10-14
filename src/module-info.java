module sim {
    // Require JavaFX modules
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    
    // Export the sim package
    exports sim;
    exports sim.model;
    exports sim.controller;
    exports sim.service;
    
    // Open sim.ui to javafx.fxml for reflection (needed for FXML)
    opens sim.ui to javafx.fxml;
}
