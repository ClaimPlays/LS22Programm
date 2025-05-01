import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.table.DefaultTableModel;
import java.awt.Desktop;
import java.net.URI;

public class TabbedWindow {
    private JTabbedPane tabbedPane;
    private ArrayList<Field> fields;
    private JTable fieldTable;
    private DefaultTableModel tableModel;
    private JTable workTable; // Neue Tabelle für Arbeitsschritte
    private DefaultTableModel workTableModel; // Model für die neue Tabelle
    private Map<String, List<String>> cropRotations; // HashMap für Fruchtfolgen
    private JComboBox<String> filterDropdown; // Dropdown für Filter
    private List<String> arbeitsschritte; // Liste von Arbeitsschritten für Dropdown

    // Lohnaufträge
    private ArrayList<Lohnauftrag> lohnaufträge; // Liste der Lohnaufträge
    private DefaultTableModel lohnAufträgeModel; // Model für Lohnaufträge
    private JTable lohnAufträgeTable; // Tabelle für Lohnaufträge
    private DefaultTableModel dauerAufträgeModel; // Model für Daueraufträge
    private JTable dauerAufträgeTable; // Tabelle für Daueraufträge

    // Neue Variablen für Einstellungen
    private String selectedFont;
    private String selectedColor;
    private boolean remindersEnabled;
    private boolean realisticCropRotationEnabled; // Neue Einstellung für realistische Fruchtfolge

    // Neue Variablen für den Kalender
    private List<String> fruits; // Liste der Fruchtsorten
    private Map<String, String[]> fruitTiming; // HashMap für Saat- und Erntezeiten

    public static void main(String[] args) {
        SwingUtilities.invokeLater(TabbedWindow::new);
    }

    public TabbedWindow() {
        // Set modern look and feel
        setLookAndFeel();
        
        JFrame frame = createMainFrame();

        tabbedPane = new JTabbedPane();
        customizeTabbedPane();

        // Initialize fields
        fields = new ArrayList<>();
        cropRotations = new HashMap<>();
        fruits = new ArrayList<>();
        fruitTiming = new HashMap<>();
        arbeitsschritte = new ArrayList<>();
        lohnaufträge = new ArrayList<>();

        // Create Tabs
        createFieldsTab();
        createWorkTab();
        createLohnarbeitTab();
        createSettingsTab();
        createInfoTab();
        createCalendarTab();

        frame.add(tabbedPane, BorderLayout.CENTER);
        frame.setVisible(true);
    }

    private void setLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private JFrame createMainFrame() {
        JFrame frame = new JFrame("Sim-Feldmanager by Claim_IZ");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);
        frame.setLocationRelativeTo(null); // Center the window
        frame.getContentPane().setBackground(new Color(240, 240, 240)); // Sanfter Grauer Hintergrund
        return frame;
    }

    private void createFieldsTab() {
        JPanel fieldsPanel = new JPanel(new BorderLayout());
        fieldsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        fieldsPanel.setBackground(Color.WHITE);

        String[] columnNames = {"Feld-Nr", "Feld Größe (ha)", "Aktuelle Frucht", "Geplante Frucht"};
        tableModel = new DefaultTableModel(columnNames, 0);
        fieldTable = new JTable(tableModel);
        fieldTable.setFillsViewportHeight(true);
        fieldTable.setBackground(new Color(240, 240, 240));
        fieldTable.setForeground(Color.BLACK);
        fieldTable.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        JScrollPane tableScrollPane = new JScrollPane(fieldTable);
        tableScrollPane.setPreferredSize(new Dimension(800, 200));

        JPanel buttonPanel = createFieldButtonPanel();
        fieldsPanel.add(buttonPanel, BorderLayout.NORTH);
        fieldsPanel.add(tableScrollPane, BorderLayout.CENTER);

        tabbedPane.addTab("Felder", fieldsPanel);
    }

    private JPanel createFieldButtonPanel() {
        JPanel buttonPanel = new JPanel();
        buttonPanel.setBackground(Color.WHITE); 

        CustomButton addFieldButton = createButton("Feld hinzufügen", "resources/icons/add.png", e -> openFieldCreationDialog());
        CustomButton editFieldButton = createButton("Feld bearbeiten", "resources/icons/edit.png", e -> editSelectedField());
        CustomButton deleteFieldButton = createButton("Feld löschen", "resources/icons/delete.png", e -> deleteSelectedField());

        buttonPanel.add(addFieldButton);
        buttonPanel.add(editFieldButton);
        buttonPanel.add(deleteFieldButton);
        return buttonPanel;
    }

    private void createWorkTab() {
        JPanel workPanel = new JPanel(new BorderLayout());
        workPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        workPanel.setBackground(Color.WHITE);
        tabbedPane.addTab("Arbeit", workPanel);

        String[] workColumnNames = {"Feld-Nr", "Arbeitsschritt", "Status"};
        workTableModel = new DefaultTableModel(workColumnNames, 0);
        workTable = new JTable(workTableModel);
        workTable.setFillsViewportHeight(true);
        workTable.setBackground(new Color(240, 240, 240));
        workTable.setForeground(Color.BLACK);
        workTable.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        JScrollPane workTableScrollPane = new JScrollPane(workTable);
        workTableScrollPane.setPreferredSize(new Dimension(320, 200));

        JPanel filterPanel = new JPanel();
        filterDropdown = new JComboBox<>(new String[]{
                "Alle", "Kalken", "Mulchen", "Pflügen", "Walzen", "Unkraut Striegel", "Spritzen", "Steine entfernen", "Düngen"
        });
        filterDropdown.addActionListener(e -> updateWorkTable());
        filterPanel.add(new JLabel("Filter: "));
        filterPanel.add(filterDropdown);

        workPanel.add(filterPanel, BorderLayout.NORTH);
        workPanel.add(workTableScrollPane, BorderLayout.CENTER);

        CustomButton markDoneButton = createButton("Arbeitsschritt als erledigt markieren", "resources/icons/done.png", e -> markWorkStepAsDone());
        workPanel.add(markDoneButton, BorderLayout.SOUTH);

        loadInitialData();
        updateWorkTable(); // Populate work steps table
    }

    private void loadInitialData() {
        loadCropRotationsFromFile();
        loadFruitsFromFile();
        loadFieldsFromFile();
        loadArbeitsschritteFromFile();
    }

    private void createLohnarbeitTab() {
        JPanel lohnPanel = new JPanel(new BorderLayout());
        lohnPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        lohnPanel.setBackground(Color.WHITE);
        tabbedPane.addTab("Lohn.Arbeit", lohnPanel);
        
        // Set up Lohnauftrag panel
        JPanel lohnauftragPanel = new JPanel(new GridLayout(5, 2));
        JTextField auftragsgeberField = createTextField();
        JTextField feldNrField = createTextField();
        JCheckBox dauerauftragCheckBox = new JCheckBox("Als Dauerauftrag");
        JComboBox<String> arbeitsschrittField = new JComboBox<>(arbeitsschritte.toArray(new String[0]));

        lohnauftragPanel.add(new JLabel("Auftrag von:"));
        lohnauftragPanel.add(auftragsgeberField);
        lohnauftragPanel.add(new JLabel("Arbeitsschritt:"));
        lohnauftragPanel.add(arbeitsschrittField);
        lohnauftragPanel.add(new JLabel("Feld-Nr:"));
        lohnauftragPanel.add(feldNrField);
        lohnauftragPanel.add(dauerauftragCheckBox);

        CustomButton addLohnauftragButton = createButton("Lohnauftrag hinzufügen", "resources/icons/add.png", e -> addLohnauftrag(auftragsgeberField, arbeitsschrittField, feldNrField, dauerauftragCheckBox));
        lohnauftragPanel.add(addLohnauftragButton);
        lohnPanel.add(lohnauftragPanel, BorderLayout.NORTH);

        // Set up Lohnauftrag tables
        lohnAufträgeModel = new DefaultTableModel(new String[]{"Auftrag von", "Arbeitsschritt", "Feld-Nr"}, 0);
        lohnAufträgeTable = new JTable(lohnAufträgeModel);
        lohnAufträgeTable.setFillsViewportHeight(true);

        dauerAufträgeModel = new DefaultTableModel(new String[]{"Auftrag von", "Arbeitsschritt", "Feld-Nr"}, 0);
        dauerAufträgeTable = new JTable(dauerAufträgeModel);
        dauerAufträgeTable.setFillsViewportHeight(true);

        JScrollPane lohnScrollPane = new JScrollPane(lohnAufträgeTable);
        lohnScrollPane.setPreferredSize(new Dimension(250, 200));
        JScrollPane dauerScrollPane = new JScrollPane(dauerAufträgeTable);
        dauerScrollPane.setPreferredSize(new Dimension(250, 200));

        JPanel lohnAufträgePanel = new JPanel(new BorderLayout());
        lohnAufträgePanel.add(new JLabel("Einzelaufträge:"), BorderLayout.NORTH);
        lohnAufträgePanel.add(lohnScrollPane, BorderLayout.CENTER);
        
        JPanel dauerAufträgePanel = new JPanel(new BorderLayout());
        dauerAufträgePanel.add(new JLabel("Daueraufträge:"), BorderLayout.NORTH);
        dauerAufträgePanel.add(dauerScrollPane, BorderLayout.CENTER);

        CustomButton deleteLohnauftragButton = createButton("Ausgewählten Auftrag löschen", "resources/icons/delete.png", e -> deleteSelectedLohnauftrag());
        CustomButton markLohnauftragDoneButton = createButton("Ausgewählten Auftrag als erledigt markieren", "resources/icons/done.png", e -> markLohnauftragAsDone());

        JPanel buttonPanelLohn = new JPanel();
        buttonPanelLohn.setBackground(Color.WHITE);
        buttonPanelLohn.add(deleteLohnauftragButton);
        buttonPanelLohn.add(markLohnauftragDoneButton);

        lohnPanel.add(lohnAufträgePanel, BorderLayout.WEST);
        lohnPanel.add(dauerAufträgePanel, BorderLayout.EAST);
        lohnPanel.add(buttonPanelLohn, BorderLayout.SOUTH);

        loadLohnaufträgeFromFile();
    }

    private void addLohnauftrag(JTextField auftragsgeberField, JComboBox<String> arbeitsschrittField, JTextField feldNrField, JCheckBox dauerauftragCheckBox) {
        String auftragsgeber = auftragsgeberField.getText().trim();
        String arbeitsschritt = (String) arbeitsschrittField.getSelectedItem();
        String feldNr = feldNrField.getText().trim();
        boolean istDauerauftrag = dauerauftragCheckBox.isSelected();

        if (!auftragsgeber.isEmpty() && arbeitsschritt != null && !feldNr.isEmpty()) {
            Lohnauftrag neuerAuftrag = new Lohnauftrag(auftragsgeber, arbeitsschritt, feldNr, istDauerauftrag);
            lohnaufträge.add(neuerAuftrag);
            updateLohnaufträgeTable();
            saveLohnaufträgeToFile(); // Save to file
            JOptionPane.showMessageDialog(auftragsgeberField, "Lohnauftrag hinzugefügt!");
        } else {
            JOptionPane.showMessageDialog(auftragsgeberField, "Bitte füllen Sie alle Felder aus.");
        }
    }

    private CustomButton createButton(String text, String iconPath, java.awt.event.ActionListener actionListener) {
        CustomButton button = new CustomButton(text, iconPath);
        button.addActionListener(actionListener);
        return button;
    }

    private void createSettingsTab() {
        JPanel settingsPanel = new JPanel();
        settingsPanel.setLayout(new GridBagLayout());
        settingsPanel.setBackground(new Color(240, 240, 255));
        settingsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        JLabel fontLabel = new JLabel("Wählen Sie die Schriftart:");
        String[] fonts = {"Segoe UI", "Arial", "Times New Roman"};
        JComboBox<String> fontDropdown = new JComboBox<>(fonts);
        JLabel colorLabel = new JLabel("Wählen Sie das Farbschema:");
        String[] colors = {"Hell", "Dunkel"};
        JComboBox<String> colorDropdown = new JComboBox<>(colors);

        gbc.gridx = 0; gbc.gridy = 0; settingsPanel.add(fontLabel, gbc);
        gbc.gridx = 1; settingsPanel.add(fontDropdown, gbc);
        gbc.gridx = 0; gbc.gridy = 1; settingsPanel.add(colorLabel, gbc);
        gbc.gridx = 1; settingsPanel.add(colorDropdown, gbc);

        // Benachrichtigungseinstellungen
        JCheckBox autoSaveCheckBox = new JCheckBox("Automatische Sicherung aktivieren");
        JCheckBox remindersCheckBox = new JCheckBox("Erinnerungen aktivieren");
        JCheckBox realisticCropRotationCheckBox = new JCheckBox("Realistische Fruchtfolge aktivieren");

        gbc.gridx = 0; gbc.gridy = 2; settingsPanel.add(autoSaveCheckBox, gbc);
        gbc.gridx = 0; gbc.gridy = 3; settingsPanel.add(remindersCheckBox, gbc);
        gbc.gridx = 0; gbc.gridy = 4; settingsPanel.add(realisticCropRotationCheckBox, gbc);

        // Fruchtfolgen-Management
        JLabel cropRotationLabel = new JLabel("Fruchtfolgen verwalten:");
        JButton manageCropRotationsButton = new JButton("Fruchtfolgen bearbeiten");
        manageCropRotationsButton.addActionListener(e -> openCropRotationManagementDialog());

        gbc.gridx = 0; gbc.gridy = 5; settingsPanel.add(cropRotationLabel, gbc);
        gbc.gridx = 1; settingsPanel.add(manageCropRotationsButton, gbc);

        // Arbeitsschritte-Verwaltung
        JLabel workStepsLabel = new JLabel("Arbeitsschritte verwalten:");
        JButton manageWorkStepsButton = new JButton("Arbeitsschritte bearbeiten");
        manageWorkStepsButton.addActionListener(e -> openWorkStepManagementDialog());

        gbc.gridx = 0; gbc.gridy = 6; settingsPanel.add(workStepsLabel, gbc);
        gbc.gridx = 1; settingsPanel.add(manageWorkStepsButton, gbc);

        // Speichern Button
        CustomButton saveSettingsButton = new CustomButton("Einstellungen speichern", "resources/icons/save.png");
        saveSettingsButton.addActionListener(e -> saveSettings(fontDropdown, colorDropdown, remindersCheckBox, realisticCropRotationCheckBox));
        gbc.gridwidth = 2; gbc.gridx = 0; gbc.gridy = 7; settingsPanel.add(saveSettingsButton, gbc);

        tabbedPane.addTab("Einstellungen", settingsPanel);
    }

    private void saveSettings(JComboBox<String> fontDropdown, JComboBox<String> colorDropdown, JCheckBox remindersCheckBox, JCheckBox realisticCropRotationCheckBox) {
        selectedFont = (String) fontDropdown.getSelectedItem();
        selectedColor = (String) colorDropdown.getSelectedItem();
        remindersEnabled = remindersCheckBox.isSelected();
        realisticCropRotationEnabled = realisticCropRotationCheckBox.isSelected();
        saveSettingsToFile();
        JOptionPane.showMessageDialog(tabbedPane, "Einstellungen gespeichert!");
    }

    private void createInfoTab() {
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BorderLayout());
        infoPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JTextArea infoTextArea = new JTextArea();
        infoTextArea.setEditable(false);
        infoTextArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        infoTextArea.setText(loadInfoTextFromFile());

        JScrollPane infoScrollPane = new JScrollPane(infoTextArea);
        infoPanel.add(infoScrollPane, BorderLayout.CENTER);

        // Buttons für externe Links
        JPanel buttonPanelInfo = createButtonPanelInfo();
        infoPanel.add(buttonPanelInfo, BorderLayout.SOUTH);

        tabbedPane.addTab("Info", infoPanel);
    }

    private JPanel createButtonPanelInfo() {
        JPanel buttonPanelInfo = new JPanel();
        buttonPanelInfo.setLayout(new FlowLayout());
        
        String discordURL = "https://dsc.gg/daf/";
        String twitchURL = "https://www.twitch.tv/claim_iz";
        String tiktokURL = "https://www.tiktok.com/@pics_by_claim";
        String donationURL = "https://paypal.me/erkpay";

        // Buttons erstellen
        JButton discordButton = createLinkButton("Discord", discordURL);
        JButton twitchButton = createLinkButton("Twitch", twitchURL);
        JButton tiktokButton = createLinkButton("TikTok", tiktokURL);
        JButton donationButton = createLinkButton("Donation", donationURL);

        buttonPanelInfo.add(discordButton);
        buttonPanelInfo.add(twitchButton);
        buttonPanelInfo.add(tiktokButton);
        buttonPanelInfo.add(donationButton);
        
        return buttonPanelInfo;
    }

    private JButton createLinkButton(String buttonText, String url) {
        JButton button = new JButton(buttonText);
        button.setToolTipText("Besuche unseren " + buttonText + "-Server.");
        button.addActionListener(e -> openWebpage(url));
        return button;
    }

    private void createCalendarTab() {
        JPanel calendarPanel = new JPanel(new BorderLayout());
        calendarPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        calendarPanel.setBackground(Color.WHITE);
        tabbedPane.addTab("Kalender", calendarPanel);

        JButton editFruitButton = new JButton("Frucht bearbeiten");
        editFruitButton.addActionListener(e -> openEditFruitDialog());

        String[] months = {"Frucht", "Jan", "Feb", "Mär", "Apr", "Mai", "Jun", "Jul", "Aug", "Sep", "Okt", "Nov", "Dez"};
        DefaultTableModel calendarModel = new DefaultTableModel(months, 0);
        fillCalendarModel(calendarModel);

        JTable calendarTable = new JTable(calendarModel) {
            @Override
            public Class<?> getColumnClass(int column) {
                return String.class; // Alle Spalten als String behandeln
            }
        };
        calendarTable.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        JScrollPane calendarScrollPane = new JScrollPane(calendarTable);
        calendarPanel.add(calendarScrollPane, BorderLayout.CENTER);
        calendarPanel.add(editFruitButton, BorderLayout.SOUTH); // Button unten im Kalender-Panel
    }

    private void fillCalendarModel(DefaultTableModel calendarModel) {
        for (String fruit : fruits) {
            Object[] fruitRow = new Object[12]; // 12 Monate
            fruitRow[0] = fruit; // Frucht 
            calendarModel.addRow(fruitRow);
        }
    }

    private void customizeTabbedPane() {
        tabbedPane.setBackground(Color.WHITE); // Hintergrundfarbe für Tabs
        tabbedPane.setForeground(Color.BLACK); // Schriftfarbe für Tabs
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 16)); // Schriftart für Tabs
    }

    private void openEditFruitDialog() {
        String[] options = fruits.toArray(new String[0]);
        String selectedFruit = (String) JOptionPane.showInputDialog(null,
                "Wählen Sie eine Frucht:",
                "Frucht bearbeiten",
                JOptionPane.PLAIN_MESSAGE,
                null,
                options,
                options[0]);

        if (selectedFruit != null) {
            String sowingTime = JOptionPane.showInputDialog("Geben Sie die Saatzeit für die Frucht " + selectedFruit + " ein:"); // z.B. "März"
            String harvestTime = JOptionPane.showInputDialog("Geben Sie die Erntezeit für die Frucht " + selectedFruit + " ein:"); // z.B. "August"

            if (sowingTime != null && harvestTime != null) {
                fruitTiming.put(selectedFruit, new String[]{sowingTime, harvestTime});
                updateCalendarColors();
            }
        }
    }

    private void updateCalendarColors() {
        // Logik zur Färbung der Tabelle implementieren
        // Durch alle Früchte iterieren und die Monate in der Tabelle entsprechend färben
    }

    private class CustomButton extends JButton {
        CustomButton(String text, String iconPath) {
            super(text);
            setIcon(new ImageIcon(iconPath));
            setBackground(new Color(70, 130, 180)); // Angenehmes Blau für die Buttons
            setForeground(Color.WHITE);
            setFocusPainted(false);
            setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setFont(new Font("Segoe UI", Font.BOLD, 14));
            setBorderPainted(false);
            setOpaque(true);

            // Add hover effect
            addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseEntered(java.awt.event.MouseEvent evt) {
                    setBackground(new Color(100, 150, 200)); // Dunkleres Blau bei Hover
                }

                @Override
                public void mouseExited(java.awt.event.MouseEvent evt) {
                    setBackground(new Color(70, 130, 180)); // Ursprüngliches Blau
                }
            });
        }
    }

    private void openWebpage(String urlString) {
        try {
            Desktop.getDesktop().browse(new URI(urlString));
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(tabbedPane, "Fehler beim Öffnen der Webseite: " + e.getMessage());
        }
    }

    private void loadCropRotationsFromFile() {
        cropRotations.clear();
        try (BufferedReader reader = new BufferedReader(new FileReader("Fruchtfolgen.txt"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts.length == 2) {
                    String crop = parts[0].trim();
                    String[] rotations = parts[1].split(",");
                    List<String> rotationList = new ArrayList<>();
                    for (String rotation : rotations) {
                        rotationList.add(rotation.trim());
                    }
                    cropRotations.put(crop, rotationList);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(tabbedPane, "Fehler beim Laden der Fruchtfolgen: " + e.getMessage());
        }
    }

    private void loadFruitsFromFile() {
        fruits.clear();
        fruitTiming.clear(); // Initialisiere Timings Map
        try (BufferedReader reader = new BufferedReader(new FileReader("Fruchtsorten.txt"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                fruits.add(line.trim());
                fruitTiming.put(line.trim(), new String[]{"", ""}); // Wartezeit und Erntezeit beginnen leer
            }
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(tabbedPane, "Fehler beim Laden der Fruchtsorten: " + e.getMessage());
        }
    }

    private String loadInfoTextFromFile() {
        StringBuilder infoText = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader("Info.txt"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                infoText.append(line).append("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
            return "Fehler beim Laden der Informationen.";
        }
        return infoText.toString();
    }

    private void saveSettingsToFile() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("Einstellungen.txt", false))) {
            writer.write("Schriftart:" + selectedFont);
            writer.newLine();
            writer.write("Farbschema:" + selectedColor);
            writer.newLine();
            writer.write("Erinnerungen aktivieren:" + remindersEnabled);
            writer.newLine();
            writer.write("Realistische Fruchtfolge aktivieren:" + realisticCropRotationEnabled);
            writer.newLine();
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(tabbedPane, "Fehler beim Speichern der Einstellungen: " + e.getMessage());
        }
    }

    private JTextField createTextField() {
        JTextField textField = new JTextField();
        textField.setBackground(new Color(230, 230, 230)); // Light gray background
        textField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        textField.setBorder(BorderFactory.createLineBorder(new Color(70, 130, 180))); // Angenehmes Blau für den Rand
        textField.setPreferredSize(new Dimension(200, 30)); // Set preferred size
        return textField;
    }

    private void openFieldCreationDialog() {
        JDialog fieldCreationDialog = new JDialog();
        fieldCreationDialog.setTitle("Feld Erstellung");
        fieldCreationDialog.setModal(true);
        fieldCreationDialog.setSize(400, 400);
        fieldCreationDialog.setLayout(new GridLayout(0, 2));
        fieldCreationDialog.setLocationRelativeTo(null);

        JTextField fieldNumberField = createTextField();
        JTextField fieldSizeField = createTextField();

        String[] fruits = cropRotations.keySet().toArray(new String[0]);

        JComboBox<String> currentCrop = new JComboBox<>(fruits);
        currentCrop.addActionListener(e -> updateCropRotation(currentCrop.getSelectedItem().toString(), new JComboBox<>()));

        JComboBox<String> plannedCrop = new JComboBox<>();
        JComboBox<String>[] dropdowns = createStatusDropdowns();

        // Layout build-up
        fieldCreationDialog.add(new JLabel("Feld-Nr:"));
        fieldCreationDialog.add(fieldNumberField);
        fieldCreationDialog.add(new JLabel("Feld Größe (ha):"));
        fieldCreationDialog.add(fieldSizeField);
        fieldCreationDialog.add(new JLabel("Aktuelle Frucht:"));
        fieldCreationDialog.add(currentCrop);
        fieldCreationDialog.add(new JLabel("Geplante Frucht:"));
        fieldCreationDialog.add(plannedCrop);
        addStatusDropdowns(fieldCreationDialog, dropdowns);

        // Save field information
        CustomButton saveFieldButton = new CustomButton("Speichern", "resources/icons/save.png");
        saveFieldButton.addActionListener(e -> saveField(fieldCreationDialog, fieldNumberField, fieldSizeField, currentCrop, plannedCrop, dropdowns));

        fieldCreationDialog.add(saveFieldButton);
        fieldCreationDialog.setVisible(true);
    }

    private JComboBox<String>[] createStatusDropdowns() {
        JComboBox<String>[] dropdowns = new JComboBox[8];
        for (int i = 0; i < dropdowns.length; i++) {
            dropdowns[i] = new JComboBox<>(new String[]{"Nicht Notwendig", "Notwendig", "Erledigt"});
        }
        return dropdowns;
    }

    private void addStatusDropdowns(JDialog dialog, JComboBox<String>[] dropdowns) {
        String[] labels = {"Kalken:", "Mulchen:", "Pflügen:", "Walzen:", "Unkraut Striegel:", "Spritzen:", "Steine:", "Düngen:"};
        for (int i = 0; i < dropdowns.length; i++) {
            dialog.add(new JLabel(labels[i]));
            dialog.add(dropdowns[i]);
        }
    }

    private void saveField(JDialog dialog, JTextField fieldNumberField, JTextField fieldSizeField, JComboBox<String> currentCrop, JComboBox<String> plannedCrop, JComboBox<String>[] dropdowns) {
        String fieldNumber = fieldNumberField.getText().trim();
        String fieldSize = fieldSizeField.getText().trim();
        String currentCropText = (String) currentCrop.getSelectedItem();
        String plannedCropText = (String) plannedCrop.getSelectedItem();

        if (fieldNumber.isEmpty() || fieldSize.isEmpty() || currentCropText == null || plannedCropText == null) {
            JOptionPane.showMessageDialog(dialog, "Bitte füllen Sie alle Felder aus.");
            return;
        }

        String[] statusPoints = new String[dropdowns.length];
        for (int i = 0; i < dropdowns.length; i++) {
            statusPoints[i] = (String) dropdowns[i].getSelectedItem();
        }

        // Add the field to the list
        fields.add(new Field(fieldNumber, fieldSize, currentCropText, plannedCropText, statusPoints));
        updateFieldTable();
        saveFieldsToFile();
        dialog.dispose();
        JOptionPane.showMessageDialog(dialog, "Feld gespeichert!");
        updateWorkTable();
    }

    private void editSelectedField() {
        int selectedRow = fieldTable.getSelectedRow();
        if (selectedRow != -1) {
            Field selectedField = fields.get(selectedRow);
            openFieldEditDialog(selectedField, selectedRow);
        } else {
            JOptionPane.showMessageDialog(fieldTable, "Bitte wählen Sie ein Feld zum Bearbeiten aus.");
        }
    }

    private void deleteSelectedField() {
        int selectedRow = fieldTable.getSelectedRow();
        if (selectedRow != -1) {
            int confirmation = JOptionPane.showConfirmDialog(fieldTable,
                    "Möchten Sie dieses Feld wirklich löschen?",
                    "Feld löschen",
                    JOptionPane.YES_NO_OPTION);

            if (confirmation == JOptionPane.YES_OPTION) {
                fields.remove(selectedRow);
                updateFieldTable();
                saveFieldsToFile();
                updateWorkTable();
                JOptionPane.showMessageDialog(fieldTable, "Feld gelöscht.");
            }
        } else {
            JOptionPane.showMessageDialog(fieldTable, "Bitte wählen Sie ein Feld zum Löschen aus.");
        }
    }

    private void openFieldEditDialog(Field field, int index) {
        JDialog fieldEditDialog = new JDialog();
        fieldEditDialog.setTitle("Feld bearbeiten");
        fieldEditDialog.setModal(true);
        fieldEditDialog.setSize(400, 400);
        fieldEditDialog.setLayout(new GridLayout(0, 2));
        fieldEditDialog.setLocationRelativeTo(null);

        String[] fruits = cropRotations.keySet().toArray(new String[0]);

        JComboBox<String> currentCrop = new JComboBox<>(fruits);
        currentCrop.setSelectedItem(field.currentCrop);
        JComboBox<String> plannedCrop = new JComboBox<>();
        plannedCrop.setSelectedItem(field.plannedCrop);

        currentCrop.addActionListener(e -> updateCropRotation(currentCrop.getSelectedItem().toString(), plannedCrop));

        // Layout setup without field number and size
        fieldEditDialog.add(new JLabel("Aktuelle Frucht:"));
        fieldEditDialog.add(currentCrop);
        fieldEditDialog.add(new JLabel("Geplante Frucht:"));
        fieldEditDialog.add(plannedCrop);

        String[] statusPointsLabels = {"Kalken:", "Mulchen:", "Pflügen:", "Walzen:", "Unkraut Striegel:", "Spritzen:", "Steine:", "Düngen:"};
        JComboBox<String>[] statusComboBoxes = new JComboBox[statusPointsLabels.length];
        for (int i = 0; i < statusPointsLabels.length; i++) {
            fieldEditDialog.add(new JLabel(statusPointsLabels[i]));
            JComboBox<String> statusCombo = new JComboBox<>(new String[]{"Nicht Notwendig", "Notwendig", "Erledigt"});
            statusCombo.setSelectedItem(field.statusPoints[i]);
            statusCombo.setBackground(Color.WHITE);
            statusComboBoxes[i] = statusCombo;
            fieldEditDialog.add(statusCombo);
        }

        // Save field information
        CustomButton saveFieldButton = new CustomButton("Änderungen speichern", "resources/icons/save.png");
        saveFieldButton.addActionListener(e -> saveFieldChanges(fieldEditDialog, index, currentCrop, plannedCrop, statusComboBoxes));
        fieldEditDialog.add(saveFieldButton);

        fieldEditDialog.setVisible(true);
    }

    private void saveFieldChanges(JDialog dialog, int index, JComboBox<String> currentCrop, JComboBox<String> plannedCrop, JComboBox<String>[] statusComboBoxes) {
        String currentCropText = (String) currentCrop.getSelectedItem();
        String plannedCropText = (String) plannedCrop.getSelectedItem();
        String[] statusPoints = new String[statusComboBoxes.length];

        for (int i = 0; i < statusPoints.length; i++) {
            statusPoints[i] = (String) statusComboBoxes[i].getSelectedItem();
        }

        // Update the field
        fields.set(index, new Field(fields.get(index).number, fields.get(index).size, currentCropText, plannedCropText, statusPoints));
        updateFieldTable();
        saveFieldsToFile();
        dialog.dispose();
        JOptionPane.showMessageDialog(dialog, "Feld aktualisiert!");
        updateWorkTable();
    }

    private void updateCropRotation(String currentCrop, JComboBox<String> cropRotationCombo) {
        cropRotationCombo.removeAllItems();
        List<String> rotations = cropRotations.get(currentCrop);
        if (realisticCropRotationEnabled && rotations != null) {
            for (String rotation : rotations) {
                cropRotationCombo.addItem(rotation);
            }
        } else {
            for (String crop : cropRotations.keySet()) {
                cropRotationCombo.addItem(crop);
            }
        }
    }

    private void updateFieldTable() {
        tableModel.setRowCount(0);
        for (Field field : fields) {
            tableModel.addRow(new Object[]{field.number, field.size + " ha", field.currentCrop, field.plannedCrop});
        }
    }

    private void saveFieldsToFile() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("Felder.txt", false))) {
            for (Field field : fields) {
                writer.write(field.number + "," + field.size + "," + field.currentCrop + "," + field.plannedCrop);
                for (String status : field.statusPoints) {
                    writer.write("," + status);
                }
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(tabbedPane, "Fehler beim Speichern der Felder: " + e.getMessage());
        }
    }

    private void loadFieldsFromFile() {
        try (BufferedReader reader = new BufferedReader(new FileReader("Felder.txt"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 4) {
                    String number = parts[0];
                    String size = parts[1];
                    String currentCrop = parts[2];
                    String plannedCrop = parts[3];
                    String[] statusPoints = new String[8];
                    System.arraycopy(parts, 4, statusPoints, 0, parts.length - 4);
                    fields.add(new Field(number, size, currentCrop, plannedCrop, statusPoints));
                }
            }
            updateFieldTable();
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(tabbedPane, "Fehler beim Laden der Felder: " + e.getMessage());
        }
    }

    private void loadArbeitsschritteFromFile() {
        arbeitsschritte.clear();
        try (BufferedReader reader = new BufferedReader(new FileReader("Arbeitsschritte.txt"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                arbeitsschritte.add(line.trim());
            }
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(tabbedPane, "Fehler beim Laden der Arbeitsschritte: " + e.getMessage());
        }
    }

    private void updateWorkTable() {
        workTableModel.setRowCount(0);

        String selectedWorkStep = (String) filterDropdown.getSelectedItem();

        for (Field field : fields) {
            for (int i = 0; i < field.statusPoints.length; i++) {
                if (field.statusPoints[i].equals("Notwendig")) {
                    String workStepLabel = getWorkStepLabel(i);
                    if (selectedWorkStep.equals("Alle") || workStepLabel.equals(selectedWorkStep)) {
                        workTableModel.addRow(new Object[]{field.number, workStepLabel, field.statusPoints[i]});
                    }
                }
            }
        }
    }

    private void markWorkStepAsDone() {
        int selectedRow = workTable.getSelectedRow();
        if (selectedRow != -1) {
            String fieldNumber = (String) workTableModel.getValueAt(selectedRow, 0);
            String workStep = (String) workTableModel.getValueAt(selectedRow, 1);

            int confirmation = JOptionPane.showConfirmDialog(workTable,
                    "Möchten Sie den Arbeitsschritt \"" + workStep + "\" für Feld-Nr " + fieldNumber + " als erledigt markieren?",
                    "Arbeitsschritt als erledigt markiert",
                    JOptionPane.YES_NO_OPTION);

            if (confirmation == JOptionPane.YES_OPTION) {
                for (Field field : fields) {
                    if (field.number.equals(fieldNumber)) {
                        for (int i = 0; i < field.statusPoints.length; i++) {
                            if (getWorkStepLabel(i).equals(workStep)) {
                                field.statusPoints[i] = "Erledigt";
                                break;
                            }
                        }
                        break;
                    }
                }
                updateWorkTable();
                saveFieldsToFile();
                JOptionPane.showMessageDialog(workTable, "Arbeitsschritt \"" + workStep + "\" als erledigt markiert.");
            }
        } else {
            JOptionPane.showMessageDialog(workTable, "Bitte wählen Sie einen Arbeitsschritt aus, um ihn als erledigt zu markieren.");
        }
    }

    private String getWorkStepLabel(int index) {
        switch (index) {
            case 0: return "Kalken";
            case 1: return "Mulchen";
            case 2: return "Pflügen";
            case 3: return "Walzen";
            case 4: return "Unkraut Striegel";
            case 5: return "Spritzen";
            case 6: return "Steine entfernen";
            case 7: return "Düngen";
            default: return "Unbekannter Arbeitsschritt";
        }
    }

    private void deleteSelectedLohnauftrag() {
        int selectedRow = lohnAufträgeTable.getSelectedRow();
        if (selectedRow != -1) {
            int confirmation = JOptionPane.showConfirmDialog(lohnAufträgeTable,
                    "Möchten Sie diesen Lohnauftrag wirklich löschen?",
                    "Auftrag löschen",
                    JOptionPane.YES_NO_OPTION);

            if (confirmation == JOptionPane.YES_OPTION) {
                lohnaufträge.remove(selectedRow);
                updateLohnaufträgeTable();
                saveLohnaufträgeToFile();
                JOptionPane.showMessageDialog(lohnAufträgeTable, "Lohnauftrag gelöscht.");
            }
        } else {
            JOptionPane.showMessageDialog(lohnAufträgeTable, "Bitte wählen Sie einen Lohnauftrag zum Löschen aus.");
        }
    }

    private void markLohnauftragAsDone() {
        int selectedRow = lohnAufträgeTable.getSelectedRow();
        if (selectedRow != -1) {
            Lohnauftrag selectedAuftrag = lohnaufträge.get(selectedRow);
            selectedAuftrag.arbeitsschritt += " (Erledigt)";
            updateLohnaufträgeTable();
            saveLohnaufträgeToFile();
            JOptionPane.showMessageDialog(lohnAufträgeTable, "Lohnauftrag als erledigt markiert.");
        } else {
            JOptionPane.showMessageDialog(lohnAufträgeTable, "Bitte wählen Sie einen Lohnauftrag aus, um ihn als erledigt zu markieren.");
        }
    }

    private static class Lohnauftrag {
        String auftragsgeber;
        String arbeitsschritt;
        String feldNr;
        boolean dauerauftrag;

        Lohnauftrag(String auftragsgeber, String arbeitsschritt, String feldNr, boolean dauerauftrag) {
            this.auftragsgeber = auftragsgeber;
            this.arbeitsschritt = arbeitsschritt;
            this.feldNr = feldNr;
            this.dauerauftrag = dauerauftrag;
        }
    }

    private void updateLohnaufträgeTable() {
        lohnAufträgeModel.setRowCount(0);
        dauerAufträgeModel.setRowCount(0);
        for (Lohnauftrag auftrag : lohnaufträge) {
            if (auftrag.dauerauftrag) {
                dauerAufträgeModel.addRow(new Object[]{auftrag.auftragsgeber, auftrag.arbeitsschritt, auftrag.feldNr});
            } else {
                lohnAufträgeModel.addRow(new Object[]{auftrag.auftragsgeber, auftrag.arbeitsschritt, auftrag.feldNr});
            }
        }
    }

    private void saveLohnaufträgeToFile() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("Lohnaufträge.txt", false))) {
            for (Lohnauftrag auftrag : lohnaufträge) {
                writer.write(auftrag.auftragsgeber + "," + auftrag.arbeitsschritt + "," + auftrag.feldNr + "," + auftrag.dauerauftrag);
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(tabbedPane, "Fehler beim Speichern der Lohnaufträge: " + e.getMessage());
        }
    }

    private void loadLohnaufträgeFromFile() {
        lohnaufträge.clear();
        try (BufferedReader reader = new BufferedReader(new FileReader("Lohnaufträge.txt"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length == 4) {
                    String auftragsgeber = parts[0];
                    String arbeitsschritt = parts[1];
                    String feldNr = parts[2];
                    boolean dauerauftrag = Boolean.parseBoolean(parts[3]);
                    lohnaufträge.add(new Lohnauftrag(auftragsgeber, arbeitsschritt, feldNr, dauerauftrag));
                }
            }
            updateLohnaufträgeTable();
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(tabbedPane, "Fehler beim Laden der Lohnaufträge: " + e.getMessage());
        }
    }

    private static class Field {
        String number;
        String size;
        String currentCrop;
        String plannedCrop;
        String[] statusPoints;

        Field(String number, String size, String currentCrop, String plannedCrop, String[] statusPoints) {
            this.number = number;
            this.size = size;
            this.currentCrop = currentCrop;
            this.plannedCrop = plannedCrop;
            this.statusPoints = statusPoints;
        }
    }

    private void openCropRotationManagementDialog() {
        JDialog cropRotationDialog = new JDialog();
        cropRotationDialog.setTitle("Fruchtfolgen verwalten");
        cropRotationDialog.setModal(true);
        cropRotationDialog.setSize(400, 300);
        cropRotationDialog.setLayout(new GridBagLayout());
        cropRotationDialog.setLocationRelativeTo(null);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        JLabel selectCropLabel = new JLabel("Wählen Sie eine Frucht:");
        JComboBox<String> cropComboBox = new JComboBox<>(cropRotations.keySet().toArray(new String[0]));
        cropRotationDialog.add(selectCropLabel, gbc);
        gbc.gridx = 1;
        cropRotationDialog.add(cropComboBox, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        JLabel newRotationLabel = new JLabel("Neue Fruchtfolge:");
        JTextField newRotationField = createTextField();
        cropRotationDialog.add(newRotationLabel, gbc);
        gbc.gridx = 1; cropRotationDialog.add(newRotationField, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        CustomButton addRotationButton = new CustomButton("Fruchtfolge hinzufügen", "resources/icons/add.png");
        CustomButton removeRotationButton = new CustomButton("Fruchtfolge entfernen", "resources/icons/delete.png");
        CustomButton addCropButton = new CustomButton("Neue Frucht hinzufügen", "resources/icons/add.png");

        addRotationButton.addActionListener(e -> {
            String selectedCrop = (String) cropComboBox.getSelectedItem();
            String newRotation = newRotationField.getText().trim();
            if (selectedCrop != null && !newRotation.isEmpty()) {
                cropRotations.get(selectedCrop).add(newRotation);
                newRotationField.setText("");
                JOptionPane.showMessageDialog(cropRotationDialog, "Fruchtfolge hinzugefügt!");
            } else {
                JOptionPane.showMessageDialog(cropRotationDialog, "Bitte wählen Sie eine Frucht und geben Sie eine Fruchtfolge ein.");
            }
        });

        removeRotationButton.addActionListener(e -> {
            String selectedCrop = (String) cropComboBox.getSelectedItem();
            String rotationToRemove = newRotationField.getText().trim();
            if (selectedCrop != null && !rotationToRemove.isEmpty()) {
                List<String> rotations = cropRotations.get(selectedCrop);
                if(rotations.remove(rotationToRemove)) {
                    newRotationField.setText("");
                    JOptionPane.showMessageDialog(cropRotationDialog, "Fruchtfolge entfernt!");
                } else {
                    JOptionPane.showMessageDialog(cropRotationDialog, "Fruchtfolge nicht gefunden.");
                }
            } else {
                JOptionPane.showMessageDialog(cropRotationDialog, "Bitte wählen Sie eine Frucht und geben Sie eine Fruchtfolge ein.");
            }
        });

        addCropButton.addActionListener(e -> {
            String newCrop = newRotationField.getText().trim();
            if (!newCrop.isEmpty() && !cropRotations.containsKey(newCrop)) {
                cropRotations.put(newCrop, new ArrayList<>());
                cropComboBox.addItem(newCrop);
                newRotationField.setText("");
                JOptionPane.showMessageDialog(cropRotationDialog, "Neue Frucht hinzugefügt!");
            } else {
                JOptionPane.showMessageDialog(cropRotationDialog, "Bitte geben Sie eine gültige neue Frucht ein.");
            }
        });

        gbc.gridx = 0; gbc.gridy = 3; cropRotationDialog.add(addRotationButton, gbc);
        gbc.gridx = 1; cropRotationDialog.add(removeRotationButton, gbc);
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2; cropRotationDialog.add(addCropButton, gbc);

        cropRotationDialog.setVisible(true);
    }

    private void openWorkStepManagementDialog() {
        JDialog workStepDialog = new JDialog();
        workStepDialog.setTitle("Arbeitsschritte verwalten");
        workStepDialog.setModal(true);
        workStepDialog.setSize(400, 300);
        workStepDialog.setLayout(new GridBagLayout());
        workStepDialog.setLocationRelativeTo(null);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        JLabel newWorkStepLabel = new JLabel("Neuer Arbeitsschritt:");
        JTextField newWorkStepField = createTextField();

        JButton addWorkStepButton = new JButton("Arbeitsschritt hinzufügen");
        JButton removeWorkStepButton = new JButton("Arbeitsschritt entfernen");

        addWorkStepButton.addActionListener(e -> {
            String newWorkStep = newWorkStepField.getText().trim();
            if (!newWorkStep.isEmpty() && !arbeitsschritte.contains(newWorkStep)) {
                arbeitsschritte.add(newWorkStep);
                saveArbeitsschritteToFile(); 
                newWorkStepField.setText("");
                JOptionPane.showMessageDialog(workStepDialog, "Neuer Arbeitsschritt hinzugefügt!");
            } else {
                JOptionPane.showMessageDialog(workStepDialog, "Bitte geben Sie einen neuen Arbeitsschritt ein oder er wurde bereits hinzugefügt.");
            }
        });

        removeWorkStepButton.addActionListener(e -> {
            String selectedWorkStep = (String) JOptionPane.showInputDialog(workStepDialog, "Wählen Sie einen Arbeitsschritt zum Entfernen:", "Arbeitsschritte entfernen", JOptionPane.QUESTION_MESSAGE, null, arbeitsschritte.toArray(), arbeitsschritte.get(0));
            if (selectedWorkStep != null) {
                arbeitsschritte.remove(selectedWorkStep);
                saveArbeitsschritteToFile(); 
                JOptionPane.showMessageDialog(workStepDialog, "Arbeitsschritt entfernt!");
            }
        });

        gbc.gridx = 0; gbc.gridy = 0; workStepDialog.add(newWorkStepLabel, gbc);
        gbc.gridx = 1; workStepDialog.add(newWorkStepField, gbc);
        gbc.gridx = 0; gbc.gridy = 1; workStepDialog.add(addWorkStepButton, gbc);
        gbc.gridx = 1; workStepDialog.add(removeWorkStepButton, gbc);

        workStepDialog.setVisible(true); 
    }

    private void saveArbeitsschritteToFile() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("Arbeitsschritte.txt", false))) {
            for (String schritt : arbeitsschritte) {
                writer.write(schritt);
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(tabbedPane, "Fehler beim Speichern der Arbeitsschritte: " + e.getMessage());
        }
    }
}