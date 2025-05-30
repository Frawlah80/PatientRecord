
import javax.swing.*;
import java.awt.*;
import java.io.*;

import java.util.*;
import java.text.SimpleDateFormat;

public class DoctorPatientApp extends JFrame {
    private JTextField nameField, phoneField, searchField;
    private JTextArea outputArea;
    private DefaultListModel<String> suggestionsModel;
    private JList<String> suggestionsList;
    private JButton deleteButton, editButton;
    private File dataFile = new File("patients.txt");

    // To keep track of selected patient
    private String selectedName = null;
    private String selectedPhone = null;

    public DoctorPatientApp() {
        setTitle("Shifa Dental Clinic - Frawlah LLC");
        setSize(1028, 720);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel topPanel = new JPanel(new GridLayout(4, 2));
        nameField = new JTextField();
        phoneField = new JTextField();

        addPlaceholder(nameField, "Frawlah Mohamed");
        addPlaceholder(phoneField, "123456789");

        JButton addButton = new JButton("Add Record");

        JLabel nameLabel = new JLabel("Name:");
        nameLabel.setFont(new Font("Times New Roman", Font.BOLD, 18));
        JLabel phoneLabel = new JLabel("Phone:");
        phoneLabel.setFont(new Font("Times New Roman", Font.BOLD, 18));

        topPanel.add(nameLabel);
        topPanel.add(nameField);
        topPanel.add(phoneLabel);
        topPanel.add(phoneField);
        topPanel.add(addButton);

        JPanel searchInputPanel = new JPanel(new BorderLayout());
        searchField = new JTextField();
        searchField.setPreferredSize(new Dimension(350, 25));

        JLabel searchLabel = new JLabel("Search (Name or Phone):");
        searchLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        searchInputPanel.add(searchLabel, BorderLayout.NORTH);
        searchInputPanel.add(searchField, BorderLayout.CENTER);

        suggestionsModel = new DefaultListModel<>();
        suggestionsList = new JList<>(suggestionsModel);
        suggestionsList.setFont(new Font("Times New Roman", Font.PLAIN, 20));
        JScrollPane suggestionsScrollPane = new JScrollPane(suggestionsList);
        suggestionsScrollPane.setPreferredSize(new Dimension(350, 100));

        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setPreferredSize(new Dimension(370, 300));
        leftPanel.add(searchInputPanel, BorderLayout.NORTH);
        leftPanel.add(suggestionsScrollPane, BorderLayout.CENTER);

        outputArea = new JTextArea();
        outputArea.setEditable(false);
        outputArea.setFont(new Font("Times New Roman", Font.PLAIN, 18));
        JScrollPane outputScrollPane = new JScrollPane(outputArea);
        outputScrollPane.setPreferredSize(new Dimension(270, 300));

        editButton = new JButton("Edit Patient");
        editButton.setEnabled(false);
        deleteButton = new JButton("Delete Patient");
        deleteButton.setEnabled(false);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(editButton);
        buttonPanel.add(deleteButton);

        JPanel outputPanel = new JPanel(new BorderLayout());
        outputPanel.add(outputScrollPane, BorderLayout.CENTER);
        outputPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(topPanel, BorderLayout.NORTH);
        add(leftPanel, BorderLayout.WEST);
        add(outputPanel, BorderLayout.CENTER);

        addButton.addActionListener(e -> addPatient());

        searchField.getDocument().addDocumentListener((SimpleDocumentListener) e -> updateSuggestions());
        suggestionsList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String selection = suggestionsList.getSelectedValue();
                if (selection != null) {
                    displayPatientInfo(selection);
                    editButton.setEnabled(true);
                    deleteButton.setEnabled(true);
                } else {
                    clearSelection();
                }
            }
        });

        editButton.addActionListener(e -> editSelectedPatient());

        deleteButton.addActionListener(e -> deleteSelectedPatient());

        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        updateSuggestions();

        setVisible(true);
    }

    private void addPlaceholder(JTextField field, String placeholder) {
        field.setForeground(Color.GRAY);
        field.setText(placeholder);
        field.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent e) {
                if (field.getText().equals(placeholder)) {
                    field.setText("");
                    field.setForeground(Color.BLACK);
                }
            }
            public void focusLost(java.awt.event.FocusEvent e) {
                if (field.getText().isEmpty()) {
                    field.setForeground(Color.GRAY);
                    field.setText(placeholder);
                }
            }
        });
    }

    private void addPatient() {
        String name = nameField.getText().trim();
        String phone = phoneField.getText().trim();

        if (name.isEmpty() || phone.isEmpty() || name.equals("Frawlah Mohamed") || phone.equals("123456789")) {
            JOptionPane.showMessageDialog(this, "Name and Phone are required.");
            return;
        }

        String reason = JOptionPane.showInputDialog(this, "Reason for Visit:");
        String amount = JOptionPane.showInputDialog(this, "Amount Paid:");
        String date = new SimpleDateFormat("dd/MM/yyyy").format(new Date());

        String record = name.toUpperCase() + "|" + phone + "|" + date + "|" + reason + "|" + amount;

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(dataFile, true))) {
            writer.write(record + "\n");
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        nameField.setText("");
        phoneField.setText("");

        addPlaceholder(nameField, "Frawlah Mohamed");
        addPlaceholder(phoneField, "123456789");

        JOptionPane.showMessageDialog(this, "Patient added.");

        updateSuggestions();
    }

    private void updateSuggestions() {
        String query = searchField.getText().toLowerCase();
        Set<String> results = new TreeSet<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(dataFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\|");
                if (parts.length >= 2) {
                    String name = parts[0];
                    String phone = parts[1];
                    String display = name + " (" + phone + ")";
                    if (query.isEmpty() || name.toLowerCase().contains(query) || phone.contains(query)) {
                        results.add(display);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        suggestionsModel.clear();
        results.forEach(suggestionsModel::addElement);
        clearSelection();
    }

    private void displayPatientInfo(String selection) {
        if (selection == null) return;
        String[] parts = selection.split(" \\(");
        selectedName = parts[0];
        selectedPhone = parts[1].replace(")", "");

        StringBuilder info = new StringBuilder("History for " + selectedName + " (" + selectedPhone + "):\n\n");
        double totalPaid = 0.0;

        try (BufferedReader reader = new BufferedReader(new FileReader(dataFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] fields = line.split("\\|");
                if (fields.length == 5 && fields[0].equals(selectedName) && fields[1].equals(selectedPhone)) {
                    info.append("Date: ").append(fields[2])
                            .append("\nReason: ").append(fields[3])
                            .append("\nPaid: INR ").append(fields[4]).append("\n\n");
                    try {
                        totalPaid += Double.parseDouble(fields[4]);
                    } catch (NumberFormatException e) {
                        // Skip malformed payment entries
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        info.append("\nTotal Paid: INR ").append(String.format("%.2f", totalPaid));
        outputArea.setText(info.toString());
    }

    private void deleteSelectedPatient() {
        if (selectedName == null || selectedPhone == null) return;

        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete all records for " + selectedName + " (" + selectedPhone + ")?",
                "Confirm Delete", JOptionPane.YES_NO_OPTION);

        if (confirm != JOptionPane.YES_OPTION) return;

        File tempFile = new File("patients_temp.txt");

        try (BufferedReader reader = new BufferedReader(new FileReader(dataFile));
             BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))) {

            String line;
            while ((line = reader.readLine()) != null) {
                String[] fields = line.split("\\|");
                if (!(fields.length >= 2 && fields[0].equals(selectedName) && fields[1].equals(selectedPhone))) {
                    writer.write(line);
                    writer.newLine();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error deleting patient data.");
            return;
        }

        if (!dataFile.delete()) {
            JOptionPane.showMessageDialog(this, "Error deleting original data file.");
            return;
        }

        if (!tempFile.renameTo(dataFile)) {
            JOptionPane.showMessageDialog(this, "Error renaming temporary file.");
            return;
        }

        JOptionPane.showMessageDialog(this, "All records for " + selectedName + " have been deleted.");

        clearSelection();
        updateSuggestions();
    }

    private void editSelectedPatient() {
        if (selectedName == null || selectedPhone == null) return;

        JTextField newNameField = new JTextField(selectedName);
        JTextField newPhoneField = new JTextField(selectedPhone);

        JPanel panel = new JPanel(new GridLayout(2, 2));
        JLabel newNameLabel = new JLabel("New Name:");
        newNameLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        JLabel newPhoneLabel = new JLabel("New Phone:");
        newPhoneLabel.setFont(new Font("SansSerif", Font.BOLD, 16));

        panel.add(newNameLabel);
        panel.add(newNameField);
        panel.add(newPhoneLabel);
        panel.add(newPhoneField);

        int result = JOptionPane.showConfirmDialog(this, panel,
                "Edit Patient Info", JOptionPane.OK_CANCEL_OPTION);

        if (result == JOptionPane.OK_OPTION) {
            String newName = newNameField.getText().trim();
            String newPhone = newPhoneField.getText().trim();

            if (newName.isEmpty() || newPhone.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Name and Phone cannot be empty.");
                return;
            }

            File tempFile = new File("patients_temp.txt");

            try (BufferedReader reader = new BufferedReader(new FileReader(dataFile));
                 BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))) {

                String line;
                while ((line = reader.readLine()) != null) {
                    String[] fields = line.split("\\|");
                    if (fields.length >= 2 && fields[0].equals(selectedName) && fields[1].equals(selectedPhone)) {
                        fields[0] = newName;
                        fields[1] = newPhone;
                        writer.write(String.join("|", fields));
                    } else {
                        writer.write(line);
                    }
                    writer.newLine();
                }
            } catch (IOException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error updating patient data.");
                return;
            }

            if (!dataFile.delete() || !tempFile.renameTo(dataFile)) {
                JOptionPane.showMessageDialog(this, "Error updating data file.");
                return;
            }

            selectedName = newName;
            selectedPhone = newPhone;

            JOptionPane.showMessageDialog(this, "Patient information updated.");

            updateSuggestions();
            displayPatientInfo(selectedName + " (" + selectedPhone + ")");
        }
    }

    private void clearSelection() {
        selectedName = null;
        selectedPhone = null;
        outputArea.setText("");
        editButton.setEnabled(false);
        deleteButton.setEnabled(false);
        suggestionsList.clearSelection();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(DoctorPatientApp::new);
    }

    interface SimpleDocumentListener extends javax.swing.event.DocumentListener {
        void update(javax.swing.event.DocumentEvent e);
        default void insertUpdate(javax.swing.event.DocumentEvent e) { update(e); }
        default void removeUpdate(javax.swing.event.DocumentEvent e) { update(e); }
        default void changedUpdate(javax.swing.event.DocumentEvent e) { update(e); }
    }
}
