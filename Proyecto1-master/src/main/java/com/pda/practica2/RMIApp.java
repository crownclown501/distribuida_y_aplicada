package com.pda.practica2;

import com.pda.practica2.interfaces.PeerInterface;
import com.pda.practica2.model.RMIPeer;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Arrays;
import java.util.List;

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

    public RMIApp(String nodeID) throws RemoteException {
        super("Intercambio de Archivos P2P");
        setSize(1000, 700); // Aumentamos el tamaño para acomodar las nuevas funcionalidades
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Panel principal con divisiones
        JSplitPane mainSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        mainSplitPane.setResizeWeight(0.6); // La parte superior ocupa 60% del espacio
        
        // Panel superior con búsqueda y resultados
        JPanel topPanel = new JPanel(new BorderLayout());
        
        // Campo de búsqueda y botones
        searchField = new JTextField(20);
        searchButton = new JButton("Buscar");
        searchButton.addActionListener(e -> searchFiles(searchField.getText()));

        uploadButton = new JButton("Subir Archivo");
        uploadButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                uploadFile();
            }
        });

        JPanel searchPanel = new JPanel();
        searchPanel.add(new JLabel("Buscar:"));
        searchPanel.add(searchField);
        searchPanel.add(searchButton);
        searchPanel.add(uploadButton);
        topPanel.add(searchPanel, BorderLayout.NORTH);

        // Panel de resultados de búsqueda con capacidad de descarga
        JPanel resultsPanel = new JPanel(new BorderLayout());
        textAreaSearchResults = new JTextArea();
        JScrollPane scrollPaneSearchResults = new JScrollPane(textAreaSearchResults);
        
        // Agregar botón de descarga para los resultados de búsqueda
        JButton downloadSelectedButton = new JButton("Descargar seleccionado");
        downloadSelectedButton.addActionListener(e -> downloadSelectedFile());
        
        resultsPanel.add(scrollPaneSearchResults, BorderLayout.CENTER);
        resultsPanel.add(downloadSelectedButton, BorderLayout.SOUTH);
        
        topPanel.add(resultsPanel, BorderLayout.CENTER);
        
        // Label para mostrar el coordinador
        jLabelCoor = new JLabel("Coordinador: ");
        topPanel.add(jLabelCoor, BorderLayout.WEST);
        
        mainSplitPane.setTopComponent(topPanel);
        
        // Panel inferior con pestañas
        JTabbedPane tabbedPane = new JTabbedPane();
        
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
        
        // Área para visualizar/reproducir archivos
        filePreviewPanel = new JPanel(new BorderLayout());
        filePreviewPanel.add(new JLabel("Vista previa de archivos"), BorderLayout.NORTH);
        tabbedPane.addTab("Vista previa", filePreviewPanel);
        
        mainSplitPane.setBottomComponent(tabbedPane);
        
        // Agregar el panel principal al frame
        add(mainSplitPane, BorderLayout.CENTER);
        
        // Iniciar el registro RMI
        try {
            registry = LocateRegistry.createRegistry(1099);
            peer = new RMIPeer(nodeID, 1, this);
            registry.rebind(nodeID, peer);
            updatePeersList();
            
            // Obtener la referencia a la tabla de archivos compartidos del peer
            sharedFilesScrollPane = peer.getSharedFilesScrollPane();
            coordinatorPanel.add(sharedFilesScrollPane, BorderLayout.CENTER);
        } catch (RemoteException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error al iniciar el servicio RMI: " + ex.getMessage(), 
                                         "Error", JOptionPane.ERROR_MESSAGE);
        }
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

    private void downloadSelectedFile() {
        // Obtener la línea seleccionada en el área de resultados
        String selectedText = textAreaSearchResults.getSelectedText();
        if (selectedText == null || selectedText.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Por favor, selecciona un archivo para descargar", 
                                         "Selección requerida", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // Extraer el nombre del archivo y el peer que lo tiene
        String[] parts = selectedText.split("\\|");
        if (parts.length < 3) {
            JOptionPane.showMessageDialog(this, "Formato de selección incorrecto. Selecciona una línea completa", 
                                         "Error de formato", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        String fileName = parts[0].trim();
        String peerName = parts[2].trim();
        
        try {
            // Llamar al método de descarga del peer
            peer.downloadFile(fileName, peerName);
            JOptionPane.showMessageDialog(this, "Descarga iniciada: " + fileName + " desde " + peerName, 
                                         "Descarga iniciada", JOptionPane.INFORMATION_MESSAGE);
        } catch (RemoteException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error al iniciar la descarga: " + ex.getMessage(), 
                                         "Error", JOptionPane.ERROR_MESSAGE);
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
                        "Archivo subido correctamente: " + file.getName(), 
                        "Éxito", 
                        JOptionPane.INFORMATION_MESSAGE);
                    updateCatalogs();
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

    private void updateCatalogs() {
        try {
            String[] mp3Files = peer.getCatalogs("mp3");
            String[] mp4Files = peer.getCatalogs("mp4");
            catalogMP3.setText(String.join("\n", mp3Files));
            catalogMP4.setText(String.join("\n", mp4Files));
        } catch (RemoteException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, 
                "Error al actualizar catálogos: " + e.getMessage(), 
                "Error", 
                JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updatePeersList() {
        try {
            StringBuilder sb = new StringBuilder("Peers conectados:\n");
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
        if (query == null || query.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "Por favor, ingresa un término de búsqueda", 
                "Búsqueda vacía", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        try {
            String[] results = peer.searchFiles(query);
            if (results.length > 0) {
                StringBuilder sb = new StringBuilder("Resultados de búsqueda para '" + query + "':\n");
                for (String result : results) {
                    sb.append(result).append("\n");
                }
                textAreaSearchResults.setText(sb.toString());
            } else {
                textAreaSearchResults.setText("No se encontraron resultados para '" + query + "'");
            }
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
            // Asegurarse de que el panel esté visible en el tab del coordinador
            coordinatorPanel.removeAll();
            coordinatorPanel.add(new JLabel("Archivos compartidos en la red (Coordinador)"), BorderLayout.NORTH);
            coordinatorPanel.add(sharedFilesScrollPane, BorderLayout.CENTER);
            coordinatorPanel.revalidate();
            coordinatorPanel.repaint();
        }
    }
    
    

    public static void main(String[] args) {
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