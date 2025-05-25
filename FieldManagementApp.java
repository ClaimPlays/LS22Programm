import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.*;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.stream.Collectors;

public class FieldManagementApp extends Application {
    private final ObservableList<String> fieldListData = FXCollections.observableArrayList();
    private final HashMap<String, String> fieldDetailsMap = new HashMap<>();
    private final File saveFile = new File("fields_data.txt");
    private final TilePane tilePane = new TilePane(); // Kachelansicht für Felder
    private String selectedField = null; // Speichert das aktuell ausgewählte Feld
    private VBox draggedBox = null; // Die aktuell gezogene Kachel

    // Fruchttypen auf Deutsch
    private final String[] cropTypes = {
            "Weizen", "Gerste", "Hafer", "Raps", "Sorghumhirse", "Sonnenblume", "Sojabohne", "Mais", "Kartoffel",
            "Zuckerrüben", "Rote Beete", "Karotte", "Pastinake", "Baumwolle", "Reis", "Grüne Bohne",
            "Erbse", "Spinat", "Zuckerrohr", "Traube", "Oliven", "Pappel"
    };

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Feld Manager - Farming Simulator 22");

        // Überschrift erstellen
        Label titleLabel = new Label("Felder Management by Agrar GBR A.N.D.P Software");
        titleLabel.getStyleClass().add("title-label"); // CSS für die Überschrift

        // Logo hinzufügen
        ImageView logoView = new ImageView();
        try {
            Image logo = new Image(new FileInputStream("images/logo.png")); // Logo-Pfad
            logoView.setImage(logo);
            logoView.setFitWidth(100);
            logoView.setFitHeight(100);
        } catch (FileNotFoundException e) {
            System.out.println("Logo-Datei nicht gefunden: images/logo.png");
        }

        // Erste Zeile der Überschrift
        Label titleLabelLine1 = new Label("Felder Management");
        // Zweite Zeile der Überschrift
        Label titleLabelLine2 = new Label("by Agrar GBR A.N.D.P Software");

        // CSS-Styling hinzufügen
        titleLabelLine1.getStyleClass().add("title-label");
        titleLabelLine2.getStyleClass().add("subtitle-label");

        // Beide Labels in ein VBox-Layout einfügen
        VBox titleBox = new VBox(5, titleLabelLine1, titleLabelLine2);

        // Überschrift und Logo in eine HBox einfügen
        HBox headerBox = new HBox(10, logoView, titleBox);
        headerBox.setPadding(new Insets(10));
        headerBox.getStyleClass().add("header-box"); // CSS für den Header

        // Load existing data from file
        loadFieldsFromFile();

        tilePane.setPadding(new Insets(10));
        tilePane.setHgap(10);
        tilePane.setVgap(10);
        tilePane.setPrefColumns(3); // Anzahl der Spalten in der Kachelansicht
        updateTilePane();

        Button createFieldButton = new Button("Feld erstellen");
        createFieldButton.setOnAction(e -> openCreateFieldWindow(primaryStage));

        Button editFieldButton = new Button("Feld bearbeiten");
        editFieldButton.setOnAction(e -> handleEditField(primaryStage, selectedField));

        Button deleteFieldButton = new Button("Feld löschen");
        deleteFieldButton.setOnAction(e -> handleDeleteField(selectedField));

        Button harvestedButton = new Button("Geerntet");
        harvestedButton.setOnAction(e -> handleHarvested(selectedField));

        HBox buttonBox = new HBox(10, createFieldButton, editFieldButton, deleteFieldButton, harvestedButton);
        buttonBox.setPadding(new Insets(10));
        buttonBox.getStyleClass().add("button-box");

        // Kachelansicht aktualisieren
        tilePane.setPadding(new Insets(10));
        tilePane.setHgap(10);
        tilePane.setVgap(10);
        tilePane.setPrefColumns(3); // Anzahl der Spalten in der Kachelansicht
        updateTilePane();

        // Hauptlayout erstellen
        BorderPane mainLayout = new BorderPane();
        mainLayout.setTop(headerBox); // Überschrift und Logo oben
        mainLayout.setCenter(tilePane); // Kachelansicht in der Mitte
        mainLayout.setBottom(buttonBox); // Buttons unten

        Scene scene = new Scene(mainLayout, 600, 400);
        scene.getStylesheets().add("style.css"); // CSS-Datei laden
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void updateTilePane() {
        tilePane.getChildren().clear();

        for (String field : fieldListData) {
            String details = fieldDetailsMap.get(field);

            // Parse the details for display
            String[] parts = details.split("\n");
            String fieldNumber = parts[0].split(": ")[1];
            String fieldSize = parts[1].split(": ")[1].replace("ha", "").trim(); // Entferne doppelte "ha"
            String currentCrop = parts[2].split(": ")[1];

            VBox fieldBox = new VBox(5);
            fieldBox.setPadding(new Insets(5)); // Kleinere Polsterung
            fieldBox.getStyleClass().add("field-box"); // CSS-Klasse hinzufügen

            Label fieldNumberLabel = new Label("Feld: " + fieldNumber);
            fieldNumberLabel.setStyle("-fx-font-size: 10;"); // Kleinere Schriftgröße
            Label fieldSizeLabel = new Label("Größe: " + fieldSize + " ha");
            fieldSizeLabel.setStyle("-fx-font-size: 10;"); // Kleinere Schriftgröße
            Label currentCropLabel = new Label(currentCrop);
            currentCropLabel.setStyle("-fx-font-size: 10;"); // Kleinere Schriftgröße

            // Add crop icon
            ImageView cropIcon = getCropIcon(currentCrop);
            cropIcon.setFitWidth(30); // Kleinere Breite des Icons
            cropIcon.setFitHeight(30); // Kleinere Höhe des Icons

            fieldBox.getChildren().addAll(fieldNumberLabel, fieldSizeLabel, cropIcon, currentCropLabel);

            // Add Drag-and-Drop functionality
            enableDragAndDrop(fieldBox, field);

            fieldBox.setOnMouseClicked(event -> handleFieldSelection(fieldBox, field));
            tilePane.getChildren().add(fieldBox);
        }
    }

    private void enableDragAndDrop(VBox fieldBox, String field) {
        fieldBox.setOnDragDetected(event -> {
            // Start Drag-and-Drop
            Dragboard dragboard = fieldBox.startDragAndDrop(TransferMode.MOVE);
            ClipboardContent content = new ClipboardContent();
            content.putString(field); // Set the field as drag content
            dragboard.setContent(content);
            event.consume();
        });

        fieldBox.setOnDragOver(event -> {
            // Allow Drag-and-Drop if the source is another VBox
            if (event.getGestureSource() != fieldBox && event.getDragboard().hasString()) {
                event.acceptTransferModes(TransferMode.MOVE);
            }
            event.consume();
        });

        fieldBox.setOnDragDropped(event -> {
            // Handle the drop action
            Dragboard dragboard = event.getDragboard();
            if (dragboard.hasString()) {
                String draggedField = dragboard.getString();

                // Update the order in fieldListData
                int draggedIndex = fieldListData.indexOf(draggedField);
                int targetIndex = fieldListData.indexOf(field);

                if (draggedIndex != -1 && targetIndex != -1) {
                    // Swap the positions
                    fieldListData.remove(draggedIndex);
                    fieldListData.add(targetIndex, draggedField);

                    // Refresh the TilePane
                    updateTilePane();
                }
                event.setDropCompleted(true);
            } else {
                event.setDropCompleted(false);
            }
            event.consume();
        });

        fieldBox.setOnDragDone(event -> {
            // Handle the end of the drag-and-drop gesture
            event.consume();
        });
    }

    private ImageView getCropIcon(String cropName) {
        try {
            // Absoluter Pfad zum Verzeichnis "images/"
            String basePath = System.getProperty("user.dir") + "/images/";
            String imagePath = basePath + cropName + ".png";
            File imageFile = new File(imagePath);

            System.out.println("Überprüfe Datei: " + imagePath);
            System.out.println("Existiert Datei? " + imageFile.exists());
            System.out.println("Ist es eine Datei? " + imageFile.isFile());

            if (!imageFile.exists()) {
                throw new FileNotFoundException("Bild nicht gefunden: " + imagePath);
            }

            Image image = new Image(new FileInputStream(imageFile));
            return new ImageView(image);
        } catch (FileNotFoundException e) {
            System.err.println("Fehler beim Laden des Icons für " + cropName + ": " + e.getMessage());
            // Fallback auf Standard-Icon
            try {
                String defaultImagePath = System.getProperty("user.dir") + "/images/default.png";
                File defaultImageFile = new File(defaultImagePath);

                System.out.println("Überprüfe Standard-Icon: " + defaultImagePath);
                System.out.println("Existiert Standard-Icon? " + defaultImageFile.exists());
                System.out.println("Ist es eine Datei? " + defaultImageFile.isFile());

                if (!defaultImageFile.exists()) {
                    throw new FileNotFoundException("Standard-Icon nicht gefunden: " + defaultImagePath);
                }

                Image defaultImage = new Image(new FileInputStream(defaultImageFile));
                return new ImageView(defaultImage);
            } catch (FileNotFoundException ex) {
                System.err.println("Standard-Icon konnte nicht geladen werden: " + ex.getMessage());
                // Gib ein leeres Platzhalter-Image zurück, falls kein Standard-Icon existiert
                return new ImageView();
            }
        }
    }

    private void handleFieldSelection(VBox fieldBox, String field) {
        // Entferne die Markierung von allen Kacheln
        tilePane.getChildren().stream()
                .filter(node -> node instanceof VBox)
                .map(node -> (VBox) node)
                .forEach(box -> box.setStyle("-fx-border-color: black; -fx-border-width: 1; -fx-background-color: #f0f0f0;"));

        // Markiere die ausgewählte Kachel
        fieldBox.setStyle("-fx-border-color: green; -fx-border-width: 2; -fx-background-color: #edd6c5;");
        selectedField = field; // Speichere das ausgewählte Feld
    }

    private void showFieldDetails(String field) {
        String details = fieldDetailsMap.get(field);
        if (details != null) {
            showAlert("Feld Details", details);
        }
    }

    private void openCreateFieldWindow(Stage owner) {
        Stage createFieldStage = new Stage();
        createFieldStage.setTitle("Feld erstellen");

        Label fieldNumberLabel = new Label("Feldnummer:");
        TextField fieldNumberField = new TextField();

        Label fieldSizeLabel = new Label("Feldgröße (ha):");
        TextField fieldSizeField = new TextField();

        Label currentCropLabel = new Label("Aktuelle Frucht:");
        ComboBox<String> currentCropCombo = new ComboBox<>(FXCollections.observableArrayList(cropTypes));

        Label followCropLabel = new Label("Folgefrüchte:");
        ArrayList<ComboBox<String>> followCropCombos = new ArrayList<>();
        VBox followCropBox = new VBox(5);
        for (int i = 0; i < 5; i++) {
            ComboBox<String> followCropCombo = new ComboBox<>(FXCollections.observableArrayList(cropTypes));
            followCropCombos.add(followCropCombo);
            followCropBox.getChildren().add(followCropCombo);
        }

        Button saveButton = new Button("Speichern");
        saveButton.setOnAction(e -> {
            String fieldNumber = fieldNumberField.getText();
            String fieldSize = fieldSizeField.getText();
            String currentCrop = currentCropCombo.getValue();

            if (fieldNumber == null || fieldNumber.isEmpty() || fieldSize == null || fieldSize.isEmpty() ||
                    currentCrop == null || currentCrop.isEmpty()) {
                showAlert("Fehler", "Bitte alle Felder ausfüllen!");
                return;
            }

            StringBuilder fullDetails = new StringBuilder("Feldnummer: " + fieldNumber + "\nFeldgröße: " + fieldSize + " ha\nAktuelle Frucht: " + currentCrop);
            String listEntryText = "Feld " + fieldNumber + ": " + currentCrop;

            for (ComboBox<String> followCropCombo : followCropCombos) {
                String followCrop = followCropCombo.getValue();
                if (followCrop != null && !followCrop.isEmpty()) {
                    fullDetails.append("\nFolgefrucht: ").append(followCrop);
                }
            }

            fieldListData.add(listEntryText);
            fieldDetailsMap.put(listEntryText, fullDetails.toString());

            // Save the data to file
            saveFieldsToFile();

            createFieldStage.close();
            updateTilePane(); // Refresh the tile view
        });

        VBox layout = new VBox(10,
                fieldNumberLabel, fieldNumberField,
                fieldSizeLabel, fieldSizeField,
                currentCropLabel, currentCropCombo,
                followCropLabel, followCropBox,
                saveButton
        );
        layout.setPadding(new Insets(10));

        Scene scene = new Scene(layout, 350, 450);
        createFieldStage.setScene(scene);
        createFieldStage.initOwner(owner);
        createFieldStage.show();
    }

    private void handleEditField(Stage owner, String selectedField) {
        if (selectedField == null) {
            showAlert("Fehler", "Bitte ein Feld auswählen!");
            return;
        }

        String fullDetails = fieldDetailsMap.get(selectedField);

        // Parse existing details
        String[] details = fullDetails.split("\n");
        String fieldNumber = details[0].split(": ")[1];
        String fieldSize = details[1].split(": ")[1];
        String currentCrop = details[2].split(": ")[1];
        ArrayList<String> followCrops = new ArrayList<>();
        for (int i = 3; i < details.length; i++) {
            followCrops.add(details[i].split(": ")[1]);
        }

        Stage editFieldStage = new Stage();
        editFieldStage.setTitle("Feld bearbeiten");

        Label fieldNumberLabel = new Label("Feldnummer:");
        TextField fieldNumberField = new TextField(fieldNumber);

        Label fieldSizeLabel = new Label("Feldgröße (ha):");
        TextField fieldSizeField = new TextField(fieldSize);

        Label currentCropLabel = new Label("Aktuelle Frucht:");
        ComboBox<String> currentCropCombo = new ComboBox<>(FXCollections.observableArrayList(cropTypes));
        currentCropCombo.setValue(currentCrop);

        Label followCropLabel = new Label("Folgefrüchte:");
        ArrayList<ComboBox<String>> followCropCombos = new ArrayList<>();
        VBox followCropBox = new VBox(5);
        for (int i = 0; i < 5; i++) {
            ComboBox<String> followCropCombo = new ComboBox<>(FXCollections.observableArrayList(cropTypes));
            if (i < followCrops.size()) {
                followCropCombo.setValue(followCrops.get(i));
            }
            followCropCombos.add(followCropCombo);
            followCropBox.getChildren().add(followCropCombo);
        }

        Button saveButton = new Button("Speichern");
        saveButton.setOnAction(e -> {
            String newFieldNumber = fieldNumberField.getText();
            String newFieldSize = fieldSizeField.getText();
            String newCurrentCrop = currentCropCombo.getValue();

            if (newFieldNumber == null || newFieldNumber.isEmpty() || newFieldSize == null || newFieldSize.isEmpty() || newCurrentCrop == null || newCurrentCrop.isEmpty()) {
                showAlert("Fehler", "Bitte alle Felder ausfüllen!");
                return;
            }

            StringBuilder newFullDetails = new StringBuilder("Feldnummer: " + newFieldNumber + "\nFeldgröße: " + newFieldSize + " ha\nAktuelle Frucht: " + newCurrentCrop);
            String newListEntryText = "Feld " + newFieldNumber + ": " + newCurrentCrop;

            for (ComboBox<String> followCropCombo : followCropCombos) {
                String followCrop = followCropCombo.getValue();
                if (followCrop != null && !followCrop.isEmpty()) {
                    newFullDetails.append("\nFolgefrucht: ").append(followCrop);
                }
            }

            fieldListData.remove(selectedField);
            fieldDetailsMap.remove(selectedField);

            fieldListData.add(newListEntryText);
            fieldDetailsMap.put(newListEntryText, newFullDetails.toString());

            // Save the data to file
            saveFieldsToFile();

            editFieldStage.close();
            updateTilePane(); // Refresh the tile view
        });

        VBox layout = new VBox(10,
                fieldNumberLabel, fieldNumberField,
                fieldSizeLabel, fieldSizeField,
                currentCropLabel, currentCropCombo,
                followCropLabel, followCropBox,
                saveButton
        );
        layout.setPadding(new Insets(10));

        Scene scene = new Scene(layout, 350, 450);
        editFieldStage.setScene(scene);
        editFieldStage.initOwner(owner);
        editFieldStage.show();
    }

    private void handleDeleteField(String selectedField) {
        if (selectedField == null) {
            showAlert("Fehler", "Bitte ein Feld auswählen!");
            return;
        }

        fieldListData.remove(selectedField);
        fieldDetailsMap.remove(selectedField);

        // Save to file
        saveFieldsToFile();
        updateTilePane(); // Aktualisiere die Kachelansicht
    }

    private void handleHarvested(String selectedField) {
        if (selectedField == null) {
            showAlert("Fehler", "Bitte ein Feld auswählen!");
            return;
        }

        String fullDetails = fieldDetailsMap.get(selectedField);
        String[] details = fullDetails.split("\n");

        String fieldNumber = details[0].split(": ")[1];
        String fieldSize = details[1].split(": ")[1];
        String currentCrop = details[2].split(": ")[1];
        ArrayList<String> followCrops = new ArrayList<>();
        for (int i = 3; i < details.length; i++) {
            followCrops.add(details[i].split(": ")[1]);
        }

        if (followCrops.isEmpty()) {
            showAlert("Info", "Keine weiteren Folgefrüchte verfügbar!");
            return;
        }

        String newCurrentCrop = followCrops.remove(0); // Setze die nächste Frucht
        StringBuilder newFullDetails = new StringBuilder("Feldnummer: " + fieldNumber + "\nFeldgröße: " + fieldSize + " ha\nAktuelle Frucht: " + newCurrentCrop);
        String newListEntryText = "Feld " + fieldNumber + ": " + newCurrentCrop;

        for (String followCrop : followCrops) {
            newFullDetails.append("\nFolgefrucht: ").append(followCrop);
        }

        fieldListData.remove(selectedField);
        fieldDetailsMap.remove(selectedField);

        fieldListData.add(newListEntryText);
        fieldDetailsMap.put(newListEntryText, newFullDetails.toString());

        // Save to file
        saveFieldsToFile();
        updateTilePane(); // Aktualisiere die Kachelansicht
    }

    private void saveFieldsToFile() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(saveFile))) {
            for (String field : fieldListData) {
                writer.write(fieldDetailsMap.get(field));
                writer.newLine();
                writer.newLine(); // Add a blank line between field entries
            }
        } catch (IOException e) {
            showAlert("Fehler", "Fehler beim Speichern der Daten: " + e.getMessage());
        }
    }

    private void loadFieldsFromFile() {
        if (!saveFile.exists()) return;

        try (BufferedReader reader = new BufferedReader(new FileReader(saveFile))) {
            StringBuilder fieldDetails = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty()) { // Blank line indicates the end of a field's details
                    if (fieldDetails.length() > 0) {
                        String details = fieldDetails.toString();
                        String[] parts = details.split("\n");
                        if (parts.length >= 3) {
                            String fieldNumber = parts[0].split(": ")[1];
                            String currentCrop = parts[2].split(": ")[1];
                            String listEntryText = "Feld " + fieldNumber + ": " + currentCrop;

                            fieldListData.add(listEntryText);
                            fieldDetailsMap.put(listEntryText, details);
                        }
                        fieldDetails.setLength(0); // Clear the StringBuilder for the next field
                    }
                } else {
                    fieldDetails.append(line).append("\n");
                }
            }
        } catch (IOException e) {
            showAlert("Fehler", "Fehler beim Laden der Daten: " + e.getMessage());
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}