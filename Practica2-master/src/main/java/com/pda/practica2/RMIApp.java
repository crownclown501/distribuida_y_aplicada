package com.pda.practica2;

import com.pda.practica2.interfaces.PeerInterface;
import com.pda.practica2.model.RMIPeer;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.swing.table.TableCellRenderer;

public class RMIApp extends JFrame {
    private RMIPeer peer;
    private PeerInterface stub;
    private Registry registry;
    private JTextArea textAreaSearchResults;
    private JTextArea jTextAreaMessages;
    private JTextField searchField;
    private JButton searchButton;
    private JButton uploadButton;
    private JTextArea catalogMP3;
    private JTextArea catalogMP4;
    private JPanel filePreviewPanel;
    private JLabel jLabelCoor;

    // New components
    private JProgressBar transferProgressBar;
    private JTable filesTable;
    private JButton downloadButton;
    private JButton playButton;
    private JButton pauseButton;
    private JButton resumeButton;
    private DefaultTableModel tableModel;
    private Map<String, String> activeTransfers = new HashMap<>();

    public RMIApp(String nodeID) throws RemoteException {
        super("Intercambio de Archivos P2P");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Campo de búsqueda y botón de buscar
        searchField = new JTextField(20);
        searchButton = new JButton("Buscar");
        searchButton.addActionListener(e -> searchFiles(searchField.getText()));

        uploadButton = new JButton("Subir Archivo");
        uploadButton.addActionListener(e -> uploadFile());

        JPanel searchPanel = new JPanel();
        searchPanel.add(searchField);
        searchPanel.add(searchButton);
        searchPanel.add(uploadButton);
        add(searchPanel, BorderLayout.NORTH);

        // Área para mostrar resultados de búsqueda
        textAreaSearchResults = new JTextArea();
        JScrollPane scrollPaneSearchResults = new JScrollPane(textAreaSearchResults);
        add(scrollPaneSearchResults, BorderLayout.CENTER);

        // Área para mostrar mensajes
        jTextAreaMessages = new JTextArea();
        JScrollPane scrollPaneMessages = new JScrollPane(jTextAreaMessages);
        add(scrollPaneMessages, BorderLayout.SOUTH);

        // Áreas para visualizar catálogos
        catalogMP3 = new JTextArea();
        JScrollPane scrollPaneCatalogMP3 = new JScrollPane(catalogMP3);
        catalogMP4 = new JTextArea();
        JScrollPane scrollPaneCatalogMP4 = new JScrollPane(catalogMP4);

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Catálogo MP3", scrollPaneCatalogMP3);
        tabbedPane.addTab("Catálogo MP4", scrollPaneCatalogMP4);
        add(tabbedPane, BorderLayout.SOUTH);

        // Área para visualizar/reproducir archivos
        filePreviewPanel = new JPanel();
        add(filePreviewPanel, BorderLayout.EAST);

        // Label para mostrar el coordinador
        jLabelCoor = new JLabel("Coordinador: ");
        add(jLabelCoor, BorderLayout.WEST);

        // Initialize new UI components
        initializeUIComponents();

        try {
            registry = LocateRegistry.createRegistry(1099);
            peer = new RMIPeer(nodeID, 1, this);
            stub = (PeerInterface) UnicastRemoteObject.exportObject(peer, 0);
            registry.rebind(nodeID, stub);
            updatePeersList();
        } catch (RemoteException ex) {
            ex.printStackTrace();
        }
    }

    private void initializeUIComponents() {
        // Configuración de la barra de progreso
        transferProgressBar = new JProgressBar(0, 100);
        transferProgressBar.setStringPainted(true);
        transferProgressBar.setString("Esperando transferencia...");

        // Configuración de la tabla de archivos
        String[] columnNames = {"Archivo", "Tipo", "Tamaño", "Peer", "Acciones"};
        tableModel = new DefaultTableModel(columnNames, 0);
        filesTable = new JTable(tableModel);
        filesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Configurar un renderizador personalizado para la columna "Acciones"
        filesTable.getColumnModel().getColumn(4).setCellRenderer(new ButtonRenderer());
        filesTable.getColumnModel().getColumn(4).setCellEditor(new ButtonEditor(new JCheckBox()));

        // Botones de control
        downloadButton = new JButton("Descargar");
        playButton = new JButton("Reproducir");
        pauseButton = new JButton("Pausar");
        resumeButton = new JButton("Reanudar");

        // Panel de controles
        JPanel controlPanel = new JPanel();
        controlPanel.add(downloadButton);
        controlPanel.add(playButton);
        controlPanel.add(pauseButton);
        controlPanel.add(resumeButton);

        // Añadir listeners
        downloadButton.addActionListener(e -> downloadSelectedFile());
        playButton.addActionListener(e -> playSelectedFile());
        pauseButton.addActionListener(e -> pauseTransfer());
        resumeButton.addActionListener(e -> resumeTransfer());

        // Añadir componentes a la interfaz
        JScrollPane tableScrollPane = new JScrollPane(filesTable);
        JPanel southPanel = new JPanel(new BorderLayout());
        southPanel.add(transferProgressBar, BorderLayout.NORTH);
        southPanel.add(controlPanel, BorderLayout.SOUTH);

        // Este panel reemplazaría o se añadiría a tu interfaz existente
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(tableScrollPane, BorderLayout.CENTER);
        mainPanel.add(southPanel, BorderLayout.SOUTH);

        // Actualizar la interfaz periódicamente para monitorear transferencias
        startTransferMonitor();
    }

    private void uploadFile() {
        JFileChooser fileChooser = new JFileChooser();
        int selection = fileChooser.showOpenDialog(this);
        if (selection == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try {
                // Registrar en el catálogo
                peer.registerCatalog(file.getName());

                // Copiar el archivo a la carpeta storage
                File storageDir = new File("storage");
                if (!storageDir.exists()) {
                    storageDir.mkdir();
                }

                File destFile = new File(storageDir, file.getName());
                Files.copy(file.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

                textAreaSearchResults.append("Archivo subido: " + file.getName() + "\n");
                updateCatalogs();
            } catch (Exception ex) {
                ex.printStackTrace();
                textAreaSearchResults.append("Error al subir el archivo: " + ex.getMessage() + "\n");
            }
        }
    }

    private void updateCatalogs() {
        try {
            String[] mp3Files = peer.getCatalogs("mp3");
            String[] mp4Files = peer.getCatalogs("mp4");
            catalogMP3.setText(String.join("\n", mp3Files));
            catalogMP4.setText(String.join("\n", mp4Files));
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void updatePeersList() {
        try {
            StringBuilder sb = new StringBuilder();
            String[] listPeers = registry.list();
            for (String peerName : listPeers) {
                sb.append(peerName).append(" - Online\n");
            }
            textAreaSearchResults.setText(sb.toString());
        } catch (RemoteException ex) {
            ex.printStackTrace();
        }
    }

    public void searchFiles(String query) {
        try {
            String[] results = peer.searchFiles(query);
            textAreaSearchResults.setText(String.join("\n", results));
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void updateCoordinator(String coordinator) {
        jLabelCoor.setText("Coordinador: " + coordinator);
    }

    public Registry getRegistry() {
        return registry;
    }

    public JTextArea getjTextAreaMessages() {
        return jTextAreaMessages;
    }

    public JLabel getjLabelCoor() {
        return jLabelCoor;
    }

    public JTextArea getjTextAreaPeers() {
        return textAreaSearchResults;
    }

    private void downloadSelectedFile() {
        int selectedRow = filesTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Selecciona un archivo primero.");
            return;
        }

        String fileName = (String) tableModel.getValueAt(selectedRow, 0);
        String peerName = (String) tableModel.getValueAt(selectedRow, 3);

        try {
            // Obtener referencia al peer remoto
            Registry registry = getRegistry();
            PeerInterface remotePeer = (PeerInterface) registry.lookup(peerName);

            // Iniciar la transferencia
            transferProgressBar.setValue(0);
            transferProgressBar.setString("Descargando " + fileName + "...");

            // Ejecutar en un hilo separado para no bloquear la UI
            new Thread(() -> {
                try {
                    // Generar un ID único para esta transferencia
                    String transferId = UUID.randomUUID().toString();
                    activeTransfers.put(transferId, fileName);

                    // Iniciar transferencia
                    remotePeer.transferFile(fileName, peer.getName());

                    // La transferencia se monitorea en el método startTransferMonitor()
                } catch (Exception e) {
                    SwingUtilities.invokeLater(() -> {
                        transferProgressBar.setString("Error en la transferencia");
                        JOptionPane.showMessageDialog(this, "Error al descargar: " + e.getMessage());
                    });
                }
            }).start();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error al iniciar descarga: " + e.getMessage());
        }
    }

    private void playSelectedFile() {
        int selectedRow = filesTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Selecciona un archivo primero.");
            return;
        }

        String fileName = (String) tableModel.getValueAt(selectedRow, 0);
        File file = new File("storage", fileName);

        if (!file.exists()) {
            JOptionPane.showMessageDialog(this, "El archivo no existe localmente. Descárgalo primero.");
            return;
        }

        try {
            // Detectar tipo de archivo y reproducir apropiadamente
            if (fileName.toLowerCase().endsWith(".mp3") || fileName.toLowerCase().endsWith(".mp4")) {
                Desktop.getDesktop().open(file);
            } else {
                JOptionPane.showMessageDialog(this, "Tipo de archivo no reproducible.");
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error al reproducir: " + e.getMessage());
        }
    }

    private void pauseTransfer() {
        // Implementar lógica para pausar transferencia
    }

    private void resumeTransfer() {
        // Implementar lógica para reanudar transferencia
    }

    private void startTransferMonitor() {
        Timer timer = new Timer(500, e -> {
            for (Map.Entry<String, String> entry : activeTransfers.entrySet()) {
                String transferId = entry.getKey();
                String fileName = entry.getValue();

                try {
                    // Obtener progreso de la transferencia
                    int progress = peer.getTransferProgress(transferId);
                    transferProgressBar.setValue(progress);

                    if (progress >= 100) {
                        // Transferencia completada
                        transferProgressBar.setString("Transferencia completada: " + fileName);
                        activeTransfers.remove(transferId);
                        refreshFilesList();
                    }
                } catch (RemoteException ex) {
                    // Error al obtener progreso
                    transferProgressBar.setString("Error al monitorear transferencia");
                }
            }
        });

        timer.start();
    }

    public void refreshFilesList() {
        tableModel.setRowCount(0);

        try {
            // Obtener archivos locales
            String[] mp3Files = peer.getCatalogs("mp3");
            String[] mp4Files = peer.getCatalogs("mp4");

            // Añadir archivos MP3
            for (String fileName : mp3Files) {
                File file = new File("storage", fileName);
                Object[] row = {
                    fileName,
                    "MP3",
                    file.exists() ? (file.length() / 1024) + " KB" : "No descargado",
                    peer.getName(),
                    "Acciones"
                };
                tableModel.addRow(row);
            }

            // Añadir archivos MP4
            for (String fileName : mp4Files) {
                File file = new File("storage", fileName);
                Object[] row = {
                    fileName,
                    "MP4",
                    file.exists() ? (file.length() / 1024) + " KB" : "No descargado",
                    peer.getName(),
                    "Acciones"
                };
                tableModel.addRow(row);
            }

            // Buscar archivos en la red (si el coordinador está disponible)
            if (!peer.getName().equals(peer.getCoordinator())) {
                try {
                    Registry registry = getRegistry();
                    PeerInterface coordinator = (PeerInterface) registry.lookup(peer.getCoordinator());
                    Map<String, String[]> networkFiles = coordinator.searchNetworkFiles("");

                    for (Map.Entry<String, String[]> entry : networkFiles.entrySet()) {
                        String peerName = entry.getKey();
                        if (!peerName.equals(peer.getName())) {
                            String[] files = entry.getValue();
                            for (String fileName : files) {
                                // Verificar si ya tenemos este archivo localmente
                                boolean alreadyListed = false;
                                for (int i = 0; i < tableModel.getRowCount(); i++) {
                                    if (tableModel.getValueAt(i, 0).equals(fileName)) {
                                        alreadyListed = true;
                                        break;
                                    }
                                }

                                if (!alreadyListed) {
                                    String fileType = fileName.toLowerCase().endsWith(".mp3") ? "MP3" :
                                                      fileName.toLowerCase().endsWith(".mp4") ? "MP4" : "Desconocido";
                                    Object[] row = {
                                        fileName,
                                        fileType,
                                        "No descargado",
                                        peerName,
                                        "Acciones"
                                    };
                                    tableModel.addRow(row);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Error al obtener archivos de la red: " + e.getMessage());
                }
            }
        } catch (RemoteException e) {
            JOptionPane.showMessageDialog(this, "Error al refrescar lista de archivos: " + e.getMessage());
        }
    }

    class ButtonRenderer extends JButton implements TableCellRenderer {
        public ButtonRenderer() {
            setOpaque(true);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            setText("Acciones");
            return this;
        }
    }

    class ButtonEditor extends DefaultCellEditor {
        protected JButton button;
        private String label;
        private boolean isPushed;

        public ButtonEditor(JCheckBox checkBox) {
            super(checkBox);
            button = new JButton();
            button.setOpaque(true);
            button.addActionListener(e -> fireEditingStopped());
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            label = (value == null) ? "Acciones" : value.toString();
            button.setText(label);
            isPushed = true;
            return button;
        }

        @Override
        public Object getCellEditorValue() {
            if (isPushed) {
                // Mostrar menú contextual con opciones
                JPopupMenu popupMenu = new JPopupMenu();
                JMenuItem downloadItem = new JMenuItem("Descargar");
                JMenuItem playItem = new JMenuItem("Reproducir");
                JMenuItem infoItem = new JMenuItem("Ver info");

                downloadItem.addActionListener(e -> downloadSelectedFile());
                playItem.addActionListener(e -> playSelectedFile());
                infoItem.addActionListener(e -> showFileInfo());

                popupMenu.add(downloadItem);
                popupMenu.add(playItem);
                popupMenu.add(infoItem);

                popupMenu.show(button, button.getWidth() / 2, button.getHeight() / 2);
            }
            isPushed = false;
            return label;
        }

        @Override
        public boolean stopCellEditing() {
            isPushed = false;
            return super.stopCellEditing();
        }
    }

    private void showFileInfo() {
        int selectedRow = filesTable.getSelectedRow();
        if (selectedRow == -1) {
            return;
        }

        String fileName = (String) tableModel.getValueAt(selectedRow, 0);
        String peerName = (String) tableModel.getValueAt(selectedRow, 3);

        try {
            Registry registry = getRegistry();
            PeerInterface remotePeer = (PeerInterface) registry.lookup(peerName);
            Map<String, String> fileInfo = remotePeer.getFileInfo(fileName);

            StringBuilder infoText = new StringBuilder();
            infoText.append("Información del archivo:\n\n");
            infoText.append("Nombre: ").append(fileName).append("\n");

            for (Map.Entry<String, String> entry : fileInfo.entrySet()) {
                if (!entry.getKey().equals("nombre")) {
                    infoText.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
                }
            }

            JOptionPane.showMessageDialog(this, infoText.toString(), "Información del archivo", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error al obtener información: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        try {
            String name = JOptionPane.showInputDialog("Ingresa tu identificador");
            RMIApp app = new RMIApp(name);
            app.setVisible(true);
            app.setTitle("Peer '" + name + "'");
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
}
