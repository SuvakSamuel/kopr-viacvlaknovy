package client;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;

import java.io.IOException;
import java.net.ConnectException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ClientAppController {

    @FXML
    private TextField sourceDirectoryTextField;

    @FXML
    private TextField destinationDirectoryTextField;

    @FXML
    private TextField numberOfSocketsTextField;

    @FXML
    private Button startStopButton;

    @FXML
    private Label statusLabel;

    private String sourceDir;
    private String destinationDir;
    private int socketCount;

    @FXML
    void initialize() {
        sourceDirectoryTextField.setText("dir\\");
        destinationDirectoryTextField.setText("C:\\Users\\suvac\\IdeaProjects\\kopr-projekt\\dir2\\");
        numberOfSocketsTextField.setText("2");
        statusLabel.setText("Čakám");
    }

    @FXML
    void run(ActionEvent event) throws IOException {
        String source = sourceDirectoryTextField.getText();
        String destination = destinationDirectoryTextField.getText();

        if(!Files.exists(Paths.get(source))) {
            statusLabel.setText("Zlyhalo");
            statusLabel.setTextFill(Color.TOMATO);
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setContentText("Zdrojový súbor " + source + "neexistuje." );
            alert.show();
            return;
        }

        if(!Files.exists(Paths.get(destination))) {
            statusLabel.setText("Zlyhalo");
            statusLabel.setTextFill(Color.TOMATO);
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setContentText("Cieľový súbor " + destination + "neexistuje." );
            alert.show();
            return;
        }

        if(sourceDirectoryTextField.getText().isEmpty()) {
            statusLabel.setText("Zlyhalo");
            statusLabel.setTextFill(Color.TOMATO);
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setContentText("Zdrojový súbor nesmie byť prázdny.");
            alert.show();
            return;
        }

        try {
            int numberOfSockets = Integer.parseInt(numberOfSocketsTextField.getText());
            if (numberOfSockets <= 0) {
                statusLabel.setText("Zlyhalo");
                statusLabel.setTextFill(Color.TOMATO);
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setContentText("Počet soketov musí byť kladné číslo.");
                alert.show();
                return;
            }
        } catch (NumberFormatException e) {
            statusLabel.setText("Zlyhalo");
            statusLabel.setTextFill(Color.TOMATO);
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setContentText("Nesprávna hodnota.");
            alert.show();
            return;
        }

        sourceDir = source;
        destinationDir = destination;
        socketCount = Integer.parseInt(numberOfSocketsTextField.getText());
        begin();
    }

    public void begin() throws IOException {
        statusLabel.setText("Spustené");
        statusLabel.setTextFill(Color.GREEN);
        System.out.println("Beginning transfer of " + sourceDir);
        ConnectionEstablisher connectionEstablisher = new ConnectionEstablisher(sourceDir, destinationDir, socketCount);
        try {
            connectionEstablisher.exchangeNecessaryInformation(sourceDir, destinationDir, socketCount);
            statusLabel.setText("Dokončené");
            statusLabel.setTextFill(Color.GREEN);
        } catch (ConnectException e) {
            statusLabel.setText("Zlyhalo");
            statusLabel.setTextFill(Color.TOMATO);
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setContentText("Server nie je dostupný. Reštartujte server.");
            alert.show();
        }
    }
}
