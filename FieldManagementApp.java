import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.scene.Cursor;
import javafx.stage.Stage;
import javafx.scene.input.MouseEvent;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import javafx.application.Platform;
import javafx.scene.chart.PieChart;

import java.awt.Desktop;
import java.io.*;
import java.net.URI;
import java.util.*;

public class FieldManagementApp extends Application {
    private final ObservableList<String> fieldListData = FXCollections.observableArrayList();
    private final HashMap<String, String> fieldDetailsMap = new HashMap<>();
    private final HashMap<String, List<String>> fieldTasksMap = new HashMap<>();
    private final HashMap<String, List<String>> fieldTasksOpenMap = new HashMap<>();
    private final HashMap<String, List<String>> fieldTasksDoneMap = new HashMap<>();
    private final File saveFile = new File("fields_data.txt");
    private final TilePane tilePane = new TilePane(); // Kachelansicht für Felder
    private String selectedField = null; // Speichert das aktuell ausgewählte Feld

    // Standard-Arbeitsschritte
    private static final List<String> STANDARD_TASKS = Arrays.asList(
            "Pflügen", "Gruppern" ,"Säen", "Düngen", "Spritzen"
    );

    // Fruchttypen auf Deutsch
    private final String[] cropTypes = {
            "Weizen", "Gerste", "Hafer", "Raps", "Sorghumhirse", "Sonnenblume", "Sojabohne", "Mais", "Kartoffel",
            "Zuckerrüben", "Rote Beete", "Karotte", "Pastinake", "Baumwolle", "Reis", "Grüne Bohne",
            "Erbse", "Spinat", "Zuckerrohr", "Traube", "Oliven", "Pappel"
    };

    private static final Map<String, List<String>> FRUCHTFOLGEN = new HashMap<>();
    static {
        FRUCHTFOLGEN.put("Weizen", Arrays.asList("Gerste", "Raps", "Mais", "Sojabohne", "Zuckerrüben"));
        FRUCHTFOLGEN.put("Gerste", Arrays.asList("Raps", "Weizen", "Mais", "Kartoffel", "Sonnenblume"));
        FRUCHTFOLGEN.put("Hafer", Arrays.asList("Luzerne", "Mais", "Raps", "Weizen", "Sonnenblume"));
        FRUCHTFOLGEN.put("Raps", Arrays.asList("Weizen", "Gerste", "Kartoffel", "Hafer", "Zuckerrüben"));
        FRUCHTFOLGEN.put("Sorghumhirse", Arrays.asList("Sojabohne", "Mais", "Sonnenblume", "Weizen", "Pappel"));
        FRUCHTFOLGEN.put("Sonnenblume", Arrays.asList("Weizen", "Hafer", "Sojabohne", "Mais", "Gerste"));
        FRUCHTFOLGEN.put("Sojabohne", Arrays.asList("Weizen", "Gerste", "Mais", "Sonnenblume", "Raps"));
        FRUCHTFOLGEN.put("Mais", Arrays.asList("Weizen", "Sojabohne", "Sonnenblume", "Raps", "Hafer"));
        FRUCHTFOLGEN.put("Kartoffel", Arrays.asList("Weizen", "Gerste", "Mais", "Sonnenblume", "Raps"));
        FRUCHTFOLGEN.put("Zuckerrüben", Arrays.asList("Weizen", "Hafer", "Mais", "Raps", "Sonnenblume"));
        FRUCHTFOLGEN.put("Rote Beete", Arrays.asList("Weizen", "Hafer", "Mais", "Raps", "Sonnenblume"));
        FRUCHTFOLGEN.put("Karotte", Arrays.asList("Weizen", "Mais", "Raps", "Hafer", "Sonnenblume"));
        FRUCHTFOLGEN.put("Pastinake", Arrays.asList("Weizen", "Mais", "Gerste", "Raps", "Hafer"));
        FRUCHTFOLGEN.put("Baumwolle", Arrays.asList("Mais", "Sojabohne", "Sonnenblume", "Weizen", "Gerste"));
        FRUCHTFOLGEN.put("Reis", Arrays.asList("Sojabohne", "Mais", "Raps", "Sonnenblume", "Weizen"));
        FRUCHTFOLGEN.put("Grüne Bohne", Arrays.asList("Weizen", "Hafer", "Mais", "Sonnenblume", "Raps"));
        FRUCHTFOLGEN.put("Spinat", Arrays.asList("Weizen", "Gerste", "Mais", "Hafer", "Raps"));
        FRUCHTFOLGEN.put("Zuckerrohr", Arrays.asList("Mais", "Sojabohne", "Weizen", "Raps", "Gerste"));
        FRUCHTFOLGEN.put("Traube", Arrays.asList("Weizen", "Hafer", "Mais", "Sonnenblume", "Raps"));
        FRUCHTFOLGEN.put("Oliven", Arrays.asList("Weizen", "Hafer", "Mais", "Raps", "Sonnenblume"));
        FRUCHTFOLGEN.put("Pappel", Arrays.asList("Weizen", "Gerste", "Mais", "Sonnenblume", "Raps"));
    }

    @Override
    public void start(Stage primaryStage) {
        // Setze das Taskleisten-Icon (WICHTIG: muss vor primaryStage.show() gesetzt werden!)
        try {
            primaryStage.getIcons().add(new Image(new FileInputStream("images/icon.png")));
        } catch (FileNotFoundException e) {
            System.out.println("Taskleisten-Icon nicht gefunden: images/icon.png");
        }

        Timeline reminderTimeline = new Timeline(
                new KeyFrame(Duration.minutes(60), e -> notifyIfTasksOpen())
        );
        reminderTimeline.setCycleCount(Timeline.INDEFINITE);
        reminderTimeline.play();

        notifyIfTasksOpen();

        primaryStage.setTitle("Feld Manager - Farming Simulator 22");

        // Überschrift erstellen
        Label titleLabelLine1 = new Label("Felder Management");
        Label titleLabelLine2 = new Label("by Kuhstall 2.0 Software");
        titleLabelLine1.getStyleClass().add("title-label");
        titleLabelLine2.getStyleClass().add("subtitle-label");
        VBox titleBox = new VBox(5, titleLabelLine1, titleLabelLine2);

        // Logo hinzufügen
        ImageView logoView = new ImageView();
        try {
            Image logo = new Image(new FileInputStream("images/logo.png"));
            logoView.setImage(logo);
            logoView.setFitWidth(100);
            logoView.setFitHeight(100);
        } catch (FileNotFoundException e) {
            System.out.println("Logo-Datei nicht gefunden: images/logo.png");
        }

        // ==== ICONS RECHTS IM HEADER ====
        ImageView discordIcon = new ImageView();
        try {
            Image discordImage = new Image(new FileInputStream("images/discord.png"));
            discordIcon.setImage(discordImage);
        } catch (FileNotFoundException e) {
            System.out.println("Discord-Icon nicht gefunden: images/discord.png");
        }
        discordIcon.setFitWidth(32);
        discordIcon.setFitHeight(32);
        discordIcon.setCursor(Cursor.HAND);
        discordIcon.getStyleClass().add("icon-button");

        ImageView spendeIcon = new ImageView();
        try {
            Image spendeImage = new Image(new FileInputStream("images/spende.png"));
            spendeIcon.setImage(spendeImage);
        } catch (FileNotFoundException e) {
            System.out.println("Spenden-Icon nicht gefunden: images/spende.png");
        }
        spendeIcon.setFitWidth(32);
        spendeIcon.setFitHeight(32);
        spendeIcon.setCursor(Cursor.HAND);
        spendeIcon.getStyleClass().add("icon-button");

        // Klick-Events für Icons
        discordIcon.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
            try {
                Desktop.getDesktop().browse(new URI("https://dsc.gg/DAF"));
            } catch (Exception ex) { ex.printStackTrace(); }
        });
        spendeIcon.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
            try {
                Desktop.getDesktop().browse(new URI("https://paypal.me/erkpay"));
            } catch (Exception ex) { ex.printStackTrace(); }
        });

        HBox iconBox = new HBox(12, discordIcon, spendeIcon);
        iconBox.setAlignment(Pos.TOP_RIGHT);
        iconBox.setPadding(new Insets(10, 18, 0, 0));

        // Header (links Logo + Titel, rechts Icons)
        BorderPane headerPane = new BorderPane();
        HBox leftHeader = new HBox(10, logoView, titleBox);
        leftHeader.setAlignment(Pos.CENTER_LEFT);
        headerPane.setLeft(leftHeader);
        headerPane.setRight(iconBox);
        headerPane.setPadding(new Insets(10, 10, 0, 10));
        headerPane.getStyleClass().add("header-box"); // CSS für den Header

        // Load existing data from file
        loadFieldsFromFile();

        tilePane.setPadding(new Insets(10));
        tilePane.setHgap(10);
        tilePane.setVgap(10);
        tilePane.setPrefColumns(3); // Anzahl der Spalten in der Kachelansicht
        updateTilePane();

        ScrollPane scrollPane = new ScrollPane(tilePane);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setPannable(true);

        Button createFieldButton = new Button("Feld erstellen");
        createFieldButton.getStyleClass().add("button");
        Button editFieldButton = new Button("Feld bearbeiten");
        editFieldButton.getStyleClass().add("button");
        Button deleteFieldButton = new Button("Feld löschen");
        deleteFieldButton.getStyleClass().addAll("button", "delete");
        Button harvestedButton = new Button("Geerntet");
        harvestedButton.getStyleClass().addAll("button", "harvested");

        // NEUER AUSWERTUNGS-BUTTON
        Button statsButton = new Button("Auswertung");
        statsButton.getStyleClass().add("button");
        statsButton.setOnAction(e -> showStatsChart()); // Methode, die das Diagramm öffnet

        createFieldButton.setOnAction(e -> openCreateFieldWindow(primaryStage));
        editFieldButton.setOnAction(e -> handleEditField(primaryStage, selectedField));
        deleteFieldButton.setOnAction(e -> handleDeleteField(selectedField));
        harvestedButton.setOnAction(e -> handleHarvested(selectedField));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox buttonBox = new HBox(10, spacer, createFieldButton, editFieldButton, deleteFieldButton, harvestedButton, statsButton);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        buttonBox.setPadding(new Insets(0, 18, 10, 0));
        buttonBox.getStyleClass().add("button-box");

        VBox topSection = new VBox();
        topSection.setSpacing(5);
        topSection.getChildren().addAll(headerPane, buttonBox);

        BorderPane mainLayout = new BorderPane();
        mainLayout.setTop(topSection);
        mainLayout.setCenter(scrollPane);

        Scene scene = new Scene(mainLayout, 760, 450);
        scene.getStylesheets().add("style.css"); // CSS-Datei laden
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    // ... Restlicher Code bleibt unverändert (updateTilePane, enableDragAndDrop, getCropIcon, handleFieldSelection usw.) ...

    // -- ab hier identisch wie bisher --
    private void showStatsChart() {
        int totalFields = fieldListData.size();
        int harvestedFields = 0;
        for (String field : fieldListData) {
            List<String> doneTasks = fieldTasksDoneMap.getOrDefault(field, new ArrayList<>());
            if (doneTasks.size() == STANDARD_TASKS.size()) {
                harvestedFields++;
            }
        }
        int openFields = totalFields - harvestedFields;

        PieChart.Data harvestedData = new PieChart.Data("Geerntet", harvestedFields);
        PieChart.Data openData = new PieChart.Data("Offen", openFields);
        PieChart pieChart = new PieChart(FXCollections.observableArrayList(harvestedData, openData));
        pieChart.setTitle("Feldstatus");
        pieChart.setLegendVisible(true);
        pieChart.setLabelsVisible(true);

        Label statsLabel = new Label("Geerntet: " + harvestedFields + " / Offene Arbeitsschritte: " + openFields + " / Gesamt: " + totalFields);
        statsLabel.setPadding(new Insets(10));

        VBox chartBox = new VBox(10, pieChart, statsLabel);
        chartBox.setPadding(new Insets(20));
        Stage chartStage = new Stage();
        chartStage.setTitle("Übersicht Felder (Grafik)");
        chartStage.setScene(new Scene(chartBox, 400, 350));
        chartStage.show();
    }

    private void notifyIfTasksOpen() {
        StringBuilder message = new StringBuilder();
        int totalOpen = 0;
        for (String field : fieldListData) {
            List<String> openTasks = fieldTasksOpenMap.getOrDefault(field, new ArrayList<>());
            if (!openTasks.isEmpty()) {
                message.append(field)
                        .append(": ")
                        .append(String.join(", ", openTasks))
                        .append("\n");
                totalOpen += openTasks.size();
            }
        }
        Platform.runLater(() -> {
            showAlert("Erinnerung: Offene Aufgaben",
                    "Folgende Felder haben noch offene Aufgaben:\n\n" + message.toString());
        });
    }

    private void updateTilePane() {
        tilePane.getChildren().clear();

        for (String field : fieldListData) {
            String details = fieldDetailsMap.get(field);

            // Parse the details for display
            String[] parts = details.split("\n");
            String fieldNumber = parts[0].split(": ")[1];
            String fieldSize = parts[1].split(": ")[1].replace("ha", "").trim();
            String currentCrop = parts[2].split(": ")[1];

            VBox fieldBox = new VBox(5);
            fieldBox.setPadding(new Insets(5));
            fieldBox.getStyleClass().add("field-box");

            Label fieldNumberLabel = new Label("Feld: " + fieldNumber);
            fieldNumberLabel.setStyle("-fx-font-size: 10;");
            Label fieldSizeLabel = new Label("Größe: " + fieldSize + " ha");
            fieldSizeLabel.setStyle("-fx-font-size: 10;");
            Label currentCropLabel = new Label(currentCrop);
            currentCropLabel.setStyle("-fx-font-size: 10;");

            ImageView cropIcon = getCropIcon(currentCrop);
            cropIcon.setFitWidth(30);
            cropIcon.setFitHeight(30);

            fieldBox.getChildren().addAll(fieldNumberLabel, fieldSizeLabel, cropIcon, currentCropLabel);

            BorderPane fieldPane = new BorderPane();
            VBox infoBox = new VBox(5, fieldNumberLabel, fieldSizeLabel, cropIcon, currentCropLabel);
            infoBox.setPadding(new Insets(0, 10, 0, 0));

            VBox tasksVBox = new VBox(5);
            tasksVBox.setPadding(new Insets(0, 0, 0, 10));
            List<String> openTasks = fieldTasksOpenMap.getOrDefault(field, new ArrayList<>(STANDARD_TASKS));
            List<String> doneTasks = fieldTasksDoneMap.getOrDefault(field, new ArrayList<>());
            for (String task : STANDARD_TASKS) {
                CheckBox cb = new CheckBox(task);
                if (doneTasks.contains(task)) {
                    cb.setSelected(true);
                    cb.setDisable(true);
                } else {
                    cb.setSelected(false);
                    cb.setDisable(false);
                }
                cb.setOnAction(e -> {
                    if (cb.isSelected()) {
                        openTasks.remove(task);
                        doneTasks.add(task);
                        fieldTasksOpenMap.put(field, new ArrayList<>(openTasks));
                        fieldTasksDoneMap.put(field, new ArrayList<>(doneTasks));
                        saveFieldsToFile();
                        updateTilePane();
                    }
                });
                tasksVBox.getChildren().add(cb);
            }
            if (doneTasks.size() == STANDARD_TASKS.size()) {
                Label doneLabel = new Label("Alle Arbeitsschritte erledigt!");
                doneLabel.setStyle("-fx-text-fill: green; -fx-font-size: 10;");
                tasksVBox.getChildren().add(doneLabel);
            }

            fieldPane.setLeft(infoBox);
            fieldPane.setRight(tasksVBox);
            fieldTasksOpenMap.put(field, new ArrayList<>(openTasks));
            fieldTasksDoneMap.put(field, new ArrayList<>(doneTasks));
            fieldBox.getChildren().add(fieldPane);

            enableDragAndDrop(fieldBox, field);

            fieldBox.setOnMouseClicked(event -> {
                handleFieldSelection(fieldBox, field);
                if (event.getClickCount() == 2) {
                    showFieldDetails(field);
                }
            });
            tilePane.getChildren().add(fieldBox);
        }
    }

    private void enableDragAndDrop(VBox fieldBox, String field) {
        fieldBox.setOnDragDetected(event -> {
            Dragboard dragboard = fieldBox.startDragAndDrop(TransferMode.MOVE);
            ClipboardContent content = new ClipboardContent();
            content.putString(field);
            dragboard.setContent(content);
            event.consume();
        });

        fieldBox.setOnDragOver(event -> {
            if (event.getGestureSource() != fieldBox && event.getDragboard().hasString()) {
                event.acceptTransferModes(TransferMode.MOVE);
            }
            event.consume();
        });

        fieldBox.setOnDragDropped(event -> {
            Dragboard dragboard = event.getDragboard();
            if (dragboard.hasString()) {
                String draggedField = dragboard.getString();
                int draggedIndex = fieldListData.indexOf(draggedField);
                int targetIndex = fieldListData.indexOf(field);
                if (draggedIndex != -1 && targetIndex != -1) {
                    fieldListData.remove(draggedIndex);
                    fieldListData.add(targetIndex, draggedField);
                    updateTilePane();
                }
                event.setDropCompleted(true);
            } else {
                event.setDropCompleted(false);
            }
            event.consume();
        });

        fieldBox.setOnDragDone(event -> {
            event.consume();
        });
    }

    private ImageView getCropIcon(String cropName) {
        try {
            String basePath = System.getProperty("user.dir") + "/images/";
            String imagePath = basePath + cropName + ".png";
            File imageFile = new File(imagePath);

            if (!imageFile.exists()) {
                throw new FileNotFoundException("Bild nicht gefunden: " + imagePath);
            }

            Image image = new Image(new FileInputStream(imageFile));
            return new ImageView(image);
        } catch (FileNotFoundException e) {
            try {
                String defaultImagePath = System.getProperty("user.dir") + "/images/default.png";
                File defaultImageFile = new File(defaultImagePath);
                if (!defaultImageFile.exists()) {
                    throw new FileNotFoundException("Standard-Icon nicht gefunden: " + defaultImagePath);
                }
                Image defaultImage = new Image(new FileInputStream(defaultImageFile));
                return new ImageView(defaultImage);
            } catch (FileNotFoundException ex) {
                return new ImageView();
            }
        }
    }

    private void handleFieldSelection(VBox fieldBox, String field) {
        tilePane.getChildren().stream()
                .filter(node -> node instanceof VBox)
                .map(node -> (VBox) node)
                .forEach(box -> box.setStyle("-fx-border-color: black; -fx-border-width: 1; -fx-background-color: #f0f0f0;"));

        fieldBox.setStyle("-fx-border-color: green; -fx-border-width: 2; -fx-background-color: #edd6c5;");
        selectedField = field;
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

        currentCropCombo.setOnAction(e -> {
            String selected = currentCropCombo.getValue();
            List<String> folgen = FRUCHTFOLGEN.getOrDefault(selected, new ArrayList<>());
            for (int i = 0; i < followCropCombos.size(); i++) {
                if (i < folgen.size()) {
                    followCropCombos.get(i).setValue(folgen.get(i));
                } else {
                    followCropCombos.get(i).setValue(null);
                }
            }
        });

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
            fieldTasksMap.put(listEntryText, new ArrayList<>(STANDARD_TASKS));
            fieldTasksOpenMap.put(listEntryText, new ArrayList<>(STANDARD_TASKS));
            fieldTasksDoneMap.put(listEntryText, new ArrayList<>());

            saveFieldsToFile();

            createFieldStage.close();
            updateTilePane();
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
            fieldTasksMap.remove(selectedField);

            fieldListData.add(newListEntryText);
            fieldDetailsMap.put(newListEntryText, newFullDetails.toString());
            fieldTasksMap.put(newListEntryText, new ArrayList<>(STANDARD_TASKS));

            saveFieldsToFile();

            editFieldStage.close();
            updateTilePane();
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
        fieldTasksMap.remove(selectedField);

        saveFieldsToFile();
        updateTilePane();
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
        ArrayList<String> followCrops = new ArrayList<>();
        for (int i = 3; i < details.length; i++) {
            followCrops.add(details[i].split(": ")[1]);
        }

        if (followCrops.isEmpty()) {
            showAlert("Info", "Keine weiteren Folgefrüchte verfügbar!");
            return;
        }

        String newCurrentCrop = followCrops.remove(0);
        StringBuilder newFullDetails = new StringBuilder("Feldnummer: " + fieldNumber + "\nFeldgröße: " + fieldSize + " ha\nAktuelle Frucht: " + newCurrentCrop);
        String newListEntryText = "Feld " + fieldNumber + ": " + newCurrentCrop;

        for (String followCrop : followCrops) {
            newFullDetails.append("\nFolgefrucht: ").append(followCrop);
        }

        fieldListData.remove(selectedField);
        fieldDetailsMap.remove(selectedField);
        fieldTasksMap.remove(selectedField);

        fieldListData.add(newListEntryText);
        fieldDetailsMap.put(newListEntryText, newFullDetails.toString());
        fieldTasksMap.put(newListEntryText, new ArrayList<>(STANDARD_TASKS));

        saveFieldsToFile();
        updateTilePane();
    }

    private void saveFieldsToFile() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(saveFile))) {
            for (String field : fieldListData) {
                writer.write(fieldDetailsMap.get(field));
                writer.newLine();
                List<String> openTasks = fieldTasksOpenMap.getOrDefault(field, new ArrayList<>());
                List<String> doneTasks = fieldTasksDoneMap.getOrDefault(field, new ArrayList<>());
                writer.write("TasksOpen: " + String.join("|", openTasks));
                writer.newLine();
                writer.write("TasksDone: " + String.join("|", doneTasks));
                writer.newLine();
                writer.newLine();
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
            List<String> openTasks = new ArrayList<>(STANDARD_TASKS);
            List<String> doneTasks = new ArrayList<>();
            String listEntryText = "";
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("TasksOpen: ")) {
                    String openLine = line.substring(10).trim();
                    openTasks = openLine.isEmpty() ? new ArrayList<>() : new ArrayList<>(Arrays.asList(openLine.split("\\|")));
                } else if (line.startsWith("TasksDone: ")) {
                    String doneLine = line.substring(11).trim();
                    doneTasks = doneLine.isEmpty() ? new ArrayList<>() : new ArrayList<>(Arrays.asList(doneLine.split("\\|")));
                } else if (line.isEmpty()) {
                    if (fieldDetails.length() > 0) {
                        String details = fieldDetails.toString();
                        String[] parts = details.split("\n");
                        if (parts.length >= 3) {
                            String fieldNumber = parts[0].split(": ")[1];
                            String currentCrop = parts[2].split(": ")[1];
                            listEntryText = "Feld " + fieldNumber + ": " + currentCrop;

                            fieldListData.add(listEntryText);
                            fieldDetailsMap.put(listEntryText, details.trim());
                            fieldTasksOpenMap.put(listEntryText, new ArrayList<>(openTasks));
                            fieldTasksDoneMap.put(listEntryText, new ArrayList<>(doneTasks));
                        }
                        fieldDetails.setLength(0);
                        openTasks = new ArrayList<>(STANDARD_TASKS);
                        doneTasks = new ArrayList<>();
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