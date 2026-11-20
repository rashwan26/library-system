package gui;

// Java core imports
import entities.*;
import exceptions.*;
import fileio.FileHandler;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.DefaultTableModel;
import patterns.BookFactory;
import services.LibraryService;

public class LibraryGUI extends JFrame {
    private FileHandler fileHandler;
    private JButton addBookButton, editBookButton, saveBookButton;
    private JButton addMemberButton, editMemberButton, saveMemberButton;
    private LibraryService libraryService;
    private JTabbedPane tabbedPane;

    // Enhanced Color scheme - Black Background with Blue Gradient (No White!)
    private final Color BACKGROUND_COLOR = new Color(10, 10, 15);
    private final Color PANEL_BACKGROUND = new Color(18, 18, 25);
    private final Color PRIMARY_COLOR = new Color(0, 150, 255);
    private final Color PRIMARY_GRADIENT = new Color(0, 180, 255);
    private final Color SECONDARY_COLOR = new Color(100, 200, 255);
    private final Color TEXT_COLOR = new Color(220, 240, 255);
    private final Color SUBTLE_TEXT = new Color(160, 180, 200);
    private final Color TABLE_BACKGROUND = new Color(25, 25, 35);
    private final Color TABLE_HEADER_COLOR = new Color(35, 35, 45);
    private final Color HIGHLIGHT_COLOR = new Color(150, 220, 255);
    private final Color INPUT_BACKGROUND = new Color(30, 30, 40);
    private final Color SUCCESS_COLOR = new Color(100, 255, 180);
    private final Color ERROR_COLOR = new Color(255, 100, 150);
    private final Color WARNING_COLOR = new Color(255, 200, 100);
    private final Color PAGE_COLOR = new Color(180, 200, 255);
    private final Color PRICE_COLOR = new Color(255, 220, 120);
    private final Color IMAGE_BORDER_COLOR = new Color(60, 80, 120);
    private final Color EDIT_COLOR = new Color(255, 180, 100); // New color for edit buttons

    // Components
    private JTextField txtIsbn, txtTitle, txtAuthor, txtYear, txtPrice;
    private JComboBox<String> cmbBookType;
    private JTable bookTable;
    private DefaultTableModel bookTableModel;

    private JTextField txtMemberId, txtMemberName, txtMemberEmail, txtMemberPhone;
    private JTable memberTable;
    private DefaultTableModel memberTableModel;

    private JTextField txtBorrowMemberId, txtBorrowIsbn;
    private JTextField txtReturnMemberId, txtReturnIsbn;
    private JTextArea txtTransactionStatus;

    // Statistics
    private JLabel lblTotalBooks, lblTotalMembers, lblAvailableBooks, lblOverdueBooks;

    // Book Image components
    private JLabel lblBookCover;
    private JButton btnUploadCover, btnRemoveCover;
    private File currentImageFile;
    private String currentBookIsbnForImage;

    // Member Image components
    private JLabel lblMemberPhoto;
    private JButton btnUploadMemberPhoto, btnRemoveMemberPhoto;
    private File currentMemberImageFile;
    private String currentMemberIdForImage;

    // Edit mode flags
    private boolean isEditingBook = false;
    private String editingBookIsbn = "";
    private boolean isEditingMember = false;
    private String editingMemberId = "";

    // Image storage directories
    private static final String IMAGES_DIR = FileHandler.dataDirectory().resolve("book_covers").toString() + File.separator;
    private static final String MEMBER_IMAGES_DIR = FileHandler.dataDirectory().resolve("member_photos").toString() + File.separator;

    private static int selectedModelRow(JTable table) {
        return util.TableSelection.modelRow(table);
    }
    private boolean confirmOverwrite(File file) {
        if(file.toPath().toAbsolutePath().normalize().startsWith(FileHandler.dataDirectory())) {
            showStyledErrorDialog("Choose an export location outside the library data directory.");return false;
        }
        return !file.exists() || JOptionPane.showConfirmDialog(this,"Replace the existing file?","Confirm overwrite",JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION;
    }

    public LibraryGUI() {
        fileHandler = new FileHandler();
        libraryService = new LibraryService();
        initializeUI();
        loadInitialData();
        startStatsUpdater();
        createImagesDirectory();
        createMemberImagesDirectory();
    }

    private void createImagesDirectory() {
        File imagesDir = new File(IMAGES_DIR);
        if (!imagesDir.exists()) {
            imagesDir.mkdirs();
        }
    }

    private void createMemberImagesDirectory() {
        File imagesDir = new File(MEMBER_IMAGES_DIR);
        if (!imagesDir.exists()) {
            imagesDir.mkdirs();
        }
    }

    private void initializeUI() {
        setTitle("📚 LIBRARY MANAGEMENT SYSTEM - NEXUS EDITION");
        setSize(1400, 900);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        getContentPane().setBackground(BACKGROUND_COLOR);
        setIconImage(createAppIcon());

        tabbedPane = createStyledTabbedPane();

        tabbedPane.addTab("📖 BOOKS", createBookPanel());
        tabbedPane.addTab("👥 MEMBERS", createMemberPanel());
        tabbedPane.addTab("🔄 TRANSACTIONS", createTransactionPanel());
        tabbedPane.addTab("🔍 SEARCH", createSearchPanel());
        tabbedPane.addTab("📊 DASHBOARD", createDashboardPanel());

        add(tabbedPane);
        createMenuBar();
        createStatusBar();
    }

    private Image createAppIcon() {
        int size = 32;
        BufferedImage icon = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = icon.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setColor(PRIMARY_COLOR);
        g2.fillRoundRect(5, 5, 22, 24, 5, 5);
        g2.setColor(PRIMARY_GRADIENT);
        g2.fillRoundRect(6, 6, 20, 22, 4, 4);

        g2.setColor(PAGE_COLOR);
        g2.fillRoundRect(10, 8, 12, 18, 2, 2);

        g2.setColor(new Color(200, 220, 255));
        for (int i = 0; i < 5; i++) {
            g2.fillRect(12, 12 + i * 3, 8, 1);
        }

        g2.dispose();
        return icon;
    }

    private JTabbedPane createStyledTabbedPane() {
        JTabbedPane pane = new JTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
        pane.setBackground(BACKGROUND_COLOR);
        pane.setForeground(TEXT_COLOR);
        pane.setFont(new Font("Segoe UI", Font.BOLD, 13));

        pane.setUI(new javax.swing.plaf.basic.BasicTabbedPaneUI() {
            @Override
            protected void paintTabBackground(Graphics g, int tabPlacement, int tabIndex,
                    int x, int y, int w, int h, boolean isSelected) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                GradientPaint gradient;
                if (isSelected) {
                    gradient = new GradientPaint(x, y, PRIMARY_COLOR, x, y + h, PRIMARY_GRADIENT);
                } else {
                    gradient = new GradientPaint(x, y, new Color(30, 30, 40), x, y + h, new Color(25, 25, 35));
                }

                g2d.setPaint(gradient);
                g2d.fillRoundRect(x + 2, y + 2, w - 4, h - 2, 10, 10);

                g2d.setColor(isSelected ? PRIMARY_COLOR.brighter() : new Color(60, 60, 70));
                g2d.drawRoundRect(x + 2, y + 2, w - 4, h - 2, 10, 10);
            }

            @Override
            protected void paintTabBorder(Graphics g, int tabPlacement, int tabIndex,
                    int x, int y, int w, int h, boolean isSelected) {
                // Border is painted in paintTabBackground
            }

            @Override
            protected void paintContentBorder(Graphics g, int tabPlacement, int selectedIndex) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setColor(new Color(40, 40, 50));
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        });

        return pane;
    }

    private void createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        menuBar.setBackground(new Color(25, 25, 35));
        menuBar.setBorder(BorderFactory.createLineBorder(new Color(50, 50, 60), 1));

        JMenu fileMenu = createStyledMenu("File", 'F');
        addStyledMenuItem(fileMenu, "Export Report", 'E', e -> exportReport());
        fileMenu.addSeparator();
        addStyledMenuItem(fileMenu, "Exit", 'X', e -> System.exit(0));

        JMenu toolsMenu = createStyledMenu("Tools", 'T');
        addStyledMenuItem(toolsMenu, "Refresh Data", 'R', e -> refreshAllData());
        addStyledMenuItem(toolsMenu, "Clear All Logs", 'L', e -> clearAllLogs());
        addStyledMenuItem(toolsMenu, "Back Up Library Data", 'B', e -> backupDatabase());

        JMenu helpMenu = createStyledMenu("Help", 'H');
        addStyledMenuItem(helpMenu, "User Guide", 'U', e -> showUserGuide());
        addStyledMenuItem(helpMenu, "About", 'A', e -> showAboutDialog());

        menuBar.add(fileMenu);
        menuBar.add(toolsMenu);
        menuBar.add(helpMenu);
        setJMenuBar(menuBar);
    }

    private void createStatusBar() {
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBackground(new Color(25, 25, 35));
        statusBar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(50, 50, 60)),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));

        JLabel statusLabel = new JLabel("Ready");
        statusLabel.setForeground(SUBTLE_TEXT);
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));

        JLabel timeLabel = new JLabel();
        timeLabel.setForeground(SUBTLE_TEXT);
        timeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));

        Timer timer = new Timer(1000, e -> {
            timeLabel.setText(new java.text.SimpleDateFormat("HH:mm:ss | yyyy-MM-dd").format(new java.util.Date()));
        });
        timer.start();

        statusBar.add(statusLabel, BorderLayout.WEST);
        statusBar.add(timeLabel, BorderLayout.EAST);

        add(statusBar, BorderLayout.SOUTH);
    }

    private JMenu createStyledMenu(String text, char mnemonic) {
        JMenu menu = new JMenu(text);
        menu.setMnemonic(mnemonic);
        menu.setForeground(TEXT_COLOR);
        menu.setBackground(new Color(25, 25, 35));
        menu.setFont(new Font("Segoe UI", Font.BOLD, 12));

        menu.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                menu.setForeground(HIGHLIGHT_COLOR);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                menu.setForeground(TEXT_COLOR);
            }
        });

        return menu;
    }

    private void addStyledMenuItem(JMenu menu, String text, char mnemonic, ActionListener action) {
        JMenuItem item = new JMenuItem(text);
        item.setMnemonic(mnemonic);
        item.setAccelerator(KeyStroke.getKeyStroke(mnemonic, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        item.setForeground(TEXT_COLOR);
        item.setBackground(new Color(35, 35, 45));
        item.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        item.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                item.setBackground(HIGHLIGHT_COLOR);
                item.setForeground(new Color(20, 20, 30));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                item.setBackground(new Color(35, 35, 45));
                item.setForeground(TEXT_COLOR);
            }
        });

        item.addActionListener(action);
        menu.add(item);
    }

    private JPanel createBookPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(PANEL_BACKGROUND);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Create main container with proper spacing
        JPanel mainContainer = new JPanel(new BorderLayout(10, 10));
        mainContainer.setBackground(PANEL_BACKGROUND);

        // Top Panel: Book Cover + Add Book Form (side by side)
        JPanel topPanel = new JPanel(new GridLayout(1, 2, 15, 0));
        topPanel.setBackground(PANEL_BACKGROUND);
        topPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        // Left Panel: Book Cover (compact size)
        JPanel leftPanel = new JPanel(new BorderLayout(5, 5));
        leftPanel.setBackground(new Color(28, 28, 38));
        leftPanel.setBorder(createStyledBorder("📸 BOOK COVER"));

        // Book cover label (smaller)
        lblBookCover = new JLabel();
        lblBookCover.setHorizontalAlignment(SwingConstants.CENTER);
        lblBookCover.setVerticalAlignment(SwingConstants.CENTER);
        lblBookCover.setOpaque(true);
        lblBookCover.setBackground(new Color(40, 40, 50));
        lblBookCover.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(IMAGE_BORDER_COLOR, 1),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        lblBookCover.setPreferredSize(new Dimension(180, 220));

        // Create default cover
        setDefaultBookCover();

        // Button panel for cover
        JPanel coverButtonPanel = new JPanel(new GridLayout(1, 2, 5, 0));
        coverButtonPanel.setBackground(new Color(28, 28, 38));
        coverButtonPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        btnUploadCover = createStyledButton("📁 UPLOAD", new Color(80, 180, 120));
        btnRemoveCover = createStyledButton("🗑️ REMOVE", new Color(255, 100, 150));

        btnUploadCover.addActionListener(e -> uploadBookCover());
        btnRemoveCover.addActionListener(e -> removeBookCover());

        coverButtonPanel.add(btnUploadCover);
        coverButtonPanel.add(btnRemoveCover);

        leftPanel.add(lblBookCover, BorderLayout.CENTER);
        leftPanel.add(coverButtonPanel, BorderLayout.SOUTH);

        // Right Panel: Add Book Form (more space)
        JPanel rightPanel = new JPanel(new BorderLayout(5, 5));
        rightPanel.setBackground(new Color(28, 28, 38));
        rightPanel.setBorder(createStyledBorder("📖 BOOK MANAGEMENT"));

        // Form panel with GridBagLayout for better control
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(new Color(28, 28, 38));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 10, 5, 10);

        // Book Type
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.3;
        formPanel.add(createStyledLabel("Book Type:*"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.7;
        gbc.gridwidth = 2;
        cmbBookType = createStyledComboBox(new String[]{"FICTION", "NONFICTION", "TEXTBOOK", "REFERENCE"});
        cmbBookType.setPreferredSize(new Dimension(150, 30));
        formPanel.add(cmbBookType, gbc);

        // ISBN
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        formPanel.add(createStyledLabel("ISBN:*"), gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 2;
        txtIsbn = createStyledTextField();
        txtIsbn.setPreferredSize(new Dimension(200, 30));
        formPanel.add(txtIsbn, gbc);

        // Title
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        formPanel.add(createStyledLabel("Title:*"), gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 2;
        txtTitle = createStyledTextField();
        txtTitle.setPreferredSize(new Dimension(200, 30));
        formPanel.add(txtTitle, gbc);

        // Author
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 1;
        formPanel.add(createStyledLabel("Author:*"), gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 2;
        txtAuthor = createStyledTextField();
        txtAuthor.setPreferredSize(new Dimension(200, 30));
        formPanel.add(txtAuthor, gbc);

        // Year
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 1;
        formPanel.add(createStyledLabel("Year:*"), gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 1;
        txtYear = createStyledTextField();
        txtYear.setPreferredSize(new Dimension(80, 30));
        formPanel.add(txtYear, gbc);

        // Price
        gbc.gridx = 2;
        gbc.gridy = 4;
        gbc.insets = new Insets(5, 20, 5, 10);
        formPanel.add(createStyledLabel("Price:*"), gbc);

        gbc.gridx = 3;
        txtPrice = createStyledTextField();
        txtPrice.setPreferredSize(new Dimension(80, 30));
        formPanel.add(txtPrice, gbc);

        // Action buttons panel - UPDATED WITH EDIT BUTTON
        JPanel buttonPanel = new JPanel(new GridLayout(2, 3, 5, 5));
        buttonPanel.setBackground(new Color(28, 28, 38));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JButton btnAdd = createGradientButton("➕ ADD BOOK", PRIMARY_COLOR, PRIMARY_GRADIENT);
        JButton btnEdit = createGradientButton("✏️ EDIT", EDIT_COLOR, new Color(255, 200, 130));
        JButton btnSaveEdit = createGradientButton("💾 SAVE", SUCCESS_COLOR, new Color(120, 255, 200));
        JButton btnClear = createStyledButton("🧹 CLEAR", new Color(70, 70, 80));
        JButton btnDelete = createGradientButton("🗑️ DELETE", new Color(255, 80, 150), new Color(255, 120, 180));
        JButton btnDetails = createStyledButton("📋 DETAILS", new Color(80, 120, 200));

        addBookButton=btnAdd; editBookButton=btnEdit; saveBookButton=btnSaveEdit;
        btnAdd.addActionListener(e -> addNewBook());
        btnEdit.addActionListener(e -> editSelectedBook());
        btnSaveEdit.addActionListener(e -> saveBookEdit());
        btnClear.addActionListener(e -> clearBookFields());
        btnDelete.addActionListener(e -> deleteSelectedBook());
        btnDetails.addActionListener(e -> viewBookDetailsWithImage());

        // Initially hide save button
        btnSaveEdit.setVisible(false);

        buttonPanel.add(btnAdd);
        buttonPanel.add(btnEdit);
        buttonPanel.add(btnSaveEdit);
        buttonPanel.add(btnClear);
        buttonPanel.add(btnDelete);
        buttonPanel.add(btnDetails);

        rightPanel.add(formPanel, BorderLayout.CENTER);
        rightPanel.add(buttonPanel, BorderLayout.SOUTH);

        // Add panels to top panel
        topPanel.add(leftPanel);
        topPanel.add(rightPanel);

        // Bottom Panel: Book Table
        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setBackground(PANEL_BACKGROUND);
        tablePanel.setBorder(createStyledBorder("📚 BOOK CATALOG"));

        String[] bookColumns = {"ISBN", "Title", "Author", "Year", "Type", "Available", "Price"};
        bookTableModel = new DefaultTableModel(bookColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if(columnIndex==3)return Integer.class;
                if(columnIndex==6)return Double.class;
                return String.class;
            }
        };

        bookTable = createStyledTable(bookTableModel);
        bookTable.setRowHeight(30);

        JScrollPane scrollPane = new JScrollPane(bookTable);
        scrollPane.setBackground(PANEL_BACKGROUND);
        scrollPane.getViewport().setBackground(TABLE_BACKGROUND);
        scrollPane.getVerticalScrollBar().setUI(new CustomScrollBarUI());

        bookTable.getTableHeader().setDefaultRenderer(new CustomHeaderRenderer());

        tablePanel.add(scrollPane, BorderLayout.CENTER);

        // Add everything to main container
        mainContainer.add(topPanel, BorderLayout.NORTH);
        mainContainer.add(tablePanel, BorderLayout.CENTER);

        panel.add(mainContainer, BorderLayout.CENTER);

        return panel;
    }

    private void setDefaultBookCover() {
        int width = 170;
        int height = 210;
        BufferedImage defaultCover = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = defaultCover.createGraphics();

        // Draw book cover background
        GradientPaint gradient = new GradientPaint(0, 0, PRIMARY_COLOR, width, height, PRIMARY_GRADIENT);
        g2.setPaint(gradient);
        g2.fillRect(0, 0, width, height);

        // Draw book spine
        g2.setColor(new Color(0, 100, 200));
        g2.fillRect(width - 10, 0, 10, height);

        // Draw title placeholder
        g2.setColor(new Color(220, 240, 255));
        g2.setFont(new Font("Arial", Font.BOLD, 14));
        String titleText = "BOOK COVER";
        FontMetrics fm = g2.getFontMetrics();
        int titleX = (width - fm.stringWidth(titleText)) / 2;
        g2.drawString(titleText, titleX, height / 2 - 10);

        // Draw author placeholder
        g2.setFont(new Font("Arial", Font.ITALIC, 11));
        String authorText = "Add your cover";
        fm = g2.getFontMetrics();
        int authorX = (width - fm.stringWidth(authorText)) / 2;
        g2.drawString(authorText, authorX, height / 2 + 15);

        g2.dispose();

        ImageIcon icon = new ImageIcon(defaultCover);
        lblBookCover.setIcon(icon);
        currentImageFile = null;
        currentBookIsbnForImage = null;
    }

    private JPanel createMemberPanel() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBackground(PANEL_BACKGROUND);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Create main container with split layout
        JPanel mainContainer = new JPanel(new GridLayout(1, 2, 15, 0));
        mainContainer.setBackground(PANEL_BACKGROUND);

        // Left Panel: Member Photo and Management
        JPanel leftPanel = new JPanel(new BorderLayout(10, 10));
        leftPanel.setBackground(PANEL_BACKGROUND);

        // Member Photo Section
        JPanel photoPanel = new JPanel(new BorderLayout(5, 5));
        photoPanel.setBackground(new Color(28, 28, 38));
        photoPanel.setBorder(createStyledBorder("📸 MEMBER PHOTO"));
        photoPanel.setPreferredSize(new Dimension(250, 300));

        // Member photo label
        lblMemberPhoto = new JLabel();
        lblMemberPhoto.setHorizontalAlignment(SwingConstants.CENTER);
        lblMemberPhoto.setVerticalAlignment(SwingConstants.CENTER);
        lblMemberPhoto.setOpaque(true);
        lblMemberPhoto.setBackground(new Color(40, 40, 50));
        lblMemberPhoto.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(IMAGE_BORDER_COLOR, 1),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        lblMemberPhoto.setPreferredSize(new Dimension(180, 220));

        // Create default member photo
        setDefaultMemberPhoto();

        // Button panel for member photo
        JPanel memberPhotoButtonPanel = new JPanel(new GridLayout(1, 2, 5, 0));
        memberPhotoButtonPanel.setBackground(new Color(28, 28, 38));
        memberPhotoButtonPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        btnUploadMemberPhoto = createStyledButton("📁 UPLOAD", new Color(80, 180, 120));
        btnRemoveMemberPhoto = createStyledButton("🗑️ REMOVE", new Color(255, 100, 150));

        btnUploadMemberPhoto.addActionListener(e -> uploadMemberPhoto());
        btnRemoveMemberPhoto.addActionListener(e -> removeMemberPhoto());

        memberPhotoButtonPanel.add(btnUploadMemberPhoto);
        memberPhotoButtonPanel.add(btnRemoveMemberPhoto);

        photoPanel.add(lblMemberPhoto, BorderLayout.CENTER);
        photoPanel.add(memberPhotoButtonPanel, BorderLayout.SOUTH);

        leftPanel.add(photoPanel, BorderLayout.NORTH);

        // Right Panel: Member Form and Table
        JPanel rightPanel = new JPanel(new BorderLayout(10, 10));
        rightPanel.setBackground(PANEL_BACKGROUND);

        // Member input panel
        JPanel inputPanel = new JPanel(new GridBagLayout());
        inputPanel.setBackground(new Color(28, 28, 38));
        inputPanel.setBorder(createStyledBorder("👤 MEMBER MANAGEMENT"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 15, 10, 15);

        String[] labels = {"Member ID:*", "Name:*", "Email:", "Phone:"};
        JTextField[] fields = new JTextField[labels.length];

        for (int i = 0; i < labels.length; i++) {
            gbc.gridx = 0;
            gbc.gridy = i;
            gbc.gridwidth = 1;
            inputPanel.add(createStyledLabel(labels[i]), gbc);

            gbc.gridx = 1;
            gbc.gridwidth = 2;
            fields[i] = createStyledTextField();
            fields[i].setPreferredSize(new Dimension(200, 35));
            inputPanel.add(fields[i], gbc);
        }

        txtMemberId = fields[0];
        txtMemberName = fields[1];
        txtMemberEmail = fields[2];
        txtMemberPhone = fields[3];

        // Action buttons panel - UPDATED WITH EDIT BUTTONS
        JPanel buttonPanel = new JPanel(new GridLayout(2, 3, 10, 10));
        buttonPanel.setBackground(new Color(28, 28, 38));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(20, 15, 15, 15));

        JButton btnAdd = createGradientButton("➕ ADD MEMBER", PRIMARY_COLOR, PRIMARY_GRADIENT);
        JButton btnEdit = createGradientButton("✏️ EDIT", EDIT_COLOR, new Color(255, 200, 130));
        JButton btnSaveEdit = createGradientButton("💾 SAVE", SUCCESS_COLOR, new Color(120, 255, 200));
        JButton btnClear = createStyledButton("🧹 CLEAR", new Color(70, 70, 80));
        JButton btnDelete = createGradientButton("🗑️ DELETE", new Color(255, 80, 150), new Color(255, 120, 180));
        JButton btnDetails = createStyledButton("👁️ DETAILS", new Color(80, 120, 200));

        addMemberButton=btnAdd; editMemberButton=btnEdit; saveMemberButton=btnSaveEdit;
        btnAdd.addActionListener(e -> addNewMember());
        btnEdit.addActionListener(e -> editSelectedMember());
        btnSaveEdit.addActionListener(e -> saveMemberEdit());
        btnClear.addActionListener(e -> clearMemberFields());
        btnDelete.addActionListener(e -> deleteSelectedMember());
        btnDetails.addActionListener(e -> viewMemberDetailsWithPhoto());

        // Initially hide save button
        btnSaveEdit.setVisible(false);

        buttonPanel.add(btnAdd);
        buttonPanel.add(btnEdit);
        buttonPanel.add(btnSaveEdit);
        buttonPanel.add(btnClear);
        buttonPanel.add(btnDelete);
        buttonPanel.add(btnDetails);

        // Member table panel
        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setBackground(PANEL_BACKGROUND);
        tablePanel.setBorder(createStyledBorder("👥 MEMBER DIRECTORY"));

        String[] memberColumns = {"ID", "Name", "Email", "Phone", "Join Date", "Borrowed"};
        memberTableModel = new DefaultTableModel(memberColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        memberTable = createStyledTable(memberTableModel);
        memberTable.setRowHeight(30);

        // Add selection listener to load member photo
        memberTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int selectedRow = selectedModelRow(memberTable);
                if (selectedRow != -1 && memberTable == memberTable) {
                    String memberId = (String) memberTableModel.getValueAt(selectedRow, 0);
                    loadMemberPhoto(memberId);
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(memberTable);
        scrollPane.setBackground(PANEL_BACKGROUND);
        scrollPane.getViewport().setBackground(TABLE_BACKGROUND);
        scrollPane.getVerticalScrollBar().setUI(new CustomScrollBarUI());

        memberTable.getTableHeader().setDefaultRenderer(new CustomHeaderRenderer());

        tablePanel.add(scrollPane, BorderLayout.CENTER);

        // Add input and button panels to right container
        JPanel inputContainer = new JPanel(new BorderLayout());
        inputContainer.setBackground(PANEL_BACKGROUND);
        inputContainer.add(inputPanel, BorderLayout.NORTH);
        inputContainer.add(buttonPanel, BorderLayout.SOUTH);

        // Add everything to right panel
        JPanel rightContainer = new JPanel(new BorderLayout(10, 10));
        rightContainer.setBackground(PANEL_BACKGROUND);
        rightContainer.add(inputContainer, BorderLayout.NORTH);
        rightContainer.add(tablePanel, BorderLayout.CENTER);

        rightPanel.add(rightContainer, BorderLayout.CENTER);

        // Add both panels to main container
        mainContainer.add(leftPanel);
        mainContainer.add(rightPanel);

        panel.add(mainContainer, BorderLayout.CENTER);

        return panel;
    }

    private void setDefaultMemberPhoto() {
        int width = 170;
        int height = 210;
        BufferedImage defaultPhoto = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = defaultPhoto.createGraphics();

        // Draw member photo background
        GradientPaint gradient = new GradientPaint(0, 0, new Color(80, 120, 200), width, height, new Color(100, 140, 220));
        g2.setPaint(gradient);
        g2.fillRect(0, 0, width, height);

        // Draw circle for face
        g2.setColor(new Color(220, 200, 180));
        g2.fillOval(width/2 - 50, height/2 - 60, 100, 100);

        // Draw features
        g2.setColor(new Color(100, 70, 50));

        // Eyes
        g2.fillOval(width/2 - 25, height/2 - 20, 10, 10);
        g2.fillOval(width/2 + 15, height/2 - 20, 10, 10);

        // Smile
        g2.setStroke(new BasicStroke(3));
        g2.drawArc(width/2 - 20, height/2, 40, 20, 180, 180);

        // Draw title placeholder
        g2.setColor(new Color(220, 240, 255));
        g2.setFont(new Font("Arial", Font.BOLD, 12));
        String titleText = "MEMBER PHOTO";
        FontMetrics fm = g2.getFontMetrics();
        int titleX = (width - fm.stringWidth(titleText)) / 2;
        g2.drawString(titleText, titleX, height - 15);

        g2.dispose();

        ImageIcon icon = new ImageIcon(defaultPhoto);
        lblMemberPhoto.setIcon(icon);
        currentMemberImageFile = null;
        currentMemberIdForImage = null;
    }

    // ==================== BOOK EDIT FUNCTIONS ====================

    private void editSelectedBook() {
        int row=selectedModelRow(bookTable);
        if(row<0){showStyledWarningDialog("Select a book to edit.");return;}
        String id=(String)bookTableModel.getValueAt(row,0);
        Book book=libraryService.getBooks().stream().filter(b->b.getIsbn().equals(id)).findFirst().orElse(null);
        if(book==null){showStyledErrorDialog("Book not found.");return;}
        isEditingBook=true;editingBookIsbn=id;
        txtIsbn.setText(id);txtTitle.setText(book.getTitle());txtAuthor.setText(book.getAuthor());
        txtYear.setText(Integer.toString(book.getPublicationYear()));txtPrice.setText(Double.toString(book.getPrice()));
        cmbBookType.setSelectedItem(LibraryService.typeOf(book));txtIsbn.setEnabled(false);loadBookCover(id);
        addBookButton.setEnabled(false);editBookButton.setEnabled(false);saveBookButton.setVisible(true);
    }

    private void saveBookEdit() {
        if(!isEditingBook){showStyledWarningDialog("Select a book and choose Edit first.");return;}
        try {
            libraryService.updateBook(editingBookIsbn,(String)cmbBookType.getSelectedItem(),txtTitle.getText().trim(),
                txtAuthor.getText().trim(),Integer.parseInt(txtYear.getText().trim()),Double.parseDouble(txtPrice.getText().trim()));
            if(currentImageFile!=null)saveBookCover(editingBookIsbn);
            clearBookFields();refreshBookTable();updateStats();showStyledSuccessDialog("Book updated successfully.");
        } catch(Exception e){showStyledErrorDialog(e.getMessage());}
    }

    // ==================== MEMBER EDIT FUNCTIONS ====================

    private void editSelectedMember() {
        int row=selectedModelRow(memberTable);
        if(row<0){showStyledWarningDialog("Select a member to edit.");return;}
        String id=(String)memberTableModel.getValueAt(row,0);
        Member member=libraryService.getMembers().stream().filter(m->m.getId().equals(id)).findFirst().orElse(null);
        if(member==null){showStyledErrorDialog("Member not found.");return;}
        isEditingMember=true;editingMemberId=id;txtMemberId.setText(id);txtMemberName.setText(member.getName());
        txtMemberEmail.setText(member.getEmail());txtMemberPhone.setText(member.getPhone());txtMemberId.setEnabled(false);
        loadMemberPhoto(id);addMemberButton.setEnabled(false);editMemberButton.setEnabled(false);saveMemberButton.setVisible(true);
    }

    private void saveMemberEdit() {
        if(!isEditingMember){showStyledWarningDialog("Select a member and choose Edit first.");return;}
        try {
            libraryService.updateMember(editingMemberId,txtMemberName.getText().trim(),txtMemberEmail.getText().trim(),txtMemberPhone.getText().trim());
            if(currentMemberImageFile!=null)saveMemberPhoto(editingMemberId);
            clearMemberFields();refreshMemberTable();updateStats();showStyledSuccessDialog("Member updated. Join date and loan history were preserved.");
        } catch(Exception e){showStyledErrorDialog(e.getMessage());}
    }

    // ==================== REST OF THE METHODS ====================

    private void uploadMemberPhoto() {
        // Check if we have a member selected or ID entered
        String memberId = txtMemberId.getText().trim();
        int selectedRow = selectedModelRow(memberTable);

        if (selectedRow == -1 && memberId.isEmpty()) {
            int option = JOptionPane.showConfirmDialog(this,
                "No member selected or ID entered. Would you like to upload the photo now?\n" +
                "The photo will be saved when you add the member with the ID.",
                "Upload Member Photo",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

            if (option != JOptionPane.YES_OPTION) {
                return;
            }
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select Member Photo");

        // Set file filter for images
        fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
            @Override
            public boolean accept(File f) {
                if (f.isDirectory()) return true;
                String name = f.getName().toLowerCase();
                return name.endsWith(".jpg") || name.endsWith(".jpeg") ||
                       name.endsWith(".png") || name.endsWith(".gif") ||
                       name.endsWith(".bmp");
            }

            @Override
            public String getDescription() {
                return "Image Files (*.jpg, *.jpeg, *.png, *.gif, *.bmp)";
            }
        });

        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            currentMemberImageFile = fileChooser.getSelectedFile();

            try {
                // Load and scale the image
                Image originalImage = services.ImageStore.read(currentMemberImageFile.toPath());
                if (originalImage == null) {
                    showStyledErrorDialog("Could not load image. Unsupported format.");
                    return;
                }

                // Scale image to fit label
                int labelWidth = lblMemberPhoto.getWidth() - 10;
                int labelHeight = lblMemberPhoto.getHeight() - 10;

                if (labelWidth <= 0) labelWidth = 170;
                if (labelHeight <= 0) labelHeight = 210;

                Image scaledImage = originalImage.getScaledInstance(
                    labelWidth, labelHeight, Image.SCALE_SMOOTH);

                ImageIcon icon = new ImageIcon(scaledImage);
                lblMemberPhoto.setIcon(icon);

                // Store member ID if available
                if (!memberId.isEmpty()) {
                    currentMemberIdForImage = memberId;
                    showStyledSuccessDialog("Member photo loaded successfully for ID: " + memberId + "\nPhoto will be saved when you add/update the member.");
                } else if (selectedRow != -1) {
                    currentMemberIdForImage = (String) memberTableModel.getValueAt(selectedRow, 0);
                    // Save immediately for existing members
                    saveMemberPhoto(currentMemberIdForImage);
                    showStyledSuccessDialog("Member photo updated successfully for selected member!");
                } else {
                    showStyledSuccessDialog("Member photo loaded. Enter ID and add member to save.");
                }

            } catch (IOException e) {
                showStyledErrorDialog("Error loading image: " + e.getMessage());
                setDefaultMemberPhoto();
            } catch (Exception e) {
                showStyledErrorDialog("Error: " + e.getMessage());
                setDefaultMemberPhoto();
            }
        }
    }

    private void removeMemberPhoto() {
        try {
            int row=selectedModelRow(memberTable);
            if(row>=0)services.ImageStore.remove(java.nio.file.Paths.get(MEMBER_IMAGES_DIR),(String)memberTableModel.getValueAt(row,0));
            currentMemberImageFile=null;currentMemberIdForImage=null;setDefaultMemberPhoto();showStyledSuccessDialog("Image removed/cleared.");
        } catch(Exception e){showStyledErrorDialog("Image removal failed: "+e.getMessage());}
    }

    private void saveMemberPhoto(String memberId) {
        if(currentMemberImageFile==null)return;
        try {services.ImageStore.store(currentMemberImageFile.toPath(),java.nio.file.Paths.get(MEMBER_IMAGES_DIR),memberId);}
        catch(Exception e) {throw new IllegalStateException("Record saved, but its image was not saved: "+e.getMessage(),e);}
    }

    private void loadMemberPhoto(String memberId) {
        if (memberId == null || memberId.isEmpty()) {
            setDefaultMemberPhoto();
            return;
        }

        // Try to find image with different extensions
        String[] extensions = {".jpg", ".jpeg", ".png", ".gif", ".bmp"};
        File imageFile = null;

        for (String ext : extensions) {
            File testFile = new File(MEMBER_IMAGES_DIR + memberId + ext);
            if (testFile.exists()) {
                imageFile = testFile;
                break;
            }
        }

        if (imageFile != null && imageFile.exists()) {
            try {
                Image originalImage = services.ImageStore.read(imageFile.toPath());
                if (originalImage != null) {
                    // Scale image to fit label
                    int labelWidth = lblMemberPhoto.getWidth() - 10;
                    int labelHeight = lblMemberPhoto.getHeight() - 10;

                    if (labelWidth <= 0) labelWidth = 170;
                    if (labelHeight <= 0) labelHeight = 210;

                    Image scaledImage = originalImage.getScaledInstance(
                        labelWidth, labelHeight, Image.SCALE_SMOOTH);

                    ImageIcon icon = new ImageIcon(scaledImage);
                    lblMemberPhoto.setIcon(icon);
                    currentMemberImageFile = imageFile;
                    currentMemberIdForImage = memberId;
                    return;
                }
            } catch (IOException e) {
                System.err.println("Error loading member photo: " + e.getMessage());
            }
        }

        // If no image found or error occurred, set default
        setDefaultMemberPhoto();
    }

    private void deleteMemberPhoto(String memberId) {
        if(memberId==null || memberId.isEmpty())return;
        try {services.ImageStore.remove(java.nio.file.Paths.get(MEMBER_IMAGES_DIR),memberId);}
        catch(Exception e){throw new IllegalStateException("Record change completed, but the image could not be removed: "+e.getMessage(),e);}
    }

    private void viewMemberDetailsWithPhoto() {
        int selectedRow = selectedModelRow(memberTable);
        if (selectedRow == -1) {
            showStyledWarningDialog("Please select a member to view details!");
            return;
        }

        String memberId = (String) memberTableModel.getValueAt(selectedRow, 0);
        String memberName = (String) memberTableModel.getValueAt(selectedRow, 1);

        Member member = null;
        List<Member> members = libraryService.getMembers();
        for (Member m : members) {
            if (m.getId().equals(memberId)) {
                member = m;
                break;
            }
        }

        if (member == null) {
            showStyledErrorDialog("Member not found in system!");
            return;
        }

        // Create a panel with photo and details
        JPanel mainPanel = new JPanel(new BorderLayout(20, 20));
        mainPanel.setBackground(new Color(30, 30, 40));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Left panel for member photo
        JPanel photoPanel = new JPanel(new BorderLayout());
        photoPanel.setBackground(new Color(30, 30, 40));
        photoPanel.setBorder(createStyledBorder("📸 MEMBER PHOTO"));
        photoPanel.setPreferredSize(new Dimension(300, 400));

        JLabel photoLabel = new JLabel();
        photoLabel.setHorizontalAlignment(SwingConstants.CENTER);
        photoLabel.setVerticalAlignment(SwingConstants.CENTER);
        photoLabel.setOpaque(true);
        photoLabel.setBackground(new Color(40, 40, 50));
        photoLabel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(IMAGE_BORDER_COLOR, 2),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        photoLabel.setPreferredSize(new Dimension(250, 300));

        // Load member photo
        loadMemberPhotoForDialog(memberId, photoLabel);

        photoPanel.add(photoLabel, BorderLayout.CENTER);

        // Photo buttons
        JPanel photoButtonPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        photoButtonPanel.setBackground(new Color(30, 30, 40));
        photoButtonPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JButton btnUploadNew = createStyledButton("📁 UPLOAD NEW", new Color(80, 180, 120));
        JButton btnRemoveCurrent = createStyledButton("🗑️ REMOVE", new Color(255, 100, 150));

        btnUploadNew.addActionListener(e -> {
            uploadMemberPhotoForMember(memberId, photoLabel);
        });

        btnRemoveCurrent.addActionListener(e -> {
            deleteMemberPhoto(memberId);
            setDefaultMemberPhotoForDialog(photoLabel);
            showStyledSuccessDialog("Member photo removed.");
        });

        photoButtonPanel.add(btnUploadNew);
        photoButtonPanel.add(btnRemoveCurrent);

        photoPanel.add(photoButtonPanel, BorderLayout.SOUTH);

        // Right panel for member details
        JPanel detailsPanel = new JPanel(new BorderLayout());
        detailsPanel.setBackground(new Color(30, 30, 40));
        detailsPanel.setBorder(createStyledBorder("👤 MEMBER DETAILS"));

        StringBuilder details = new StringBuilder();
        details.append("Member Details\n");
        details.append("==============\n\n");
        details.append("ID: ").append(member.getId()).append("\n\n");
        details.append("Name: ").append(member.getName()).append("\n\n");
        details.append("Email: ").append(member.getEmail()).append("\n\n");
        details.append("Phone: ").append(member.getPhone()).append("\n\n");
        details.append("Join Date: ").append(member.getJoinDate()).append("\n\n");
        details.append("Borrowed Books: ").append(member.getBorrowedBooks())
                .append("/").append(Member.getMaxBorrowLimit()).append("\n\n");

        List<Transaction> transactions = libraryService.getTransactions();
        List<Transaction> activeBorrowings = new ArrayList<>();

        for (Transaction t : transactions) {
            if (t.getMemberId().equals(memberId) && !t.isReturned()) {
                activeBorrowings.add(t);
            }
        }

        if (!activeBorrowings.isEmpty()) {
            details.append("📚 CURRENTLY BORROWED BOOKS:\n");
            details.append("=".repeat(40)).append("\n");

            for (Transaction t : activeBorrowings) {
                details.append("• Book ISBN: ").append(t.getBookIsbn()).append("\n");
                details.append("  Borrow Date: ").append(t.getBorrowDate()).append("\n");
                details.append("  Due Date: ").append(t.getDueDate()).append("\n");

                if (t.isOverdue()) {
                    details.append("  ⚠️ OVERDUE! Fine: $")
                            .append(String.format("%.2f", t.getFineAmount())).append("\n");
                }
                details.append("\n");
            }
        } else {
            details.append("📚 No currently borrowed books.\n");
        }

        List<Transaction> allMemberTransactions = new ArrayList<>();
        for (Transaction t : transactions) {
            if (t.getMemberId().equals(memberId)) {
                allMemberTransactions.add(t);
            }
        }

        if (!allMemberTransactions.isEmpty()) {
            details.append("\n📊 BORROWING HISTORY:\n");
            details.append("=".repeat(40)).append("\n");
            int totalBorrowed = allMemberTransactions.size();
            int returnedCount = 0;
            int overdueCount = 0;

            for (Transaction t : allMemberTransactions) {
                if (t.isReturned()) returnedCount++;
                if (t.isOverdue()) overdueCount++;
            }

            details.append("Total Books Borrowed: ").append(totalBorrowed).append("\n");
            details.append("Books Returned: ").append(returnedCount).append("\n");
            details.append("Overdue Incidents: ").append(overdueCount).append("\n");
        }

        JTextArea textArea = new JTextArea(details.toString());
        textArea.setEditable(false);
        textArea.setBackground(new Color(30, 30, 40));
        textArea.setForeground(TEXT_COLOR);
        textArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        textArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        detailsPanel.add(new JScrollPane(textArea), BorderLayout.CENTER);

        mainPanel.add(photoPanel, BorderLayout.WEST);
        mainPanel.add(detailsPanel, BorderLayout.CENTER);

        JOptionPane.showMessageDialog(this, mainPanel,
            "Member Details: " + memberName, JOptionPane.INFORMATION_MESSAGE);
    }

    private void loadMemberPhotoForDialog(String memberId, JLabel photoLabel) {
        if (memberId == null || memberId.isEmpty()) {
            setDefaultMemberPhotoForDialog(photoLabel);
            return;
        }

        String[] extensions = {".jpg", ".jpeg", ".png", ".gif", ".bmp"};
        File imageFile = null;

        for (String ext : extensions) {
            File testFile = new File(MEMBER_IMAGES_DIR + memberId + ext);
            if (testFile.exists()) {
                imageFile = testFile;
                break;
            }
        }

        if (imageFile != null && imageFile.exists()) {
            try {
                Image originalImage = services.ImageStore.read(imageFile.toPath());
                if (originalImage != null) {
                    Image scaledImage = originalImage.getScaledInstance(
                        200, 250, Image.SCALE_SMOOTH);

                    ImageIcon icon = new ImageIcon(scaledImage);
                    photoLabel.setIcon(icon);
                    return;
                }
            } catch (IOException e) {
                System.err.println("Error loading member photo: " + e.getMessage());
            }
        }

        setDefaultMemberPhotoForDialog(photoLabel);
    }

    private void setDefaultMemberPhotoForDialog(JLabel photoLabel) {
        int width = 200;
        int height = 250;
        BufferedImage defaultPhoto = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = defaultPhoto.createGraphics();

        GradientPaint gradient = new GradientPaint(0, 0, new Color(80, 120, 200), width, height, new Color(100, 140, 220));
        g2.setPaint(gradient);
        g2.fillRect(0, 0, width, height);

        // Draw circle for face
        g2.setColor(new Color(220, 200, 180));
        g2.fillOval(width/2 - 60, height/2 - 70, 120, 120);

        // Draw features
        g2.setColor(new Color(100, 70, 50));

        // Eyes
        g2.fillOval(width/2 - 30, height/2 - 20, 15, 15);
        g2.fillOval(width/2 + 15, height/2 - 20, 15, 15);

        // Smile
        g2.setStroke(new BasicStroke(4));
        g2.drawArc(width/2 - 25, height/2 + 10, 50, 25, 180, 180);

        g2.setColor(new Color(220, 240, 255));
        g2.setFont(new Font("Arial", Font.BOLD, 16));
        String titleText = "NO PHOTO";
        FontMetrics fm = g2.getFontMetrics();
        int titleX = (width - fm.stringWidth(titleText)) / 2;
        g2.drawString(titleText, titleX, height - 20);

        g2.setFont(new Font("Arial", Font.ITALIC, 12));
        String text = "Upload a photo";
        fm = g2.getFontMetrics();
        int textX = (width - fm.stringWidth(text)) / 2;
        g2.drawString(text, textX, height - 5);

        g2.dispose();

        ImageIcon icon = new ImageIcon(defaultPhoto);
        photoLabel.setIcon(icon);
    }

    private void uploadMemberPhotoForMember(String memberId, JLabel photoLabel) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select Member Photo");

        fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
            @Override
            public boolean accept(File f) {
                if (f.isDirectory()) return true;
                String name = f.getName().toLowerCase();
                return name.endsWith(".jpg") || name.endsWith(".jpeg") ||
                       name.endsWith(".png") || name.endsWith(".gif") ||
                       name.endsWith(".bmp");
            }

            @Override
            public String getDescription() {
                return "Image Files (*.jpg, *.jpeg, *.png, *.gif, *.bmp)";
            }
        });

        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File imageFile = fileChooser.getSelectedFile();

            try {
                Image originalImage = services.ImageStore.read(imageFile.toPath());
                if (originalImage == null) {
                    showStyledErrorDialog("Could not load image. Unsupported format.");
                    return;
                }

                // Save the image
                services.ImageStore.store(imageFile.toPath(),java.nio.file.Paths.get(MEMBER_IMAGES_DIR),memberId);

                // Update the display
                Image scaledImage = originalImage.getScaledInstance(
                    200, 250, Image.SCALE_SMOOTH);

                ImageIcon icon = new ImageIcon(scaledImage);
                photoLabel.setIcon(icon);

                // Also update the main member photo panel
                loadMemberPhoto(memberId);

                showStyledSuccessDialog("Member photo updated successfully!");

            } catch (IOException e) {
                showStyledErrorDialog("Error loading image: " + e.getMessage());
            } catch (Exception e) {
                showStyledErrorDialog("Error: " + e.getMessage());
            }
        }
    }

    private JPanel createTransactionPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(PANEL_BACKGROUND);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel topPanel = new JPanel(new GridLayout(1, 2, 20, 20));
        topPanel.setBackground(PANEL_BACKGROUND);

        topPanel.add(createTransactionCard("🔄 BORROW BOOK",
            "Enter member ID and book ISBN to borrow",
            "BORROW", true));
        topPanel.add(createTransactionCard("📚 RETURN BOOK",
            "Enter member ID and book ISBN to return",
            "RETURN", false));

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBackground(PANEL_BACKGROUND);

        txtTransactionStatus = new JTextArea();
        txtTransactionStatus.setBackground(new Color(30, 30, 40));
        txtTransactionStatus.setForeground(TEXT_COLOR);
        txtTransactionStatus.setFont(new Font("Consolas", Font.PLAIN, 12));
        txtTransactionStatus.setEditable(false);
        txtTransactionStatus.setCaretColor(PRIMARY_COLOR);
        txtTransactionStatus.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JScrollPane scrollPane = new JScrollPane(txtTransactionStatus);
        scrollPane.setBackground(PANEL_BACKGROUND);
        scrollPane.setBorder(createStyledBorder("📜 TRANSACTION LOG"));
        scrollPane.getVerticalScrollBar().setUI(new CustomScrollBarUI());

        JPanel logButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        logButtonPanel.setBackground(new Color(30, 30, 40));

        JButton btnClear = createStyledButton("Clear Log", new Color(70, 70, 80));
        JButton btnExport = createStyledButton("Export Log", new Color(80, 180, 120));
        JButton btnOverdue = createStyledButton("View Overdue", new Color(255, 100, 150));

        btnClear.addActionListener(e -> txtTransactionStatus.setText(""));
        btnExport.addActionListener(e -> exportLog());
        btnOverdue.addActionListener(e -> viewOverdueBooks());

        logButtonPanel.add(btnClear);
        logButtonPanel.add(btnExport);
        logButtonPanel.add(btnOverdue);

        bottomPanel.add(scrollPane, BorderLayout.CENTER);
        bottomPanel.add(logButtonPanel, BorderLayout.SOUTH);

        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(bottomPanel, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createTransactionCard(String title, String description, String buttonText, boolean isBorrow) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(new Color(28, 28, 38));
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(50, 50, 60), 1),
            BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setForeground(PRIMARY_COLOR);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        JLabel descLabel = new JLabel(description);
        descLabel.setForeground(SUBTLE_TEXT);
        descLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        descLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));

        JPanel inputPanel = new JPanel(new GridLayout(2, 2, 10, 10));
        inputPanel.setBackground(new Color(28, 28, 38));

        inputPanel.add(createStyledLabel("Member ID:"));
        JTextField txtMemberField = createStyledTextField();
        inputPanel.add(txtMemberField);

        inputPanel.add(createStyledLabel("Book ISBN:"));
        JTextField txtBookField = createStyledTextField();
        inputPanel.add(txtBookField);

        JButton btnAction = createGradientButton(buttonText,
            isBorrow ? PRIMARY_COLOR : new Color(80, 180, 120),
            isBorrow ? PRIMARY_GRADIENT : new Color(100, 200, 140));
        btnAction.setPreferredSize(new Dimension(150, 40));

        if (isBorrow) {
            txtBorrowMemberId = txtMemberField;
            txtBorrowIsbn = txtBookField;
            btnAction.addActionListener(e -> borrowBookAction());
        } else {
            txtReturnMemberId = txtMemberField;
            txtReturnIsbn = txtBookField;
            btnAction.addActionListener(e -> returnBookAction());
        }

        JPanel buttonContainer = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonContainer.setBackground(new Color(28, 28, 38));
        buttonContainer.add(btnAction);

        JPanel content = new JPanel(new BorderLayout(0, 15));
        content.setBackground(new Color(28, 28, 38));
        content.add(titleLabel, BorderLayout.NORTH);
        content.add(descLabel, BorderLayout.CENTER);
        content.add(inputPanel, BorderLayout.SOUTH);

        card.add(content, BorderLayout.CENTER);
        card.add(buttonContainer, BorderLayout.SOUTH);

        return card;
    }

    private JPanel createSearchPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(PANEL_BACKGROUND);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel searchPanel = new JPanel(new BorderLayout(10, 10));
        searchPanel.setBackground(new Color(28, 28, 38));
        searchPanel.setBorder(createStyledBorder("🔍 ADVANCED SEARCH"));

        JPanel inputArea = new JPanel(new GridBagLayout());
        inputArea.setBackground(new Color(28, 28, 38));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 10, 10, 10);

        gbc.gridx = 0;
        gbc.gridy = 0;
        inputArea.add(createStyledLabel("Search For:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        JTextField txtSearch = createStyledTextField();
        txtSearch.setPreferredSize(new Dimension(300, 40));
        inputArea.add(txtSearch, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        JButton btnSearch = createGradientButton("🔍 SEARCH", PRIMARY_COLOR, PRIMARY_GRADIENT);
        btnSearch.setPreferredSize(new Dimension(120, 40));
        inputArea.add(btnSearch, gbc);

        JPanel optionsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 10));
        optionsPanel.setBackground(new Color(28, 28, 38));

        optionsPanel.add(createStyledLabel("Search By:"));
        JComboBox<String> cmbSearchType = createStyledComboBox(
            new String[]{"All Fields", "Title", "Author", "ISBN", "Genre", "Subject"}
        );
        cmbSearchType.setPreferredSize(new Dimension(150, 35));
        optionsPanel.add(cmbSearchType);

        optionsPanel.add(createStyledLabel("Availability:"));
        JComboBox<String> cmbAvailability = createStyledComboBox(
            new String[]{"All", "Available Only", "Borrowed Only"}
        );
        cmbAvailability.setPreferredSize(new Dimension(150, 35));
        optionsPanel.add(cmbAvailability);

        searchPanel.add(inputArea, BorderLayout.NORTH);
        searchPanel.add(optionsPanel, BorderLayout.CENTER);

        JTextArea txtResults = new JTextArea();
        txtResults.setBackground(new Color(30, 30, 40));
        txtResults.setForeground(TEXT_COLOR);
        txtResults.setFont(new Font("Consolas", Font.PLAIN, 12));
        txtResults.setEditable(false);
        txtResults.setCaretColor(PRIMARY_COLOR);

        JScrollPane resultsScroll = new JScrollPane(txtResults);
        resultsScroll.setBackground(PANEL_BACKGROUND);
        resultsScroll.setBorder(createStyledBorder("📊 SEARCH RESULTS"));
        resultsScroll.getVerticalScrollBar().setUI(new CustomScrollBarUI());

        btnSearch.addActionListener(e -> {
            String query = txtSearch.getText();
            String searchType = (String) cmbSearchType.getSelectedItem();
            performSearchWithDebug(query, searchType, txtResults);
        });

        txtSearch.addActionListener(e -> btnSearch.doClick());

        panel.add(searchPanel, BorderLayout.NORTH);
        panel.add(resultsScroll, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createDashboardPanel() {
        JPanel panel = new JPanel(new BorderLayout(20, 20));
        panel.setBackground(PANEL_BACKGROUND);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel statsPanel = new JPanel(new GridLayout(2, 2, 15, 15));
        statsPanel.setBackground(PANEL_BACKGROUND);

        statsPanel.add(createStatCard("📚 Total Books", "0", PRIMARY_COLOR));
        statsPanel.add(createStatCard("👥 Total Members", "0", new Color(100, 200, 140)));
        statsPanel.add(createStatCard("✅ Available Books", "0", SUCCESS_COLOR));
        statsPanel.add(createStatCard("⚠️ Overdue Books", "0", ERROR_COLOR));

        Component[] statsCards = statsPanel.getComponents();
        lblTotalBooks = ((JLabel) ((JPanel) statsCards[0]).getComponent(1));
        lblTotalMembers = ((JLabel) ((JPanel) statsCards[1]).getComponent(1));
        lblAvailableBooks = ((JLabel) ((JPanel) statsCards[2]).getComponent(1));
        lblOverdueBooks = ((JLabel) ((JPanel) statsCards[3]).getComponent(1));

        JPanel actionsPanel = new JPanel(new GridLayout(2, 3, 15, 15));
        actionsPanel.setBackground(PANEL_BACKGROUND);
        actionsPanel.setBorder(createStyledBorder("⚡ QUICK ACTIONS"));

        String[] actions = {"📖 Add New Book", "👤 Add New Member", "🔄 Quick Borrow",
                           "📊 Generate Report", "🧹 Clear All Logs", "⚙️ Settings"};

        for (String action : actions) {
            actionsPanel.add(createActionButton(action));
        }

        panel.add(statsPanel, BorderLayout.NORTH);
        panel.add(actionsPanel, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createStatCard(String title, String value, Color color) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(new Color(28, 28, 38));
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(50, 50, 60), 1),
            BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setForeground(SUBTLE_TEXT);
        titleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        JLabel valueLabel = new JLabel(value);
        valueLabel.setForeground(color);
        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        valueLabel.setHorizontalAlignment(SwingConstants.CENTER);

        card.add(titleLabel, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);

        return card;
    }

    private JButton createActionButton(String text) {
        JButton button = new JButton(text);
        button.setBackground(new Color(35, 35, 45));
        button.setForeground(TEXT_COLOR);
        button.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(60, 60, 70), 1),
            BorderFactory.createEmptyBorder(15, 10, 15, 10)
        ));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(new Color(45, 45, 55));
                button.setForeground(HIGHLIGHT_COLOR);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(new Color(35, 35, 45));
                button.setForeground(TEXT_COLOR);
            }
        });

        button.addActionListener(e -> {
            switch (text) {
                case "📖 Add New Book":
                    tabbedPane.setSelectedIndex(0);
                    break;
                case "👤 Add New Member":
                    tabbedPane.setSelectedIndex(1);
                    break;
                case "🔄 Quick Borrow":
                    tabbedPane.setSelectedIndex(2);
                    break;
                case "📊 Generate Report":
                    generateReport();
                    break;
                case "🧹 Clear All Logs":
                    txtTransactionStatus.setText("");
                    break;
                case "⚙️ Settings":
                    showSettingsDialog();
                    break;
            }
        });

        return button;
    }

    // Helper Methods
    private Border createStyledBorder(String title) {
        TitledBorder border = BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(50, 50, 60), 2),
            title
        );
        border.setTitleColor(PRIMARY_COLOR);
        border.setTitleFont(new Font("Segoe UI", Font.BOLD, 14));
        border.setTitleJustification(TitledBorder.CENTER);
        return border;
    }

    private JLabel createStyledLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(TEXT_COLOR);
        label.setFont(new Font("Segoe UI", Font.BOLD, 12));
        return label;
    }

    private JTextField createStyledTextField() {
        JTextField field = new JTextField();
        field.setBackground(INPUT_BACKGROUND);
        field.setForeground(TEXT_COLOR);
        field.setCaretColor(PRIMARY_COLOR);
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(60, 60, 70), 1),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        field.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                field.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(PRIMARY_COLOR, 2),
                    BorderFactory.createEmptyBorder(7, 11, 7, 11)
                ));
                field.setBackground(new Color(35, 35, 45));
            }

            @Override
            public void focusLost(FocusEvent e) {
                field.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(60, 60, 70), 1),
                    BorderFactory.createEmptyBorder(8, 12, 8, 12)
                ));
                field.setBackground(INPUT_BACKGROUND);
            }
        });

        return field;
    }

    private JComboBox<String> createStyledComboBox(String[] items) {
        JComboBox<String> combo = new JComboBox<>(items);
        combo.setBackground(INPUT_BACKGROUND);
        combo.setForeground(TEXT_COLOR);
        combo.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(60, 60, 70), 1),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        combo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                    int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                setBackground(isSelected ? HIGHLIGHT_COLOR : INPUT_BACKGROUND);
                setForeground(isSelected ? new Color(20, 20, 30) : TEXT_COLOR);
                return this;
            }
        });
        return combo;
    }

    private JButton createStyledButton(String text, Color bgColor) {
        JButton button = new JButton(text);
        button.setBackground(bgColor);
        button.setForeground(new Color(20, 20, 30));
        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(bgColor.darker(), 1),
            BorderFactory.createEmptyBorder(10, 20, 10, 20)
        ));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setFocusPainted(false);

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(bgColor.brighter());
                button.setForeground(new Color(20, 20, 30));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(bgColor);
                button.setForeground(new Color(20, 20, 30));
            }
        });

        return button;
    }

    private JButton createGradientButton(String text, Color color1, Color color2) {
        return new GradientButton(text, color1, color2);
    }

    private JTable createStyledTable(DefaultTableModel model) {
        JTable table = new JTable(model) {
            @Override
            public Component prepareRenderer(javax.swing.table.TableCellRenderer renderer,
                    int row, int column) {
                Component c = super.prepareRenderer(renderer, row, column);

                if (isRowSelected(row)) {
                    c.setBackground(HIGHLIGHT_COLOR);
                    c.setForeground(new Color(20, 20, 30));
                } else {
                    if (row % 2 == 0) {
                        c.setBackground(new Color(30, 30, 40));
                    } else {
                        c.setBackground(new Color(35, 35, 45));
                    }
                    c.setForeground(TEXT_COLOR);
                }

                if (column == 5 && this == bookTable) {
                    String status = getValueAt(row, column).toString();
                    if (status.equals("❌ No") || status.equals("No")) {
                        c.setForeground(ERROR_COLOR);
                        c.setFont(new Font("Segoe UI", Font.BOLD, 11));
                    } else {
                        c.setForeground(SUCCESS_COLOR);
                    }
                }

                if (column == 6 && this == bookTable) {
                    c.setForeground(PRICE_COLOR);
                    if(c instanceof JLabel && getValueAt(row,column) instanceof Number)
                        ((JLabel)c).setText(String.format(java.util.Locale.ROOT,"$%.2f",((Number)getValueAt(row,column)).doubleValue()));
                }

                return c;
            }
        };

        table.setAutoCreateRowSorter(true);
        table.setBackground(TABLE_BACKGROUND);
        table.setForeground(TEXT_COLOR);
        table.setGridColor(new Color(50, 50, 60));
        table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        table.setRowHeight(25);
        table.setSelectionBackground(HIGHLIGHT_COLOR);
        table.setSelectionForeground(new Color(20, 20, 30));
        table.setShowGrid(true);
        table.setIntercellSpacing(new Dimension(1, 1));

        return table;
    }

    // Custom UI Classes
    class GradientButton extends JButton {
        private Color color1, color2;

        public GradientButton(String text, Color color1, Color color2) {
            super(text);
            this.color1 = color1;
            this.color2 = color2;
            setContentAreaFilled(false);
            setForeground(new Color(20, 20, 30));
            setFont(new Font("Segoe UI", Font.BOLD, 13));
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setFocusPainted(false);
            setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            if (getModel().isPressed()) {
                g2.setPaint(new GradientPaint(0, 0, color2.darker(), 0, getHeight(), color1.darker()));
            } else if (getModel().isRollover()) {
                g2.setPaint(new GradientPaint(0, 0, color1.brighter(), 0, getHeight(), color2.brighter()));
            } else {
                g2.setPaint(new GradientPaint(0, 0, color1, 0, getHeight(), color2));
            }

            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
            g2.dispose();

            super.paintComponent(g);
        }
    }

    class CustomScrollBarUI extends javax.swing.plaf.basic.BasicScrollBarUI {
        @Override
        protected void configureScrollBarColors() {
            this.thumbColor = new Color(60, 60, 70);
            this.thumbHighlightColor = new Color(80, 80, 90);
            this.thumbDarkShadowColor = new Color(40, 40, 50);
            this.thumbLightShadowColor = new Color(70, 70, 80);
            this.trackColor = new Color(30, 30, 40);
            this.trackHighlightColor = new Color(50, 50, 60);
        }

        @Override
        protected JButton createDecreaseButton(int orientation) {
            return createInvisibleButton();
        }

        @Override
        protected JButton createIncreaseButton(int orientation) {
            return createInvisibleButton();
        }

        private JButton createInvisibleButton() {
            JButton button = new JButton();
            button.setPreferredSize(new Dimension(0, 0));
            button.setMinimumSize(new Dimension(0, 0));
            button.setMaximumSize(new Dimension(0, 0));
            return button;
        }

        @Override
        protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setColor(thumbColor);
            g2.fillRoundRect(thumbBounds.x + 2, thumbBounds.y + 2,
                           thumbBounds.width - 4, thumbBounds.height - 4, 5, 5);
            g2.dispose();
        }

        @Override
        protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
            g.setColor(trackColor);
            g.fillRect(trackBounds.x, trackBounds.y, trackBounds.width, trackBounds.height);
        }
    }

    class CustomHeaderRenderer extends JLabel implements javax.swing.table.TableCellRenderer {
        public CustomHeaderRenderer() {
            setOpaque(true);
            setBackground(TABLE_HEADER_COLOR);
            setForeground(TEXT_COLOR);
            setFont(new Font("Segoe UI", Font.BOLD, 12));
            setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 2, 1, new Color(50, 50, 60)),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
            ));
            setHorizontalAlignment(SwingConstants.CENTER);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            setText(value.toString());
            return this;
        }
    }

    // Business Logic Methods

    private void uploadBookCover() {
        // Check if we have a book selected or ISBN entered
        String isbn = txtIsbn.getText().trim();
        int selectedRow = selectedModelRow(bookTable);

        if (selectedRow == -1 && isbn.isEmpty()) {
            int option = JOptionPane.showConfirmDialog(this,
                "No book selected or ISBN entered. Would you like to upload the cover now?\n" +
                "The cover will be saved when you add the book with the ISBN.",
                "Upload Book Cover",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

            if (option != JOptionPane.YES_OPTION) {
                return;
            }
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select Book Cover Image");

        // Set file filter for images
        fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
            @Override
            public boolean accept(File f) {
                if (f.isDirectory()) return true;
                String name = f.getName().toLowerCase();
                return name.endsWith(".jpg") || name.endsWith(".jpeg") ||
                       name.endsWith(".png") || name.endsWith(".gif") ||
                       name.endsWith(".bmp");
            }

            @Override
            public String getDescription() {
                return "Image Files (*.jpg, *.jpeg, *.png, *.gif, *.bmp)";
            }
        });

        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            currentImageFile = fileChooser.getSelectedFile();

            try {
                // Load and scale the image
                Image originalImage = services.ImageStore.read(currentImageFile.toPath());
                if (originalImage == null) {
                    showStyledErrorDialog("Could not load image. Unsupported format.");
                    return;
                }

                // Scale image to fit label
                int labelWidth = lblBookCover.getWidth() - 10;
                int labelHeight = lblBookCover.getHeight() - 10;

                if (labelWidth <= 0) labelWidth = 170;
                if (labelHeight <= 0) labelHeight = 210;

                Image scaledImage = originalImage.getScaledInstance(
                    labelWidth, labelHeight, Image.SCALE_SMOOTH);

                ImageIcon icon = new ImageIcon(scaledImage);
                lblBookCover.setIcon(icon);

                // Store ISBN if available
                if (!isbn.isEmpty()) {
                    currentBookIsbnForImage = isbn;
                    showStyledSuccessDialog("Cover image loaded successfully for ISBN: " + isbn + "\nImage will be saved when you add the book.");
                } else if (selectedRow != -1) {
                    currentBookIsbnForImage = (String) bookTableModel.getValueAt(selectedRow, 0);
                    // Save immediately for existing books
                    saveBookCover(currentBookIsbnForImage);
                    showStyledSuccessDialog("Cover image updated successfully for selected book!");
                } else {
                    showStyledSuccessDialog("Cover image loaded. Enter ISBN and add book to save.");
                }

            } catch (IOException e) {
                showStyledErrorDialog("Error loading image: " + e.getMessage());
                setDefaultBookCover();
            } catch (Exception e) {
                showStyledErrorDialog("Error: " + e.getMessage());
                setDefaultBookCover();
            }
        }
    }

    private void removeBookCover() {
        try {
            int row=selectedModelRow(bookTable);
            if(row>=0)services.ImageStore.remove(java.nio.file.Paths.get(IMAGES_DIR),(String)bookTableModel.getValueAt(row,0));
            currentImageFile=null;currentBookIsbnForImage=null;setDefaultBookCover();showStyledSuccessDialog("Image removed/cleared.");
        } catch(Exception e){showStyledErrorDialog("Image removal failed: "+e.getMessage());}
    }

    private void saveBookCover(String isbn) {
        if(currentImageFile==null)return;
        try {services.ImageStore.store(currentImageFile.toPath(),java.nio.file.Paths.get(IMAGES_DIR),isbn);}
        catch(Exception e) {throw new IllegalStateException("Record saved, but its image was not saved: "+e.getMessage(),e);}
    }

    private void loadBookCover(String isbn) {
        if (isbn == null || isbn.isEmpty()) {
            setDefaultBookCover();
            return;
        }

        // Try to find image with different extensions
        String[] extensions = {".jpg", ".jpeg", ".png", ".gif", ".bmp"};
        File imageFile = null;

        for (String ext : extensions) {
            File testFile = new File(IMAGES_DIR + isbn + ext);
            if (testFile.exists()) {
                imageFile = testFile;
                break;
            }
        }

        if (imageFile != null && imageFile.exists()) {
            try {
                Image originalImage = services.ImageStore.read(imageFile.toPath());
                if (originalImage != null) {
                    // Scale image to fit label
                    int labelWidth = lblBookCover.getWidth() - 10;
                    int labelHeight = lblBookCover.getHeight() - 10;

                    if (labelWidth <= 0) labelWidth = 170;
                    if (labelHeight <= 0) labelHeight = 210;

                    Image scaledImage = originalImage.getScaledInstance(
                        labelWidth, labelHeight, Image.SCALE_SMOOTH);

                    ImageIcon icon = new ImageIcon(scaledImage);
                    lblBookCover.setIcon(icon);
                    currentImageFile = imageFile;
                    currentBookIsbnForImage = isbn;
                    return;
                }
            } catch (IOException e) {
                System.err.println("Error loading book cover: " + e.getMessage());
            }
        }

        // If no image found or error occurred, set default
        setDefaultBookCover();
    }

    private String getFileExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < filename.length() - 1) {
            return filename.substring(dotIndex + 1).toLowerCase();
        }
        return "jpg";
    }

    private void addNewBook() {
        try {
            Book book=BookFactory.createBook((String)cmbBookType.getSelectedItem(),txtIsbn.getText().trim(),
                txtTitle.getText().trim(),txtAuthor.getText().trim(),Integer.parseInt(txtYear.getText().trim()),Double.parseDouble(txtPrice.getText().trim()));
            libraryService.addBook(book);
            if(currentImageFile!=null)saveBookCover(book.getIsbn());
            clearBookFields();refreshBookTable();updateStats();showStyledSuccessDialog("Book added successfully.");
        } catch(Exception e){showStyledErrorDialog(e.getMessage());}
    }

    private void addNewMember() {
        try {
            String id=txtMemberId.getText().trim();
            libraryService.addMember(new Member(id,txtMemberName.getText().trim(),txtMemberEmail.getText().trim(),txtMemberPhone.getText().trim(),"MEM"+id));
            if(currentMemberImageFile!=null)saveMemberPhoto(id);
            clearMemberFields();refreshMemberTable();updateStats();showStyledSuccessDialog("Member added successfully.");
        } catch(Exception e){showStyledErrorDialog(e.getMessage());}
    }

    private void deleteSelectedMember() {
        int row=selectedModelRow(memberTable);
        if(row<0){showStyledWarningDialog("Select a member to delete.");return;}
        String id=(String)memberTableModel.getValueAt(row,0);
        if(JOptionPane.showConfirmDialog(this,"Delete this member? Completed loan history will be retained.","Confirm deletion",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)return;
        try {
            libraryService.deleteMember(id);refreshBookTable();refreshMemberTable();updateStats();deleteMemberPhoto(id);
            showStyledSuccessDialog("Member deleted. Completed loan history retained.");
        } catch(Exception e){showStyledErrorDialog(e.getMessage());}
    }





    private void viewMemberDetails() {
        // Keep this method for backward compatibility
        viewMemberDetailsWithPhoto();
    }

    private void deleteSelectedBook() {
        int row=selectedModelRow(bookTable);
        if(row<0){showStyledWarningDialog("Select a book to delete.");return;}
        String id=(String)bookTableModel.getValueAt(row,0);
        if(JOptionPane.showConfirmDialog(this,"Delete this book? Completed loan history will be retained.","Confirm deletion",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)return;
        try {
            libraryService.deleteBook(id);refreshBookTable();refreshMemberTable();updateStats();deleteBookCover(id);
            showStyledSuccessDialog("Book deleted. Completed loan history retained.");
        } catch(Exception e){showStyledErrorDialog(e.getMessage());}
    }

    private void deleteBookCover(String isbn) {
        if(isbn==null || isbn.isEmpty())return;
        try {services.ImageStore.remove(java.nio.file.Paths.get(IMAGES_DIR),isbn);}
        catch(Exception e){throw new IllegalStateException("Record change completed, but the image could not be removed: "+e.getMessage(),e);}
    }

    private void viewBookDetailsWithImage() {
        int selectedRow = selectedModelRow(bookTable);
        if (selectedRow == -1) {
            showStyledWarningDialog("Please select a book to view details!");
            return;
        }

        String isbn = (String) bookTableModel.getValueAt(selectedRow, 0);
        String title = (String) bookTableModel.getValueAt(selectedRow, 1);

        Book book = null;
        List<Book> books = libraryService.getBooks();
        for (Book b : books) {
            if (b.getIsbn().equals(isbn)) {
                book = b;
                break;
            }
        }

        if (book == null) {
            showStyledErrorDialog("Book not found in system!");
            return;
        }

        // Create a panel with image and details
        JPanel mainPanel = new JPanel(new BorderLayout(20, 20));
        mainPanel.setBackground(new Color(30, 30, 40));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Left panel for book cover
        JPanel imagePanel = new JPanel(new BorderLayout());
        imagePanel.setBackground(new Color(30, 30, 40));
        imagePanel.setBorder(createStyledBorder("📸 BOOK COVER"));
        imagePanel.setPreferredSize(new Dimension(300, 400));

        JLabel coverLabel = new JLabel();
        coverLabel.setHorizontalAlignment(SwingConstants.CENTER);
        coverLabel.setVerticalAlignment(SwingConstants.CENTER);
        coverLabel.setOpaque(true);
        coverLabel.setBackground(new Color(40, 40, 50));
        coverLabel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(IMAGE_BORDER_COLOR, 2),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        coverLabel.setPreferredSize(new Dimension(250, 300));

        // Load book cover image
        loadBookCoverForDialog(isbn, coverLabel);

        imagePanel.add(coverLabel, BorderLayout.CENTER);

        // Image buttons
        JPanel imageButtonPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        imageButtonPanel.setBackground(new Color(30, 30, 40));
        imageButtonPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JButton btnUploadNew = createStyledButton("📁 UPLOAD NEW", new Color(80, 180, 120));
        JButton btnRemoveCurrent = createStyledButton("🗑️ REMOVE", new Color(255, 100, 150));

        btnUploadNew.addActionListener(e -> {
            uploadBookCoverForBook(isbn, coverLabel);
        });

        btnRemoveCurrent.addActionListener(e -> {
            deleteBookCover(isbn);
            setDefaultBookCoverForDialog(coverLabel);
            showStyledSuccessDialog("Book cover removed.");
        });

        imageButtonPanel.add(btnUploadNew);
        imageButtonPanel.add(btnRemoveCurrent);

        imagePanel.add(imageButtonPanel, BorderLayout.SOUTH);

        // Right panel for book details
        JPanel detailsPanel = new JPanel(new BorderLayout());
        detailsPanel.setBackground(new Color(30, 30, 40));
        detailsPanel.setBorder(createStyledBorder("📖 BOOK DETAILS"));

        StringBuilder details = new StringBuilder();
        details.append("Title: ").append(book.getTitle()).append("\n\n");
        details.append("Author: ").append(book.getAuthor()).append("\n\n");
        details.append("ISBN: ").append(isbn).append("\n\n");
        details.append("Year: ").append(book.getPublicationYear()).append("\n\n");
        details.append("Available: ").append(book.isAvailable() ? "✅ Yes" : "❌ No").append("\n\n");
        details.append("Price: $").append(String.format("%.2f", book.getPrice())).append("\n\n");

        String type = "BOOK";
        if (book instanceof FictionBook) type = "FICTION";
        else if (book instanceof Textbook) type = "TEXTBOOK";
        else if (book instanceof ReferenceBook) type = "REFERENCE";
        else if (book instanceof NonFictionBook) type = "NONFICTION";
        details.append("Type: ").append(type).append("\n\n");

        if (!book.isAvailable()) {
            details.append("⚠️ BORROWING STATUS:\n");
            List<Transaction> transactions = libraryService.getTransactions();
            boolean found = false;
            for (Transaction t : transactions) {
                if (t.getBookIsbn().equals(isbn) && !t.isReturned()) {
                    found = true;
                    details.append("• Borrowed by: Member ").append(t.getMemberId()).append("\n");
                    details.append("• Borrow Date: ").append(t.getBorrowDate()).append("\n");
                    details.append("• Due Date: ").append(t.getDueDate()).append("\n");

                    if (t.isOverdue()) {
                        details.append("• ⚠️ OVERDUE!\n");
                        details.append("• Fine: $").append(String.format("%.2f", t.getFineAmount())).append("\n");
                    }
                    break;
                }
            }
            if (!found) {
                details.append("• Currently borrowed (details unavailable)\n");
            }
        }

        JTextArea textArea = new JTextArea(details.toString());
        textArea.setEditable(false);
        textArea.setBackground(new Color(30, 30, 40));
        textArea.setForeground(TEXT_COLOR);
        textArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        textArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        detailsPanel.add(new JScrollPane(textArea), BorderLayout.CENTER);

        mainPanel.add(imagePanel, BorderLayout.WEST);
        mainPanel.add(detailsPanel, BorderLayout.CENTER);

        JOptionPane.showMessageDialog(this, mainPanel,
            "Book Details: " + title, JOptionPane.INFORMATION_MESSAGE);
    }

    private void loadBookCoverForDialog(String isbn, JLabel coverLabel) {
        if (isbn == null || isbn.isEmpty()) {
            setDefaultBookCoverForDialog(coverLabel);
            return;
        }

        String[] extensions = {".jpg", ".jpeg", ".png", ".gif", ".bmp"};
        File imageFile = null;

        for (String ext : extensions) {
            File testFile = new File(IMAGES_DIR + isbn + ext);
            if (testFile.exists()) {
                imageFile = testFile;
                break;
            }
        }

        if (imageFile != null && imageFile.exists()) {
            try {
                Image originalImage = services.ImageStore.read(imageFile.toPath());
                if (originalImage != null) {
                    Image scaledImage = originalImage.getScaledInstance(
                        200, 250, Image.SCALE_SMOOTH);

                    ImageIcon icon = new ImageIcon(scaledImage);
                    coverLabel.setIcon(icon);
                    return;
                }
            } catch (IOException e) {
                System.err.println("Error loading book cover: " + e.getMessage());
            }
        }

        setDefaultBookCoverForDialog(coverLabel);
    }

    private void setDefaultBookCoverForDialog(JLabel coverLabel) {
        int width = 200;
        int height = 250;
        BufferedImage defaultCover = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = defaultCover.createGraphics();

        GradientPaint gradient = new GradientPaint(0, 0, PRIMARY_COLOR, width, height, PRIMARY_GRADIENT);
        g2.setPaint(gradient);
        g2.fillRect(0, 0, width, height);

        g2.setColor(new Color(0, 100, 200));
        g2.fillRect(width - 15, 0, 15, height);

        g2.setColor(new Color(220, 240, 255));
        g2.setFont(new Font("Arial", Font.BOLD, 16));
        String titleText = "NO COVER";
        FontMetrics fm = g2.getFontMetrics();
        int titleX = (width - fm.stringWidth(titleText)) / 2;
        g2.drawString(titleText, titleX, height / 2);

        g2.setFont(new Font("Arial", Font.ITALIC, 12));
        String authorText = "Upload an image";
        fm = g2.getFontMetrics();
        int authorX = (width - fm.stringWidth(authorText)) / 2;
        g2.drawString(authorText, authorX, height / 2 + 25);

        g2.dispose();

        ImageIcon icon = new ImageIcon(defaultCover);
        coverLabel.setIcon(icon);
    }

    private void uploadBookCoverForBook(String isbn, JLabel coverLabel) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select Book Cover Image");

        fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
            @Override
            public boolean accept(File f) {
                if (f.isDirectory()) return true;
                String name = f.getName().toLowerCase();
                return name.endsWith(".jpg") || name.endsWith(".jpeg") ||
                       name.endsWith(".png") || name.endsWith(".gif") ||
                       name.endsWith(".bmp");
            }

            @Override
            public String getDescription() {
                return "Image Files (*.jpg, *.jpeg, *.png, *.gif, *.bmp)";
            }
        });

        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File imageFile = fileChooser.getSelectedFile();

            try {
                Image originalImage = services.ImageStore.read(imageFile.toPath());
                if (originalImage == null) {
                    showStyledErrorDialog("Could not load image. Unsupported format.");
                    return;
                }

                // Save the image
                services.ImageStore.store(imageFile.toPath(),java.nio.file.Paths.get(IMAGES_DIR),isbn);

                // Update the display
                Image scaledImage = originalImage.getScaledInstance(
                    200, 250, Image.SCALE_SMOOTH);

                ImageIcon icon = new ImageIcon(scaledImage);
                coverLabel.setIcon(icon);

                // Also update the main book cover panel
                loadBookCover(isbn);

                showStyledSuccessDialog("Book cover updated successfully!");

            } catch (IOException e) {
                showStyledErrorDialog("Error loading image: " + e.getMessage());
            } catch (Exception e) {
                showStyledErrorDialog("Error: " + e.getMessage());
            }
        }
    }

    private void borrowBookAction() {
        String memberId = txtBorrowMemberId.getText().trim();
        String bookIsbn = txtBorrowIsbn.getText().trim();

        if (memberId.isEmpty() || bookIsbn.isEmpty()) {
            appendToStatus("❌ Error: Please enter both Member ID and Book ISBN");
            return;
        }

        try {
            Transaction transaction = libraryService.borrowBook(memberId, bookIsbn);

            appendToStatus("✅ SUCCESS: Book borrowed successfully!");
            appendToStatus("Transaction ID: " + transaction.getTransactionId());
            appendToStatus("Member ID: " + transaction.getMemberId());
            appendToStatus("Book ISBN: " + transaction.getBookIsbn());
            appendToStatus("Borrow Date: " + transaction.getBorrowDate());
            appendToStatus("Due Date: " + transaction.getDueDate());
            appendToStatus("Please return by: " + transaction.getDueDate());
            appendToStatus("-".repeat(50));

            txtBorrowMemberId.setText("");
            txtBorrowIsbn.setText("");

            refreshBookTable();
            refreshMemberTable();
            updateStats();

        } catch (BookNotFoundException e) {
            appendToStatus("❌ Error: " + e.getMessage());
        } catch (MemberLimitException e) {
            appendToStatus("❌ Error: " + e.getMessage());
            appendToStatus("Maximum borrowing limit reached!");
        } catch (IllegalStateException e) {
            appendToStatus("❌ Error: " + e.getMessage());
        } catch (Exception e) {
            appendToStatus("❌ Error: " + e.getMessage());
        }
    }

    private void returnBookAction() {
        String memberId = txtReturnMemberId.getText().trim();
        String bookIsbn = txtReturnIsbn.getText().trim();

        if (memberId.isEmpty() || bookIsbn.isEmpty()) {
            appendToStatus("❌ Error: Please enter both Member ID and Book ISBN");
            return;
        }

        try {
            Transaction transaction = libraryService.returnBook(memberId, bookIsbn);

            appendToStatus("✅ SUCCESS: Book returned successfully!");
            appendToStatus("Transaction ID: " + transaction.getTransactionId());
            appendToStatus("Member ID: " + transaction.getMemberId());
            appendToStatus("Book ISBN: " + transaction.getBookIsbn());
            appendToStatus("Borrow Date: " + transaction.getBorrowDate());
            appendToStatus("Return Date: " + transaction.getReturnDate());

            if (transaction.getFineAmount() > 0) {
                appendToStatus("⚠️ Overdue Fine: $" + String.format("%.2f", transaction.getFineAmount()));
            }

            appendToStatus("-".repeat(50));

            txtReturnMemberId.setText("");
            txtReturnIsbn.setText("");

            refreshBookTable();
            refreshMemberTable();
            updateStats();

        } catch (BookNotFoundException e) {
            appendToStatus("❌ Error: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            appendToStatus("❌ Error: " + e.getMessage());
        } catch (Exception e) {
            appendToStatus("❌ Error: " + e.getMessage());
        }
    }

    private void performSearchWithDebug(String query, String searchType, JTextArea resultsArea) {
        try {
            List<Book> results=libraryService.searchBooks(query,searchType);
            resultsArea.setText("Found "+results.size()+" book(s).\n\n");
            for(Book book:results)resultsArea.append(book.getTitle()+" by "+book.getAuthor()+"\nID: "+book.getIsbn()+" | "+LibraryService.typeOf(book)+" | "+(book.isAvailable()?"Available":"Borrowed")+"\n\n");
        } catch(Exception e){resultsArea.setText("Search failed: "+e.getMessage());}
    }

    private void performSearch(String query, String searchType, JTextArea resultsArea) {
        performSearchWithDebug(query, searchType, resultsArea);
    }

    private void viewOverdueBooks() {
        List<Transaction> overdue = libraryService.getOverdueTransactions();

        if (overdue.isEmpty()) {
            appendToStatus("✅ No overdue books!");
            return;
        }

        appendToStatus("⚠️ OVERDUE BOOKS:");
        appendToStatus("=".repeat(50));

        for (Transaction t : overdue) {
            appendToStatus("Transaction ID: " + t.getTransactionId());
            appendToStatus("Member ID: " + t.getMemberId());
            appendToStatus("Book ISBN: " + t.getBookIsbn());
            appendToStatus("Borrow Date: " + t.getBorrowDate());
            appendToStatus("Due Date: " + t.getDueDate());
            long daysOverdue = java.time.LocalDate.now().toEpochDay() - t.getDueDate().toEpochDay();
            appendToStatus("Days Overdue: " + daysOverdue);
            appendToStatus("-".repeat(30));
        }
    }

    private void appendToStatus(String message) {
        txtTransactionStatus.append(message + "\n");
        txtTransactionStatus.setCaretPosition(txtTransactionStatus.getDocument().getLength());
    }

    private void refreshBookTable() {
        bookTableModel.setRowCount(0);

        List<Book> books = libraryService.getBooks();
        for (Book book : books) {
            String type = "BOOK";
            if (book instanceof FictionBook) type = "FICTION";
            else if (book instanceof Textbook) type = "TEXTBOOK";
            else if (book instanceof ReferenceBook) type = "REFERENCE";
            else if (book instanceof NonFictionBook) type = "NONFICTION";

            String available = book.isAvailable() ? "✅ Yes" : "❌ No";

            bookTableModel.addRow(new Object[]{
                book.getIsbn(),
                book.getTitle(),
                book.getAuthor(),
                book.getPublicationYear(),
                type,
                available,
                book.getPrice()
            });
        }
    }

    private void refreshMemberTable() {
        memberTableModel.setRowCount(0);

        List<Member> members = libraryService.getMembers();
        for (Member member : members) {
            memberTableModel.addRow(new Object[]{
                member.getId(),
                member.getName(),
                member.getEmail(),
                member.getPhone(),
                member.getJoinDate().toString(),
                member.getBorrowedBooks() + "/" + Member.getMaxBorrowLimit()
            });
        }
    }



    private void clearBookFields() {
        txtIsbn.setText("");
        txtTitle.setText("");
        txtAuthor.setText("");
        txtYear.setText("");
        txtPrice.setText("");
        setDefaultBookCover();
        txtIsbn.requestFocus();
        txtIsbn.setEnabled(true);
        txtIsbn.setBackground(INPUT_BACKGROUND);
        isEditingBook = false;
        editingBookIsbn = "";
        currentImageFile=null;currentBookIsbnForImage=null;
        if(addBookButton!=null){addBookButton.setEnabled(true);editBookButton.setEnabled(true);saveBookButton.setVisible(false);}
    }

    private void clearMemberFields() {
        txtMemberId.setText("");
        txtMemberName.setText("");
        txtMemberEmail.setText("");
        txtMemberPhone.setText("");
        setDefaultMemberPhoto();
        txtMemberId.requestFocus();
        txtMemberId.setEnabled(true);
        txtMemberId.setBackground(INPUT_BACKGROUND);
        isEditingMember = false;
        editingMemberId = "";
        currentMemberImageFile=null;currentMemberIdForImage=null;
        if(addMemberButton!=null){addMemberButton.setEnabled(true);editMemberButton.setEnabled(true);saveMemberButton.setVisible(false);}
    }

    private void loadInitialData() {
        refreshBookTable();
        refreshMemberTable();
        updateStats();
    }

    private void startStatsUpdater() {
        Timer timer = new Timer(5000, e -> updateStats());
        timer.start();
    }

    private void updateStats() {
        List<Book> books = libraryService.getBooks();
        List<Member> members = libraryService.getMembers();
        List<Transaction> overdue = libraryService.getOverdueTransactions();

        int availableBooks = 0;
        for (Book book : books) {
            if (book.isAvailable()) availableBooks++;
        }

        if (lblTotalBooks != null) lblTotalBooks.setText(String.valueOf(books.size()));
        if (lblTotalMembers != null) lblTotalMembers.setText(String.valueOf(members.size()));
        if (lblAvailableBooks != null) lblAvailableBooks.setText(String.valueOf(availableBooks));
        if (lblOverdueBooks != null) lblOverdueBooks.setText(String.valueOf(overdue.size()));
    }

    private JPanel createStyledMessagePanel(String message, Color textColor) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(30, 30, 40));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JTextArea textArea = new JTextArea(message);
        textArea.setEditable(false);
        textArea.setBackground(new Color(30, 30, 40));
        textArea.setForeground(textColor);
        textArea.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        textArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        panel.add(textArea, BorderLayout.CENTER);
        return panel;
    }

    private void showStyledErrorDialog(String message) {
        JOptionPane.showMessageDialog(this,
            createStyledMessagePanel(message, ERROR_COLOR),
            "❌ Error",
            JOptionPane.ERROR_MESSAGE);
    }

    private void showStyledSuccessDialog(String message) {
        JOptionPane.showMessageDialog(this,
            createStyledMessagePanel(message, SUCCESS_COLOR),
            "✅ Success",
            JOptionPane.INFORMATION_MESSAGE);
    }

    private void showStyledWarningDialog(String message) {
        JOptionPane.showMessageDialog(this,
            createStyledMessagePanel(message, WARNING_COLOR),
            "⚠️ Warning",
            JOptionPane.WARNING_MESSAGE);
    }

    private void exportReport() {
        JFileChooser chooser=new JFileChooser();chooser.setDialogTitle("Export Book Catalogue (CSV)");chooser.setSelectedFile(new File("library-catalogue.csv"));
        if(chooser.showSaveDialog(this)!=JFileChooser.APPROVE_OPTION)return;
        if(!confirmOverwrite(chooser.getSelectedFile()))return;
        try {services.LibraryExports.report(libraryService,chooser.getSelectedFile().toPath());showStyledSuccessDialog("Catalogue exported successfully.");}
        catch(Exception e){showStyledErrorDialog("Export failed: "+e.getMessage());}
    }

    private void refreshAllData() {
        try {libraryService=new LibraryService();refreshBookTable();refreshMemberTable();updateStats();
            showStyledSuccessDialog("Library data reloaded successfully.");}
        catch(Exception e){showStyledErrorDialog(e.getMessage());}
    }

    private void clearAllLogs() {
        txtTransactionStatus.setText("");
        showStyledSuccessDialog("All logs cleared!");
    }

    private void backupDatabase() {
        JFileChooser chooser=new JFileChooser();chooser.setDialogTitle("Back Up Library Data and Images");chooser.setSelectedFile(new File("library-backup.zip"));
        if(chooser.showSaveDialog(this)!=JFileChooser.APPROVE_OPTION || !confirmOverwrite(chooser.getSelectedFile()))return;
        try {services.LibraryExports.backup(FileHandler.dataDirectory(),chooser.getSelectedFile().toPath());showStyledSuccessDialog("Backup ZIP created. It contains private library data; store it securely.");}
        catch(Exception e){showStyledErrorDialog("Backup failed: "+e.getMessage());}
    }

    private void exportLog() {
        JFileChooser chooser=new JFileChooser();chooser.setSelectedFile(new File("transaction-log.txt"));
        if(chooser.showSaveDialog(this)!=JFileChooser.APPROVE_OPTION || !confirmOverwrite(chooser.getSelectedFile()))return;
        try {services.LibraryExports.log(txtTransactionStatus.getText(),chooser.getSelectedFile().toPath());showStyledSuccessDialog("Visible transaction log exported.");}
        catch(Exception e){showStyledErrorDialog("Log export failed: "+e.getMessage());}
    }

    private void generateReport() {
        exportReport();
    }

    private void showSettingsDialog() {
        showStyledWarningDialog("Data folder: "+FileHandler.dataDirectory()+"\nLoan rules: 5 books per member, 14 days per regular loan, 1 unit per late day.");
    }

    private void showUserGuide() {
        JTextArea guide=new JTextArea(
            "LIBRARY SYSTEM\n\n"+
            "Books: enter ID, title, author, publication year and price; choose a type.\n"+
            "Select a row to edit, view details, upload a cover, or delete an available book.\n"+
            "Members: add contact details and an optional photo. IDs stay fixed during editing.\n"+
            "Borrow: enter member ID and book ID. Return books through the Returns card.\n"+
            "Limits: 5 active loans per member; regular loans last 14 days.\n"+
            "Reference books default to in-library use. Late returns accrue 1 unit/day.\n"+
            "Click table headers to sort; selections still target the correct record.\n"+
            "File > Export Report writes a catalogue CSV. Tools > Back Up Library Data writes a ZIP.\n"+
            "Use CLEAR to leave edit mode. Completed loan history is retained when records are deleted.\n\n"+
            "Data folder: "+FileHandler.dataDirectory()+"\n"+
            "Images: JPG/JPEG, PNG, GIF or BMP; at most 10 MB / 16 million pixels.\n"+
            "New images are normalized to ID.png. Keep records and backups private.");
        guide.setEditable(false);guide.setLineWrap(true);guide.setWrapStyleWord(true);
        guide.setBackground(PANEL_BACKGROUND);guide.setForeground(TEXT_COLOR);
        JScrollPane scroll=new JScrollPane(guide);scroll.setPreferredSize(new Dimension(650,400));
        JOptionPane.showMessageDialog(this,scroll,"User Guide",JOptionPane.INFORMATION_MESSAGE);
    }

    private void showAboutDialog() {
        JOptionPane.showMessageDialog(this,
            "Library System - maintenance version 1.1.0\nJava 17+ desktop application\n"+
            "Book catalogue, images, members, loans, reports and backups.\n\n"+
            "Original project supplied by rashwan26.\n"+
            "Original notice: © 2024 Library Management System. All Rights Reserved.",
            "About Library System",JOptionPane.INFORMATION_MESSAGE);
    }

    // Main method to run the application
    public static void main(String[] args) {
        main.Main.main(args);
    }
}
