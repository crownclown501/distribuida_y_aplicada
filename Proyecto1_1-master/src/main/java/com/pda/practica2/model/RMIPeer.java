package com.pda.practica2.model;

import com.pda.practica2.RMIApp;
import com.pda.practica2.interfaces.PeerInterface;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.rmi.registry.LocateRegistry;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.swing.table.DefaultTableModel;
import javax.swing.JTable;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

public class RMIPeer extends UnicastRemoteObject implements PeerInterface {
    private String name;
    private int id;
    private List<String> catalog;
    private String coordinator;
    private boolean electionInProgress;
    private boolean foundGreater;
    private List<String> peers;
    private RMIApp app;
    private ServerSocket fileServerSocket;
    private boolean fileServerRunning;
    // Modelo de tabla para archivos compartidos
    private DefaultTableModel sharedFilesTableModel;
    private JTable sharedFilesTable;
    private JScrollPane sharedFilesScrollPane;

    // Clase para representar un archivo compartido
    public static class SharedFile implements Serializable {
        private String fileName;
        private long fileSize;
        private String uploadedBy;
        private String uploadDate;

        public SharedFile(String fileName, long fileSize, String uploadedBy) {
            this.fileName = fileName;
            this.fileSize = fileSize;
            this.uploadedBy = uploadedBy;
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            this.uploadDate = sdf.format(new Date());
        }

        public String getFileName() {
            return fileName;
        }

        public long getFileSize() {
            return fileSize;
        }

        public String getUploadedBy() {
            return uploadedBy;
        }

        public String getUploadDate() {
            return uploadDate;
        }
    }
    
        public void connectToPeers() {
        try {
            // Obtener el registro RMI
            Registry registry = LocateRegistry.getRegistry("localhost", 1099);
            
            // Listar todos los peers registrados
            String[] peerNames = registry.list();
            
            for (String peerName : peerNames) {
                try {
                    // No conectarse a sí mismo
                    if (!peerName.equals(this.getName())) {
                        // Buscar la interfaz del peer remoto
                        PeerInterface remotePeer = (PeerInterface) registry.lookup(peerName);
                        
                        // Verificar si ya no está conectado
                        if (!connectedPeers.contains(remotePeer)) {
                            // Realizar la conexión
                            remotePeer.connect(this);
                            connectedPeers.add(remotePeer);
                            
                            // Opcional: Notificar sobre la conexión
                            System.out.println("Conectado a peer: " + peerName);
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Error conectando con peer " + peerName + ": " + e.getMessage());
                }
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
        
       // Implementación del método connect en la interfaz
    @Override
    public void connect(PeerInterface peer) throws RemoteException {
        // Lógica para establecer conexión
        if (!connectedPeers.contains(peer)) {
            connectedPeers.add(peer);
            System.out.println("Peer conectado: " + peer.getName());
        }
    }
    

    // Lista de archivos compartidos
    private List<SharedFile> sharedFiles;
    
    private List<PeerInterface> connectedPeers = new ArrayList<>();

   // In RMIPeer.java, update the constructor to call scanStorageDirectory
    public RMIPeer(String name, int id, RMIApp app) throws RemoteException {
        super();
        this.name = name;
        this.id = id;
        this.catalog = new ArrayList<>();
        this.coordinator = name; // Inicialmente, cada peer es su propio coordinador
        this.electionInProgress = false;
        this.foundGreater = false;
        this.peers = new ArrayList<>();
        this.app = app;
        this.fileServerRunning = false;
        this.sharedFiles = new ArrayList<>();

        // Inicializar la tabla de archivos compartidos
        initSharedFilesTable();

        // Asegurar que la carpeta 'storage' exista
        ensureStorageDirectoryExists();

        // Escaneo del directorio de storage para registrar archivos existentes
        scanStorageDirectory();

        // Iniciar el servidor de archivos
        startFileServer();

        ensureStorageDirectoryExists();

        loadExistingFilesIntoCatalog();  // Cargar archivos existentes en el catálogo

        // Registrar el peer en el Registry
        try {
            Registry registry = LocateRegistry.getRegistry(1099);
            registry.rebind(name, this);
            System.out.println("Peer registrado en el Registry: " + name); // Mensaje de depuración
        } catch (RemoteException e) {
            System.err.println("Error al registrar el peer en el Registry: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
   
    

    private void initSharedFilesTable() {
        // Crear el modelo de tabla con las columnas necesarias
        sharedFilesTableModel = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Hacer que todas las celdas no sean editables
            }
        };
        
        // Agregar columnas al modelo
        sharedFilesTableModel.addColumn("Nombre del Archivo");
        sharedFilesTableModel.addColumn("Tamaño (KB)");
        sharedFilesTableModel.addColumn("Subido por");
        sharedFilesTableModel.addColumn("Fecha/Hora");
        
        // Crear la tabla con el modelo
        sharedFilesTable = new JTable(sharedFilesTableModel);
        
        // Configurar la tabla para mejor visualización
        sharedFilesTable.getColumnModel().getColumn(0).setPreferredWidth(200);
        sharedFilesTable.getColumnModel().getColumn(1).setPreferredWidth(80);
        sharedFilesTable.getColumnModel().getColumn(2).setPreferredWidth(100);
        sharedFilesTable.getColumnModel().getColumn(3).setPreferredWidth(150);
        
        // Crear scroll pane para la tabla
        sharedFilesScrollPane = new JScrollPane(sharedFilesTable);
    }

    public JScrollPane getSharedFilesScrollPane() {
        return sharedFilesScrollPane;
    }

    private void ensureStorageDirectoryExists() {
        File storageDir = new File("storage");
        if (!storageDir.exists()) {
            storageDir.mkdir();
            System.out.println("Carpeta 'storage' creada en: " + storageDir.getAbsolutePath());
        } else {
            System.out.println("Carpeta 'storage' ya existe en: " + storageDir.getAbsolutePath());
        }
    }
    
    /**
     * Inicia un servidor en un hilo separado para recibir archivos
     */
public void startFileServer() {
    if (fileServerRunning) {
        System.out.println("El servidor de archivos ya está en ejecución");
        return;
    }

    new Thread(() -> {
        int port = 12345;
        while (port < 65535) {
            try {
                fileServerSocket = new ServerSocket(port);
                fileServerRunning = true;
                System.out.println("Servidor de archivos iniciado en el puerto " + port);
                break;
            } catch (IOException e) {
                port++;
            }
        }
        if (!fileServerRunning) {
            System.err.println("No se pudo encontrar un puerto disponible para el servidor de archivos");
            return;
        }
        while (fileServerRunning) {
            try {
                Socket clientSocket = fileServerSocket.accept();
                System.out.println("Nueva conexión recibida desde: " + clientSocket.getInetAddress());
                
                // Manejar la transferencia en un hilo separado
                new Thread(() -> handleFileTransfer(clientSocket)).start();
            } catch (IOException e) {
                
                if (fileServerRunning) {
                    System.err.println("Error al aceptar conexión: " + e.getMessage());
                }
            }
        }
    }).start();
}

    
    /**
     * Detiene el servidor de archivos
     */
    public void stopFileServer() {
        fileServerRunning = false;
        try {
            if (fileServerSocket != null && !fileServerSocket.isClosed()) {
                fileServerSocket.close();
            }
        } catch (IOException e) {
            System.err.println("Error al cerrar el servidor de archivos: " + e.getMessage());
        }
    }
    
    /**
     * Maneja la recepción de un archivo desde otro peer
     */
private void handleFileTransfer(Socket socket) {
    DataInputStream dataInputStream = null;
    FileOutputStream fileOutputStream = null;
    
    try {
        // Create a buffer to read the file name
        dataInputStream = new DataInputStream(socket.getInputStream());
        
        // Read the file name
        String fileName = dataInputStream.readUTF();
        System.out.println("Recibiendo archivo: " + fileName);
        
        // Create the file in the storage folder
        File storageDir = new File("storage");
        File receivedFile = new File(storageDir, fileName);
        
        // Open a stream to write to the file
        fileOutputStream = new FileOutputStream(receivedFile);
        
            // Crear un buffer para la transferencia
            byte[] buffer = new byte[8192]; // Buffer más grande para mejor rendimiento
        int bytesRead;
        long totalBytesRead = 0;
        
            // Leer datos del socket y escribir en el archivo
        while ((bytesRead = dataInputStream.read(buffer)) != -1) {
            fileOutputStream.write(buffer, 0, bytesRead);
            totalBytesRead += bytesRead;
                System.out.println("Bytes recibidos: " + totalBytesRead);
            }
        
            // Cerrar los flujos
            fileOutputStream.close();
            dataInputStream.close();
            socket.close();
            
        System.out.println("Archivo recibido con éxito: " + fileName + " (" + totalBytesRead + " bytes)");
        
        // Register the file in the catalog
        registerCatalog(fileName);
        
        // Create a SharedFile for this file
        SharedFile sharedFile = new SharedFile(fileName, receivedFile.length(), "Descargado");
        
        // Add to the list of shared files
        addSharedFile(sharedFile);
        
        // Update the user interface (must be done in the EDT thread)
        if (app != null) {
            SwingUtilities.invokeLater(() -> {
                app.getjTextAreaMessages().append("Archivo recibido: " + fileName + "\n");
            });
        }
        
    } catch (IOException e) {
        System.err.println("Error durante la recepción del archivo: " + e.getMessage());
        e.printStackTrace();
        
        // Update UI with error message
        if (app != null) {
            final String errorMsg = e.getMessage();
            SwingUtilities.invokeLater(() -> {
                app.getjTextAreaMessages().append("Error al recibir archivo: " + errorMsg + "\n");
            });
        }
    } finally {
        // Close all resources
        try {
            if (fileOutputStream != null) fileOutputStream.close();
            if (dataInputStream != null) dataInputStream.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            System.err.println("Error al cerrar recursos: " + e.getMessage());
        }
    }
}

    @Override
    public void message(String nodeID, String message) throws RemoteException {
        app.getjTextAreaMessages().append(nodeID + ": " + message + "\n");
    }

    @Override
    public void updatePeerList(String[] peers) throws RemoteException {
        this.peers.clear();
        this.peers.addAll(Arrays.asList(peers));
        System.out.println("Lista de peers actualizada: " + this.peers);
    }
    
@Override
public String[] searchFiles(String query) throws RemoteException {
    System.out.println("Buscando archivos con query: '" + query + "'");
    
    // If we are the coordinator, search in our shared files table
    if (name.equals(coordinator)) {
        List<String> results = new ArrayList<>();
        System.out.println("Buscando en " + sharedFiles.size() + " archivos compartidos");
        
        for (SharedFile file : sharedFiles) {
            if (file.getFileName().toLowerCase().contains(query.toLowerCase())) {
                results.add(file.getFileName() + " | " + 
                           (file.getFileSize() / 1024) + " KB | " + 
                           file.getUploadedBy() + " | " + 
                           file.getUploadDate());
            }
        }
        System.out.println("Encontrados " + results.size() + " resultados");
        return results.toArray(new String[0]);
    } else {
        // If we are not the coordinator, search on the coordinator
        try {
            System.out.println("Buscando a través del coordinador: " + coordinator);
            PeerInterface coordinatorPeer = (PeerInterface) app.getRegistry().lookup(coordinator);
            return coordinatorPeer.searchFiles(query);
        } catch (Exception e) {
            System.err.println("Error al buscar archivos en el coordinador: " + e.getMessage());
            e.printStackTrace();
            
            // Search in our local catalog as backup
            System.out.println("Usando catálogo local como respaldo");
            List<String> results = new ArrayList<>();
            for (String file : catalog) {
                if (file.toLowerCase().contains(query.toLowerCase())) {
                    results.add(file + " | Local | " + name);
                }
            }
            return results.toArray(new String[0]);
        }
    }
}

    @Override
    public void transferFile(String fileName, String nodeID) throws RemoteException {
        System.out.println("Iniciando transferencia de archivo '" + fileName + "' a " + nodeID);

        try {
            // Obtener la dirección IP del peer destino
            String host = getPeerAddress(nodeID);
            if (host == null) {
                throw new RemoteException("No se pudo encontrar la dirección del peer: " + nodeID);
            }
            
            System.out.println("Conectando a " + host + ":12345");

            // Establecer conexión con el peer destino
            Socket socket = new Socket(host, 12345);
            DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
            
            // Enviar el nombre del archivo primero
            dataOutputStream.writeUTF(fileName);
            System.out.println("Nombre de archivo enviado: " + fileName);

            // Asegurar que la carpeta 'storage' exista
            File storageDir = new File("storage");
            if (!storageDir.exists()) {
                storageDir.mkdir();
            }

            // Leer el archivo desde la carpeta 'storage'
            File fileToSend = new File(storageDir, fileName);
            if (!fileToSend.exists()) {
                throw new RemoteException("El archivo '" + fileName + "' no existe en la carpeta 'storage'.");
            }

            // Obtener tamaño del archivo para mostrar progreso
            long fileSize = fileToSend.length();
            System.out.println("Tamaño del archivo a enviar: " + fileSize + " bytes");
            
            FileInputStream fileInputStream = new FileInputStream(fileToSend);

            // Leer y enviar el archivo
            byte[] buffer = new byte[8192]; // Buffer más grande para mejor rendimiento
            int bytesRead;
            long totalBytesSent = 0;
            
            while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                dataOutputStream.write(buffer, 0, bytesRead);
                totalBytesSent += bytesRead;
                
                // Mostrar progreso cada 1MB enviado
                if (totalBytesSent % (1024 * 1024) < 8192) {
                    System.out.println("Progreso: " + (totalBytesSent * 100 / fileSize) + "% (" + 
                                      totalBytesSent + "/" + fileSize + " bytes)");
                }
            }

            System.out.println("Transferencia completada: " + totalBytesSent + " bytes enviados.");
            fileInputStream.close();
            dataOutputStream.flush();
            dataOutputStream.close();
            socket.close();

        } catch (IOException e) {
            System.err.println("Error durante la transferencia de archivo: " + e.getMessage());
            e.printStackTrace();
            throw new RemoteException("Error durante la transferencia de archivo: " + e.getMessage());
        }
    }

    private String getPeerAddress(String nodeID) {
        // Buscar en la lista de peers conocidos para encontrar la dirección IP correspondiente
        for (String peer : peers) {
            String[] parts = peer.split(":");
            if (parts.length >= 2 && parts[0].equals(nodeID)) {
                return parts[1]; // Retorna la dirección IP asociada con el nodeID
            }
        }

        // Si no se encuentra el peer en la lista, intentar obtenerlo del registro RMI
        try {
            Registry registry = app.getRegistry();
            String[] registeredPeers = registry.list();

            for (String registeredPeer : registeredPeers) {
                if (registeredPeer.equals(nodeID) || (registeredPeer.startsWith(nodeID + "_"))) {
                    // Obtener la referencia remota para extraer información de conexión
                    PeerInterface peer = (PeerInterface) registry.lookup(registeredPeer);

                    // En un entorno real, se podría implementar un método en la interfaz
                    // para obtener la dirección IP directamente, pero como alternativa
                    // usamos la información de la conexión remota
                    String remoteAddress = peer.toString();
                    if (remoteAddress.contains("endpoint:[")) {
                        int startIdx = remoteAddress.indexOf("endpoint:[") + 10;
                        int endIdx = remoteAddress.indexOf("]", startIdx);
                        if (endIdx > startIdx) {
                            String endpoint = remoteAddress.substring(startIdx, endIdx);
                            if (endpoint.contains(":")) {
                                return endpoint.substring(0, endpoint.indexOf(":"));
                            }
                        }
                    }
                    
                    // Si no podemos extraer la información, al menos sabemos que el peer existe
                    // Podemos intentar usar localhost o InetAddress.getLocalHost().getHostAddress()
                    try {
                        return InetAddress.getLocalHost().getHostAddress();
                    } catch (UnknownHostException uhe) {
                        return "localhost";
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error al buscar dirección del peer " + nodeID + ": " + e.getMessage());
            e.printStackTrace();
        }

        // Si no se encuentra, devolver la dirección local como fallback
        System.out.println("No se encontró dirección para " + nodeID + ", usando localhost como fallback");
        return "127.0.0.1";
    }

@Override
public void registerCatalog(String catalogItem) throws RemoteException {
    // Check if the file is already in the catalog to avoid duplicates
    if (!catalog.contains(catalogItem)) {
        catalog.add(catalogItem);
        System.out.println("Archivo registrado en el catálogo: " + catalogItem);
        
        // If this node is the coordinator, add the file to the shared files table
        if (name.equals(coordinator)) {
            File file = new File("storage/" + catalogItem);
            if (file.exists()) {
                SharedFile sharedFile = new SharedFile(catalogItem, file.length(), name);
                addSharedFile(sharedFile);
                System.out.println("Archivo añadido a la tabla de archivos compartidos: " + catalogItem);
            } else {
                System.out.println("Archivo no encontrado en storage: " + catalogItem);
            }
        } else {
            // If we are not the coordinator, notify the coordinator about the new file
            try {
                PeerInterface coordinatorPeer = (PeerInterface) app.getRegistry().lookup(coordinator);
                coordinatorPeer.notifyNewFile(catalogItem, name);
                System.out.println("Coordinador notificado sobre nuevo archivo: " + catalogItem);
            } catch (Exception e) {
                System.err.println("Error al notificar al coordinador sobre el nuevo archivo: " + e.getMessage());
                e.printStackTrace();
            }
        }
    } else {
        System.out.println("El archivo ya existe en el catálogo: " + catalogItem);
    }
}

    /**
     * Método para ser notificado de un nuevo archivo por otro peer
     */
@Override
public void notifyNewFile(String fileName, String peerName) throws RemoteException {
    if (name.equals(coordinator)) {
        // Intentar obtener información sobre el tamaño del archivo
        long fileSize = 0;
        try {
            // Intentar obtener la referencia al peer para acceder a su archivo
            PeerInterface peer = (PeerInterface) app.getRegistry().lookup(peerName);
            fileSize = peer.getFileSize(fileName);
        } catch (Exception e) {
            System.err.println("Error al obtener tamaño del archivo: " + e.getMessage());
            fileSize = 0; // Si no podemos obtener el tamaño, asumimos 0
        }

        final SharedFile sharedFile = new SharedFile(fileName, fileSize, peerName);

        // Asegúrate de que la actualización de la GUI se realice en el EDT
        SwingUtilities.invokeLater(() -> {
            addSharedFile(sharedFile);
        });
    }
}

    /**
     * Método para obtener el tamaño de un archivo
     */
    @Override
    public long getFileSize(String fileName) throws RemoteException {
        File file = new File("storage/" + fileName);
        if (file.exists()) {
            return file.length();
        }
        return 0;
    }

    /**
     * Método para descargar un archivo de otro peer
     */
    @Override
    public void downloadFile(String fileName, String fromPeer) throws RemoteException {
        try {
            // Obtener la referencia al peer que tiene el archivo
            PeerInterface peer = (PeerInterface) app.getRegistry().lookup(fromPeer);
            
            // Solicitar la transferencia del archivo
            peer.transferFile(fileName, name);
        } catch (Exception e) {
            System.err.println("Error al descargar archivo: " + e.getMessage());
            e.printStackTrace();
            throw new RemoteException("Error al descargar archivo: " + e.getMessage());
        }
    }

    /**
     * Método para agregar un archivo compartido a la tabla
     */
    private void addSharedFile(SharedFile file) {
        // Agregar a la lista
        synchronized (sharedFiles) {
            // Verificar si ya existe
            boolean exists = false;
            for (SharedFile existing : sharedFiles) {
                if (existing.getFileName().equals(file.getFileName())) {
                    exists = true;
                    break;
                }
            }
            
            if (!exists) {
                sharedFiles.add(file);
                
                // Actualizar el modelo de tabla en el hilo de EDT
                SwingUtilities.invokeLater(() -> {
                    sharedFilesTableModel.addRow(new Object[]{
                        file.getFileName(),
                        file.getFileSize() / 1024, // Convertir a KB
                        file.getUploadedBy(),
                        file.getUploadDate()
                    });
                });
            }
        }
    }

    /**
     * Método para guardar un archivo en la carpeta storage
     * @param sourceFile El archivo fuente
     * @return true si se guardó correctamente, false en caso contrario
     */
    public boolean saveFileToStorage(File sourceFile) {
        try {
            File storageDir = new File("storage");
            if (!storageDir.exists()) {
                storageDir.mkdir();
            }
            
            File destFile = new File(storageDir, sourceFile.getName());
            Files.copy(sourceFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            
            // Registrar en el catálogo
            registerCatalog(sourceFile.getName());
            
            // Agregar a la tabla de archivos compartidos si somos el coordinador
            if (name.equals(coordinator)) {
                SharedFile sharedFile = new SharedFile(sourceFile.getName(), destFile.length(), name);
                addSharedFile(sharedFile);
            }
            
            return true;
        } catch (Exception e) {
            System.err.println("Error al guardar archivo en storage: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public String[] getCatalogs(String type) throws RemoteException {
        List<String> filteredCatalog = new ArrayList<>();
        for (String file : catalog) {
            if (file.toLowerCase().endsWith("." + type.toLowerCase())) {
                filteredCatalog.add(file);
            }
        }
        return filteredCatalog.toArray(new String[0]);
    }

    /**
     * Método para obtener información de todos los archivos compartidos
     */
    @Override
    public List<SharedFile> getAllSharedFiles() throws RemoteException {
        return new ArrayList<>(sharedFiles);
    }

    @Override
    public void startElection(String nameNode, int nodeId) throws RemoteException {
        electionInProgress = true;
        foundGreater = false;

        if (nameNode.equals(name)) {
            System.out.println("Comenzaste la elección...");

            Registry registry = app.getRegistry(); // Obtener el Registry desde RMIApp
            for (String nodeName : registry.list()) {
                String[] nameAndId = nodeName.split("_");

                if (!nodeName.equals(name) && Integer.parseInt(nameAndId[1]) > id) {
                    try {
                        PeerInterface stub = (PeerInterface) registry.lookup(nodeName);
                        System.out.println("Enviando mensaje de elección a: " + nodeName);
                        stub.startElection(nameNode, nodeId);
                        foundGreater = true;
                    } catch (NotBoundException e) {
                        System.err.println("Peer no encontrado: " + nodeName);
                        e.printStackTrace();
                    }
                }
            }
            if (!foundGreater) {
                iWon(name);
            }
        } else {
            System.out.println("Petición recibida de: " + nameNode);
            sendOk(name, nameNode);
        }
    }

    @Override
    public void sendOk(String where, String to) throws RemoteException {
        if (!name.equals(to)) {
            try {
                PeerInterface stub = (PeerInterface) app.getRegistry().lookup(to);
                System.out.println("Enviando OK a " + to);
                stub.sendOk(where, to);
                startElection(name, id);
            } catch (NotBoundException e) {
                System.err.println("Peer no encontrado: " + to);
                e.printStackTrace();
            } catch (RemoteException e) {
                System.err.println("Error de comunicación remota con el peer: " + to);
                e.printStackTrace();
                throw e; // Re-lanzar la excepción para que sea manejada por el llamador si es necesario
            }
        } else {
            System.out.println(where + " contestó con Ok..");
        }
    }

    @Override
    public void iWon(String node) throws RemoteException {
        coordinator = node;
        app.updateCoordinator(node);
        electionInProgress = false;
        if (node.equals(name)) {
            System.out.println("Has ganado la elección.");
            System.out.println("Notificando a los otros nodos.....");
            Registry registry = app.getRegistry(); // Obtener el Registry desde RMIApp
            for (String nodeName : registry.list()) {
                if (!nodeName.equals(name)) {
                    try {
                        PeerInterface stub = (PeerInterface) registry.lookup(nodeName);
                        stub.iWon(node);
                        
                        // Solicitar los archivos compartidos de cada peer
                        try {
                            List<SharedFile> peerFiles = stub.getAllSharedFiles();
                            for (SharedFile file : peerFiles) {
                                addSharedFile(file);
                            }
                        } catch (Exception e) {
                            System.err.println("Error al obtener archivos compartidos del peer " + nodeName + ": " + e.getMessage());
                        }
                    } catch (NotBoundException e) {
                        System.err.println("Peer no encontrado: " + nodeName);
                        e.printStackTrace();
                    }
                }
            }
            System.out.println("Nodo " + node + " es el nuevo coordinador\n");
        } else {
            System.out.println("Nodo " + node + " ganó la elección.");
            System.out.println("Nodo " + node + " es el nuevo coordinador\n");
        }
    }

    @Override
    public void updateCoor(String coordinator) throws RemoteException {
        this.coordinator = coordinator;
        System.out.println("Coordinador actualizado a: " + coordinator);
        app.getjLabelCoor().setText("Coordinador: " + coordinator);
    }

    @Override
    public boolean isalive() throws RemoteException {
        System.out.println("Peer " + name + " está activo.");
        return true;
    }

    @Override
    public void updatePeers(String peers) throws RemoteException {
        this.peers = Arrays.asList(peers.split(","));
        System.out.println("Lista de peers actualizada: " + this.peers);
        app.getjTextAreaPeers().setText(String.join("\n", this.peers));
    }

    @Override
    public String getName() throws RemoteException {
        return name;
    }

    @Override
    public String getCoordinator() throws RemoteException {
        return coordinator;
    }

    @Override
    public boolean getelectionInProgress() throws RemoteException {
        return electionInProgress;
    }

    @Override
    public int getId() throws RemoteException {
        return id;
    }
    
    private void scanStorageDirectory() {
    File storageDir = new File("storage");
    if (storageDir.exists() && storageDir.isDirectory()) {
        File[] files = storageDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    try {
                        // Register the file in the catalog
                        registerCatalog(file.getName());
                        System.out.println("Archivo encontrado en storage y registrado: " + file.getName());
                    } catch (RemoteException e) {
                        System.err.println("Error al registrar archivo existente: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
    
    // Add this method to RMIPeer.java
private void loadExistingFilesIntoCatalog() {
    File storageDir = new File("storage");
    if (storageDir.exists() && storageDir.isDirectory()) {
        File[] files = storageDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    try {
                        // Only add files that aren't already in the catalog
                        if (!catalog.contains(file.getName())) {
                            registerCatalog(file.getName());
                        }
                    } catch (RemoteException e) {
                        System.err.println("Error al registrar archivo existente: " + e.getMessage());
                    }
                }
            }
        }
    }
}

public void sendMessageToAllPeers(String message) {
    for (String peerName : peers) {
        try {
            PeerInterface peer = (PeerInterface) app.getRegistry().lookup(peerName);
            peer.sendChatMessage(name, message);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}

public void sendChatMessage(String nodeID, String message) throws RemoteException {
    SwingUtilities.invokeLater(() -> {
        if (app.getChatPanel() != null) {
            app.getChatPanel().getChatArea().append(nodeID + ": " + message + "\n");
        }
    });
}

    // Método para obtener la lista de peers conectados
    public List<PeerInterface> getConnectedPeers() {
        return connectedPeers;
    }



    
    
}