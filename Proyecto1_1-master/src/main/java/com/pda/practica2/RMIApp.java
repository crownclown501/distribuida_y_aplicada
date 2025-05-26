package com.pda.practica2;

import com.pda.practica2.interfaces.PeerInterface;
import com.pda.practica2.model.ChatPanel;
import com.pda.practica2.model.PeersPanel;
import com.pda.practica2.model.RMIPeer;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Arrays;
import java.util.List;
import javax.swing.table.DefaultTableModel;

public class RMIApp extends JFrame {
    private RMIPeer peer;
    private Registry registry;
    private JTextArea textAreaSearchResults;
    private JTextArea jTextAreaMessages; // Componente para mostrar mensajes
    private JTextField searchField;
    private JButton searchButton;
    private JButton uploadButton;
    private JTextArea catalogMP3;
    private JTextArea catalogMP4;
    private JPanel filePreviewPanel;
    private JLabel jLabelCoor; // Componente para mostrar el coordinador
    private JPanel coordinatorPanel; // Panel para mostrar la tabla del coordinador
    private JScrollPane sharedFilesScrollPane; // ScrollPane para la tabla del coordinador
    private boolean isCoordinator = false; // Indica si este peer es el coordinador
    private JTable resultsTable;
    private PeersPanel peersPanel;
    private JTabbedPane tabbedPane; // Declaración de tabbedPane
    private JScrollPane resultsScrollPane; // Referencia al JScrollPane de resultados
    private JPanel resultsPanel; // Add this declaration
    private ChatPanel chatPanel; // Declarar la variable de instancia

    
    
    

    public RMIApp(String nodeID) throws RemoteException {
    super("Intercambio de Archivos P2P");
    setSize(1000, 700);
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    setLayout(new BorderLayout());

    // Inicializar el JScrollPane de resultados
    resultsScrollPane = new JScrollPane();

    // Inicialización de tabbedPane
    tabbedPane = new JTabbedPane();

    // Panel principal con divisiones
    JSplitPane mainSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
    mainSplitPane.setResizeWeight(0.6);

    // Panel superior con búsqueda y resultados
    JPanel topPanel = new JPanel(new BorderLayout());

    // Campo de búsqueda y botones
    searchField = new JTextField(20);
    searchButton = new JButton("Buscar");
    searchButton.addActionListener(e -> searchFiles(searchField.getText()));

    uploadButton = new JButton("Subir Archivo");
    uploadButton.addActionListener(e -> uploadFile());

    JPanel searchPanel = new JPanel();
    searchPanel.add(new JLabel("Buscar:"));
    searchPanel.add(searchField);
    searchPanel.add(searchButton);
    searchPanel.add(uploadButton);
    topPanel.add(searchPanel, BorderLayout.NORTH);

    // Panel de resultados de búsqueda con capacidad de descarga
    resultsPanel = new JPanel(new BorderLayout());
    textAreaSearchResults = new JTextArea();
    resultsScrollPane = new JScrollPane(textAreaSearchResults);

    // Agregar botón de descarga para los resultados de búsqueda
    JButton downloadSelectedButton = new JButton("Descargar seleccionados");
    downloadSelectedButton.addActionListener(e -> downloadSelectedFile());

    resultsPanel.add(resultsScrollPane, BorderLayout.CENTER);
    resultsPanel.add(downloadSelectedButton, BorderLayout.SOUTH);

    topPanel.add(resultsPanel, BorderLayout.CENTER);

    // Label para mostrar el coordinador
    jLabelCoor = new JLabel("Coordinador: ");
    topPanel.add(jLabelCoor, BorderLayout.WEST);

    mainSplitPane.setTopComponent(topPanel);

    // Panel inferior con pestañas
    tabbedPane = new JTabbedPane();
        
    // Panel de catálogos
    JPanel catalogsPanel = new JPanel(new GridLayout(1, 2));

    catalogMP3 = new JTextArea();
    JScrollPane scrollPaneCatalogMP3 = new JScrollPane(catalogMP3);
    JPanel mp3Panel = new JPanel(new BorderLayout());
    mp3Panel.add(new JLabel("Catálogo MP3"), BorderLayout.NORTH);
    mp3Panel.add(scrollPaneCatalogMP3, BorderLayout.CENTER);

    catalogMP4 = new JTextArea();
    JScrollPane scrollPaneCatalogMP4 = new JScrollPane(catalogMP4);
    JPanel mp4Panel = new JPanel(new BorderLayout());
    mp4Panel.add(new JLabel("Catálogo MP4"), BorderLayout.NORTH);
    mp4Panel.add(scrollPaneCatalogMP4, BorderLayout.CENTER);

    catalogsPanel.add(mp3Panel);
    catalogsPanel.add(mp4Panel);

    tabbedPane.addTab("Catálogos", catalogsPanel);

    // Panel de mensajes
    jTextAreaMessages = new JTextArea();
    JScrollPane scrollPaneMessages = new JScrollPane(jTextAreaMessages);
    tabbedPane.addTab("Mensajes", scrollPaneMessages);

    // Panel del coordinador (se mostrará cuando este peer sea el coordinador)
    coordinatorPanel = new JPanel(new BorderLayout());
    coordinatorPanel.add(new JLabel("Archivos compartidos en la red (solo visible para el coordinador)"),
                        BorderLayout.NORTH);
    tabbedPane.addTab("Panel del Coordinador", coordinatorPanel);

    // Crear el panel de peers
    peersPanel = new PeersPanel();

    // Agregar el panel de peers a la pestaña de "Peers Conectados"
    tabbedPane.addTab("Peers Conectados", peersPanel);

    // Agregar el panel principal al frame
    add(mainSplitPane, BorderLayout.CENTER);
        
    // Iniciar el registro RMI
    try {
        // Buscar un puerto disponible
        int port = findAvailablePort();
        
        // Crear el registro RMI en el puerto disponible
        registry = LocateRegistry.createRegistry(port);
        
        // Configurar el hostname para la conexión RMI
        System.setProperty("java.rmi.server.hostname", "localhost");
        
        peer = new RMIPeer(nodeID, 1, this);
        registry.rebind(nodeID, peer);
        System.out.println("Peer registrado en el Registry en el puerto " + port + ": " + nodeID);

        // Actualizar la lista de peers después de registrar el peer local
        updatePeersList();
        updateCatalogs();

        // Incluir el peer local en la lista de peers
        SwingUtilities.invokeLater(() -> {
            peersPanel.updatePeers(new String[]{nodeID + ":localhost:" + port});
        });

    } catch (RemoteException e) {
        e.printStackTrace();
        JOptionPane.showMessageDialog(this, "Error al iniciar el servicio RMI: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }

    // Crear y agregar el panel de chat después de inicializar peer
    chatPanel = new ChatPanel(peer);
    tabbedPane.addTab("Chat", chatPanel);

    mainSplitPane.setBottomComponent(tabbedPane);

    // Agregar el panel principal al frame
    add(mainSplitPane, BorderLayout.CENTER);

    if (peer.getSharedFilesScrollPane() != null) {
        sharedFilesScrollPane = peer.getSharedFilesScrollPane();
        coordinatorPanel.add(sharedFilesScrollPane, BorderLayout.CENTER);
    }
}
    
    

    // Método para encontrar un puerto disponible
    private int findAvailablePort() {
        int port = 1099;
        while (port < 65535) {
            try {
                ServerSocket serverSocket = new ServerSocket(port);
                serverSocket.close();
                return port;
            } catch (IOException e) {
                port++;
            }
        }
        throw new RuntimeException("No se pudo encontrar un puerto disponible");
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
        return textAreaSearchResults; // Asumiendo que los peers se muestran aquí
    }

// Replace the downloadSelectedFile method in RMIApp.java
    private void downloadSelectedFile() {
        if (resultsTable == null) {
            JOptionPane.showMessageDialog(this,
                "No hay resultados de búsqueda disponibles",
                "Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        DefaultTableModel model = (DefaultTableModel) resultsTable.getModel();
        boolean fileSelected = false;

        for (int i = 0; i < model.getRowCount(); i++) {
            Boolean selected = (Boolean) model.getValueAt(i, 0);
            if (selected) {
                fileSelected = true;
                String fileName = (String) model.getValueAt(i, 1);
                String peerName = (String) model.getValueAt(i, 3);

                try {
                    // Llamar al método de descarga del peer
                    peer.downloadFile(fileName, peerName);
                    JOptionPane.showMessageDialog(this,
                        "Descarga iniciada: " + fileName + " desde " + peerName,
                        "Descarga iniciada", JOptionPane.INFORMATION_MESSAGE);
                } catch (RemoteException ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(this,
                        "Error al iniciar la descarga: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }

        if (!fileSelected) {
            JOptionPane.showMessageDialog(this,
                "Por favor, selecciona al menos un archivo para descargar",
                "Selección requerida", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void uploadFile() {
        JFileChooser fileChooser = new JFileChooser();
        int selection = fileChooser.showOpenDialog(this);
        if (selection == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try {
                // Verificar que sea un archivo mp3 o mp4
                String fileName = file.getName().toLowerCase();
                if (!fileName.endsWith(".mp3") && !fileName.endsWith(".mp4")) {
                    JOptionPane.showMessageDialog(this,
                        "Solo se permiten archivos MP3 o MP4",
                        "Tipo de archivo no soportado",
                        JOptionPane.WARNING_MESSAGE);
                    return;
                }

                // Usar el método en RMIPeer para guardar y registrar el archivo
                boolean success = peer.saveFileToStorage(file);

                if (success) {
                    JOptionPane.showMessageDialog(this,
                        "Archivo subido correctamente: " + file.getName(), "Éxito",  JOptionPane.INFORMATION_MESSAGE);
                    updateCatalogs();  // Update catalogs after successful upload
                } else {
                    JOptionPane.showMessageDialog(this,
                        "Error al subir el archivo",
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this,
                    "Error al subir el archivo: " + ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }

// In RMIApp.java
    private void updateCatalogs() {
        try {
            // Get catalogs from the peer
            String[] mp3Files = peer.getCatalogs("mp3");
            String[] mp4Files = peer.getCatalogs("mp4");

            // Clear the text areas first
            catalogMP3.setText("");
            catalogMP4.setText("");

            // Add the files to the text areas
            if (mp3Files.length > 0) {
                catalogMP3.setText(String.join("\n", mp3Files));
            }

            if (mp4Files.length > 0) {
                catalogMP4.setText(String.join("\n", mp4Files));
            }

            // Log the update
            System.out.println("Catálogos actualizados: " + mp3Files.length + " archivos MP3, "
                                + mp4Files.length + " archivos MP4");
        } catch (RemoteException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "Error al actualizar catálogos: " + e.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    public void updatePeersList() {
        try {
            String[] peers = registry.list();
            System.out.println("Actualizando lista de peers: " + Arrays.toString(peers));

            // Asegúrate de que la actualización de la GUI se realice en el EDT
            SwingUtilities.invokeLater(() -> {
                peersPanel.updatePeers(peers);
            });
        } catch (RemoteException ex) {
            ex.printStackTrace();
        }
    }



// In RMIApp.java, modify the searchFiles method
    public void searchFiles(String query) {
        if (query == null || query.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Por favor, ingresa un término de búsqueda",
                "Búsqueda vacía",
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            String[] resultsArray = peer.searchFiles(query);

            // Crear un nuevo modelo de tabla para los resultados de búsqueda
            DefaultTableModel model = new DefaultTableModel() {
                @Override
                public Class<?> getColumnClass(int columnIndex) {
                    return columnIndex == 0 ? Boolean.class : String.class;
                }

                @Override
                public boolean isCellEditable(int row, int column) {
                    return column == 0; // Solo la columna de checkbox es editable
                }
            };

            // Definir las columnas
            model.addColumn("Seleccionar");
            model.addColumn("Nombre");
            model.addColumn("Tamaño");
            model.addColumn("Subido por");
            model.addColumn("Fecha");

            // Añadir los resultados al modelo
            for (String result : resultsArray) {
                String[] parts = result.split("\\|");
                if (parts.length >= 3) {
                    String fileName = parts[0].trim();
                    String fileSize = parts[1].trim();
                    String uploader = parts[2].trim();
                    String date = parts.length > 3 ? parts[3].trim() : "";

                    model.addRow(new Object[]{false, fileName, fileSize, uploader, date});
                }
            }

            // Crear una nueva tabla y reemplazar el área de texto
            resultsTable = new JTable(model);
            resultsTable.getColumnModel().getColumn(0).setMaxWidth(80);
            resultsTable.getColumnModel().getColumn(1).setPreferredWidth(200);
            resultsTable.getColumnModel().getColumn(2).setPreferredWidth(80);
            resultsTable.getColumnModel().getColumn(3).setPreferredWidth(100);
            resultsTable.getColumnModel().getColumn(4).setPreferredWidth(150);

            // Actualizar el JScrollPane con la nueva tabla
            resultsScrollPane.setViewportView(resultsTable);

            // Actualizar la referencia a la tabla de resultados
            this.resultsTable = resultsTable;
            resultsScrollPane.revalidate();
            resultsScrollPane.repaint();

        } catch (RemoteException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "Error al buscar archivos: " + e.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }




    public void updateCoordinator(String coordinator) {
        jLabelCoor.setText("Coordinador: " + coordinator);

        // Verificar si este peer es el coordinador
        try {
            isCoordinator = peer.getName().equals(coordinator);

            // Si este peer es el coordinador, mostrar su panel especial
            if (isCoordinator) {
                jTextAreaMessages.append("¡Este peer ahora es el coordinador de la red!\n");
                // Actualizar la tabla de archivos compartidos
                refreshSharedFilesTable();
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * Actualiza la tabla de archivos compartidos en el panel del coordinador
     */
    private void refreshSharedFilesTable() {
        if (isCoordinator && sharedFilesScrollPane != null) {
            // Clear existing components
            coordinatorPanel.removeAll();

            // Add the title label
            coordinatorPanel.add(new JLabel("Archivos compartidos en la red (Coordinador)"), BorderLayout.NORTH);

            // Add the scroll pane containing the table
            coordinatorPanel.add(sharedFilesScrollPane, BorderLayout.CENTER);

            // Request layout update
            coordinatorPanel.revalidate();
            coordinatorPanel.repaint();
        }
    }


    public ChatPanel getChatPanel() {
        return chatPanel; // Ahora chatPanel está definido y accesible
    }

    
// Modificar el método main para manejar múltiples peers
public static void main(String[] args) {
     System.setProperty("java.rmi.server.hostname", "localhost");
    try {
        String name = JOptionPane.showInputDialog("Ingresa tu identificador");
        if (name == null || name.trim().isEmpty()) {
            JOptionPane.showMessageDialog(null, 
                "Identificador no válido. La aplicación se cerrará.", 
                "Error", 
                JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        }
        
        RMIApp app = new RMIApp(name);
        app.setVisible(true);
        app.setTitle("Peer '" + name + "'");
        app.setLocationRelativeTo(null); // Centrar en pantalla
    } catch (RemoteException e) {
        e.printStackTrace();
        JOptionPane.showMessageDialog(null, 
            "Error al iniciar la aplicación: " + e.getMessage(), 
            "Error", 
            JOptionPane.ERROR_MESSAGE);
    }
}
}
