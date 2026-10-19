import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.*;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.spec.KeySpec;
import java.util.Map;
import java.util.LinkedHashMap;

public class Encrypt extends JFrame {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final String KEY_STRING = "Key"; 
    private static final int IV_LENGTH_BYTE = 12;
    private static final int TAG_LENGTH_BIT = 128;

    private JTextField inputText;
    private JTextArea outputArea;
    private JLabel statusLabel;

    public enum DbMode {
        MYSQL("MySQL DB"),
        SQLITE("SQLite File DB"),
        IN_MEMORY("Local In-Memory DB");

        private final String name;
        DbMode(String name) { this.name = name; }
        public String getName() { return name; }
    }

    // Active Database Mode
    private static DbMode activeDbMode = DbMode.IN_MEMORY;

    // Local in-memory database fallback fields
    private static final Map<Integer, String> localDatabase = new LinkedHashMap<>();
    private static int localIdCounter = 1;

    public Encrypt() {
        super("Secure Shield - Encryption & Storage");

        // Initialize connection and mode
        initializeDatabase();

        // Premium Dark Theme styling constants
        Color bgColor = new Color(18, 18, 18);          // Dark Charcoal
        Color cardColor = new Color(30, 30, 30);        // Slightly lighter dark gray
        Color inputBgColor = new Color(45, 45, 45);     // Charcoal input field
        Color textColor = new Color(240, 240, 240);     // Off-white text
        Color secondaryTextColor = new Color(170, 170, 170); // Medium gray text
        Color primaryPurple = new Color(98, 0, 238);    // Vibrant Purple
        Color hoverPurple = new Color(124, 77, 255);    // Soft Violet
        Color pressedPurple = new Color(55, 0, 179);    // Deep Indigo
        Color darkRed = new Color(207, 102, 121);       // Soft Red/Pink for delete
        Color darkRedHover = new Color(229, 115, 115);
        Color darkRedPressed = new Color(176, 0, 32);

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Configure Frame
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 520);
        setLocationRelativeTo(null);
        getContentPane().setBackground(bgColor);
        setLayout(new BorderLayout());

        // Header Panel
        JPanel headerPanel = new JPanel(new GridLayout(2, 1, 2, 2));
        headerPanel.setBackground(bgColor);
        headerPanel.setBorder(new EmptyBorder(15, 20, 10, 20));
        
        JLabel titleLabel = new JLabel("SECURE CRYPTO STORAGE");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setForeground(textColor);
        
        JLabel subtitleLabel = new JLabel("Encrypt texts securely with AES/GCM and save locally");
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subtitleLabel.setForeground(secondaryTextColor);
        
        headerPanel.add(titleLabel);
        headerPanel.add(subtitleLabel);
        add(headerPanel, BorderLayout.NORTH);

        // Center Panel (Main Container)
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBackground(bgColor);
        mainPanel.setBorder(new EmptyBorder(5, 20, 15, 20));

        // Input Form Area
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(cardColor);
        formPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(45, 45, 45), 1, true),
            new EmptyBorder(15, 15, 15, 15)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 6, 6, 6);

        JLabel inputLabel = new JLabel("Enter Plaintext / Record ID:");
        inputLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        inputLabel.setForeground(textColor);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.3;
        formPanel.add(inputLabel, gbc);

        inputText = new JTextField(20);
        inputText.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        inputText.setBackground(inputBgColor);
        inputText.setForeground(textColor);
        inputText.setCaretColor(textColor);
        inputText.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(70, 70, 70), 1, true),
            new EmptyBorder(8, 10, 8, 10)
        ));
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 0.7;
        formPanel.add(inputText, gbc);

        // Main Actions
        JPanel actionButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actionButtonPanel.setBackground(cardColor);
        
        ModernButton encryptButton = new ModernButton("Encrypt & Store", primaryPurple, hoverPurple, pressedPurple);
        ModernButton decryptButton = new ModernButton("Decrypt by ID", primaryPurple, hoverPurple, pressedPurple);
        actionButtonPanel.add(encryptButton);
        actionButtonPanel.add(decryptButton);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(12, 6, 6, 6);
        formPanel.add(actionButtonPanel, gbc);

        mainPanel.add(formPanel);
        mainPanel.add(Box.createVerticalStrut(15));

        // Output Display Area
        JPanel outputPanel = new JPanel(new BorderLayout());
        outputPanel.setBackground(cardColor);
        outputPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(45, 45, 45), 1, true),
            new EmptyBorder(10, 10, 10, 10)
        ));

        outputArea = new JTextArea(10, 30);
        outputArea.setEditable(false);
        outputArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        outputArea.setBackground(new Color(25, 25, 25));
        outputArea.setForeground(new Color(200, 200, 200));
        outputArea.setCaretColor(Color.WHITE);
        outputArea.setLineWrap(true);
        outputArea.setWrapStyleWord(true);
        
        JScrollPane scrollPane = new JScrollPane(outputArea);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(40, 40, 40)));
        outputPanel.add(scrollPane, BorderLayout.CENTER);

        // Dynamic status bar on output panel
        statusLabel = new JLabel("Database Connection Mode: " + activeDbMode.getName());
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        statusLabel.setBorder(new EmptyBorder(5, 5, 0, 5));
        if (activeDbMode == DbMode.MYSQL) {
            statusLabel.setForeground(new Color(3, 218, 198)); // Teal
        } else if (activeDbMode == DbMode.SQLITE) {
            statusLabel.setForeground(new Color(0, 191, 255)); // Sky Blue
        } else {
            statusLabel.setForeground(new Color(207, 102, 121)); // Red/Pink
        }
        outputPanel.add(statusLabel, BorderLayout.SOUTH);

        mainPanel.add(outputPanel);
        add(mainPanel, BorderLayout.CENTER);

        // Bottom Operations Panel
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBackground(bgColor);
        bottomPanel.setBorder(new EmptyBorder(0, 20, 20, 20));

        JPanel utilityBtnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        utilityBtnPanel.setBackground(bgColor);

        ModernButton displayButton = new ModernButton("Display All Logs", new Color(60, 60, 60), new Color(80, 80, 80), new Color(40, 40, 40));
        ModernButton deleteButton = new ModernButton("Delete Record", darkRed, darkRedHover, darkRedPressed);
        utilityBtnPanel.add(displayButton);
        utilityBtnPanel.add(deleteButton);

        ModernButton exitButton = new ModernButton("Exit App", new Color(60, 60, 60), new Color(80, 80, 80), new Color(40, 40, 40));

        bottomPanel.add(utilityBtnPanel, BorderLayout.WEST);
        bottomPanel.add(exitButton, BorderLayout.EAST);
        add(bottomPanel, BorderLayout.SOUTH);

        // Actions Wiring
        encryptButton.addActionListener(e -> handleEncrypt());
        decryptButton.addActionListener(e -> handleDecrypt());
        displayButton.addActionListener(e -> handleDisplay());
        deleteButton.addActionListener(e -> handleDelete());
        exitButton.addActionListener(e -> System.exit(0));

        setVisible(true);
    }

    private static void initializeDatabase() {
        // 1. Try MySQL Connection
        String mysqlUrl = "jdbc:mysql://localhost:3306/miniproj";
        String mysqlUser = "root";
        String mysqlPass = "PHW#84#JEOR";
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            try (Connection conn = DriverManager.getConnection(mysqlUrl, mysqlUser, mysqlPass)) {
                activeDbMode = DbMode.MYSQL;
                System.out.println("Connected to MySQL successfully.");
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute("CREATE TABLE IF NOT EXISTS encrypted_texts (id INT AUTO_INCREMENT PRIMARY KEY, encrypted_text TEXT)");
                } catch (SQLException ignored) {}
                return;
            }
        } catch (Throwable e) {
            System.out.println("MySQL database connection failed: " + e.getMessage());
        }

        // 2. Try SQLite Connection (Embedded Local DB)
        String sqliteUrl = "jdbc:sqlite:db/encrypted_texts.db";
        try {
            Class.forName("org.sqlite.JDBC");
            try (Connection conn = DriverManager.getConnection(sqliteUrl)) {
                activeDbMode = DbMode.SQLITE;
                System.out.println("Connected to SQLite file database successfully.");
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute("CREATE TABLE IF NOT EXISTS encrypted_texts (id INTEGER PRIMARY KEY AUTOINCREMENT, encrypted_text TEXT NOT NULL)");
                }
                return;
            }
        } catch (Throwable e) {
            System.out.println("SQLite local database connection failed: " + e.getMessage());
        }

        // 3. Fallback to Local In-Memory LinkedHashMap
        activeDbMode = DbMode.IN_MEMORY;
        System.out.println("Database drivers not loaded or connection issues. Falling back to local in-memory DB.");
    }

    private void handleEncrypt() {
        try {
            String originalText = inputText.getText().trim();
            if (originalText.isEmpty()) {
                outputArea.setText("Error: Input text is empty. Enter something to encrypt.");
                return;
            }
            String encryptedText = encryptText(originalText);
            storeEncryptedTextInDatabase(encryptedText);
            outputArea.setText("Text encrypted and stored successfully.\n\nPlaintext: " + originalText + "\n\nCiphertext (AES/GCM Base64):\n" + encryptedText);
            inputText.setText("");
        } catch (Exception ex) {
            ex.printStackTrace();
            outputArea.setText("Error during encryption: " + ex.getMessage());
        }
    }

    private void handleDecrypt() {
        try {
            String idStr = inputText.getText().trim();
            if (idStr.isEmpty()) {
                outputArea.setText("Error: Enter an integer ID to decrypt.");
                return;
            }
            int id = Integer.parseInt(idStr);
            String decryptedText = decryptTextFromDatabase(id);
            outputArea.setText("Decrypted Output:\n\nRecord ID: " + id + "\nPlaintext Decrypted: " + decryptedText);
        } catch (NumberFormatException nfe) {
            outputArea.setText("Invalid ID format. Please enter an integer ID to decrypt.");
        } catch (Exception ex) {
            ex.printStackTrace();
            outputArea.setText("Error during decryption: " + ex.getMessage());
        }
    }

    private void handleDisplay() {
        displayEncryptedTextsFromDatabase(outputArea);
    }

    private void handleDelete() {
        try {
            String idStr = inputText.getText().trim();
            if (idStr.isEmpty()) {
                outputArea.setText("Error: Enter a Record ID to delete.");
                return;
            }
            int deleteId = Integer.parseInt(idStr);
            deleteTextFromDatabase(deleteId);
            outputArea.setText("Record deletion executed for ID: " + deleteId);
            handleDisplay();
        } catch (NumberFormatException e) {
            outputArea.setText("Invalid ID format. Please enter an integer ID to delete.");
        }
    }

    public static String encryptText(String text) throws Exception {
        SecretKey secretKey = generateKey();
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        
        byte[] iv = new byte[IV_LENGTH_BYTE];
        new java.security.SecureRandom().nextBytes(iv);
        javax.crypto.spec.GCMParameterSpec parameterSpec = new javax.crypto.spec.GCMParameterSpec(TAG_LENGTH_BIT, iv);
        
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);
        byte[] ciphertext = cipher.doFinal(text.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        
        // Pack GCM IV + Ciphertext
        byte[] packed = new byte[iv.length + ciphertext.length];
        System.arraycopy(iv, 0, packed, 0, iv.length);
        System.arraycopy(ciphertext, 0, packed, iv.length, ciphertext.length);
        
        return Base64.getEncoder().encodeToString(packed);
    }

    public static String decryptText(String encryptedText) throws Exception {
        byte[] packed = Base64.getDecoder().decode(encryptedText);
        if (packed.length < IV_LENGTH_BYTE) {
            throw new IllegalArgumentException("Incorrect cipher size.");
        }
        
        byte[] iv = new byte[IV_LENGTH_BYTE];
        System.arraycopy(packed, 0, iv, 0, iv.length);
        
        int ciphertextLen = packed.length - IV_LENGTH_BYTE;
        byte[] ciphertext = new byte[ciphertextLen];
        System.arraycopy(packed, iv.length, ciphertext, 0, ciphertextLen);
        
        SecretKey secretKey = generateKey();
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        javax.crypto.spec.GCMParameterSpec parameterSpec = new javax.crypto.spec.GCMParameterSpec(TAG_LENGTH_BIT, iv);
        
        cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);
        byte[] plaintext = cipher.doFinal(ciphertext);
        
        return new String(plaintext, java.nio.charset.StandardCharsets.UTF_8);
    }

    public static SecretKey generateKey() throws Exception {
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(KEY_STRING.toCharArray(), KEY_STRING.getBytes(), 65536, 256);
        SecretKey tmp = factory.generateSecret(spec);
        return new SecretKeySpec(tmp.getEncoded(), ALGORITHM);
    }
    
    private static void storeEncryptedTextInDatabase(String encryptedText) {
        if (activeDbMode == DbMode.IN_MEMORY) {
            localDatabase.put(localIdCounter++, encryptedText);
            return;
        }
        String url = (activeDbMode == DbMode.MYSQL) ? "jdbc:mysql://localhost:3306/miniproj" : "jdbc:sqlite:db/encrypted_texts.db";
        String user = (activeDbMode == DbMode.MYSQL) ? "root" : "";
        String pass = (activeDbMode == DbMode.MYSQL) ? "PHW#84#JEOR" : "";

        String query = "INSERT INTO encrypted_texts (encrypted_text) VALUES (?)";
        try (Connection connection = DriverManager.getConnection(url, user, pass);
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setString(1, encryptedText);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Database storage failed. Falling back to local memory: " + e.getMessage());
            localDatabase.put(localIdCounter++, encryptedText);
        }
    }

    private static String decryptTextFromDatabase(int id) {
        if (activeDbMode == DbMode.IN_MEMORY) {
            if (localDatabase.containsKey(id)) {
                try {
                    return decryptText(localDatabase.get(id));
                } catch (Exception e) {
                    return "Error decrypting local text: " + e.getMessage();
                }
            } else {
                return "Text not found (Memory)";
            }
        }
        
        String url = (activeDbMode == DbMode.MYSQL) ? "jdbc:mysql://localhost:3306/miniproj" : "jdbc:sqlite:db/encrypted_texts.db";
        String user = (activeDbMode == DbMode.MYSQL) ? "root" : "";
        String pass = (activeDbMode == DbMode.MYSQL) ? "PHW#84#JEOR" : "";

        String query = "SELECT encrypted_text FROM encrypted_texts WHERE id = ?";
        try (Connection connection = DriverManager.getConnection(url, user, pass);
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setInt(1, id);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    String encryptedText = resultSet.getString("encrypted_text");
                    return decryptText(encryptedText);
                }
            }
        } catch (Exception e) {
            System.err.println("Database fetch failed: " + e.getMessage());
        }
        
        // Fallback checks
        if (localDatabase.containsKey(id)) {
            try {
                return decryptText(localDatabase.get(id));
            } catch (Exception e) {
                return "Error decrypting: " + e.getMessage();
            }
        }
        return "Record not found.";
    }

    private static void displayEncryptedTextsFromDatabase(JTextArea outputArea) {
        StringBuilder resultText = new StringBuilder();
        resultText.append("--- DATABASE ACTIVE MODE: ").append(activeDbMode.getName()).append(" ---\n\n");

        if (activeDbMode == DbMode.IN_MEMORY) {
            if (localDatabase.isEmpty()) {
                resultText.append("No encrypted records in memory database.");
            } else {
                for (Map.Entry<Integer, String> entry : localDatabase.entrySet()) {
                    resultText.append("Record ID: ").append(entry.getKey())
                              .append("\nEncrypted Text: ").append(entry.getValue()).append("\n\n");
                }
            }
            outputArea.setText(resultText.toString());
            return;
        }

        String url = (activeDbMode == DbMode.MYSQL) ? "jdbc:mysql://localhost:3306/miniproj" : "jdbc:sqlite:db/encrypted_texts.db";
        String user = (activeDbMode == DbMode.MYSQL) ? "root" : "";
        String pass = (activeDbMode == DbMode.MYSQL) ? "PHW#84#JEOR" : "";

        String query = "SELECT id, encrypted_text FROM encrypted_texts ORDER BY id ASC";
        try (Connection connection = DriverManager.getConnection(url, user, pass);
             PreparedStatement preparedStatement = connection.prepareStatement(query);
             ResultSet resultSet = preparedStatement.executeQuery()) {
            
            boolean found = false;
            while (resultSet.next()) {
                found = true;
                int id = resultSet.getInt("id");
                String encryptedText = resultSet.getString("encrypted_text");
                resultText.append("Record ID: ").append(id)
                          .append("\nEncrypted Text: ").append(encryptedText).append("\n\n");
            }
            if (!found) {
                resultText.append("No encrypted records in SQL database.");
            }
        } catch (SQLException e) {
            resultText.append("Database error occurred: ").append(e.getMessage());
        }

        if (!localDatabase.isEmpty()) {
            resultText.append("\n--- In-Memory Fallback Records ---\n");
            for (Map.Entry<Integer, String> entry : localDatabase.entrySet()) {
                resultText.append("Record ID (Memory): ").append(entry.getKey())
                          .append("\nEncrypted Text: ").append(entry.getValue()).append("\n\n");
            }
        }

        outputArea.setText(resultText.toString());
    }

    private static void deleteTextFromDatabase(int id) {
        if (activeDbMode == DbMode.IN_MEMORY) {
            if (localDatabase.containsKey(id)) {
                localDatabase.remove(id);
                System.out.println("Text with ID " + id + " deleted from memory.");
            } else {
                System.out.println("Text not found with ID: " + id + " in memory.");
            }
            return;
        }

        String url = (activeDbMode == DbMode.MYSQL) ? "jdbc:mysql://localhost:3306/miniproj" : "jdbc:sqlite:db/encrypted_texts.db";
        String user = (activeDbMode == DbMode.MYSQL) ? "root" : "";
        String pass = (activeDbMode == DbMode.MYSQL) ? "PHW#84#JEOR" : "";

        String query = "DELETE FROM encrypted_texts WHERE id = ?";
        try (Connection connection = DriverManager.getConnection(url, user, pass);
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setInt(1, id);
            int rowsAffected = preparedStatement.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("Text with ID " + id + " deleted successfully from DB.");
            } else {
                if (localDatabase.containsKey(id)) {
                    localDatabase.remove(id);
                    System.out.println("Text with ID " + id + " deleted successfully from local fallback memory.");
                } else {
                    System.out.println("Text not found with ID: " + id);
                }
            }
        } catch (SQLException e) {
            System.err.println("DB delete exception: " + e.getMessage());
            localDatabase.remove(id);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Encrypt::new);
    }

    // Modern Flat Styling Button Subclass
    static class ModernButton extends JButton {
        private final Color normalColor;
        private final Color hoverColor;
        private final Color pressedColor;

        public ModernButton(String text, Color normal, Color hover, Color pressed) {
            super(text);
            this.normalColor = normal;
            this.hoverColor = hover;
            this.pressedColor = pressed;

            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setForeground(Color.WHITE);
            setFont(new Font("Segoe UI", Font.BOLD, 12));
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (getModel().isPressed()) {
                g2.setColor(pressedColor);
            } else if (getModel().isRollover()) {
                g2.setColor(hoverColor);
            } else {
                g2.setColor(normalColor);
            }
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
            super.paintComponent(g2);
            g2.dispose();
        }
    }
}
