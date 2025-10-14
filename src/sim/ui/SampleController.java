package sim.ui;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

/**
 * Sample controller class for FXML demonstration.
 * This class is in the sim.ui package which is opened to javafx.fxml in module-info.java.
 */
public class SampleController {

    @FXML
    private Label messageLabel;

    @FXML
    public void initialize() {
        // Initialization logic if needed
    }

    public void setMessage(String message) {
        if (messageLabel != null) {
            messageLabel.setText(message);
        }
    }
}
