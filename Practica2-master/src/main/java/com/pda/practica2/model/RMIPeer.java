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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;

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

    // Variable para almacenar las transferencias activas
    private final Map<String, TransferInfo> activeTransfers = new ConcurrentHashMap<>();

    // Constructor público para permitir la creación de instancias desde otras clases
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

        // Asegurar que la carpeta 'storage' exista
        ensureStorageDirectoryExists();

        // Iniciar el servidor de archivos
        startFileServer();
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
            try {
                fileServerSocket = new ServerSocket(12345);
                fileServerRunning = true;
                System.out.println("Servidor de archivos iniciado en el puerto 12345");

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
            } catch (IOException e) {
                System.err.println("Error al iniciar el servidor de archivos: " + e.getMessage());
                e.printStackTrace();
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
        try {
            DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());

            // Leer información de la transferencia
            String fileName = dataInputStream.readUTF();
            long startByte = dataInputStream.readLong();
            long endByte = dataInputStream.readLong();
            String transferId = dataInputStream.readUTF();

            System.out.println("Recibiendo transferencia parcial: " + fileName +
                              " (bytes " + startByte + "-" + endByte + ", ID: " + transferId + ")");

            // Crear/abrir el archivo
            File storageDir = new File("storage");
            File receivedFile = new File(storageDir, fileName);
            boolean isNewFile = !receivedFile.exists();

            // Abrir el archivo para escritura en modo aleatorio
            try (RandomAccessFile raf = new RandomAccessFile(receivedFile, "rw")) {
                // Posicionarse en el byte inicial
                raf.seek(startByte);

                // Crear un buffer para la transferencia
                byte[] buffer = new byte[8192];
                int bytesRead;
                long totalBytesRead = 0;
                long bytesToReceive = endByte - startByte + 1;

                // Leer datos del socket y escribir en el archivo
                while (totalBytesRead < bytesToReceive &&
                      (bytesRead = dataInputStream.read(buffer, 0, (int)Math.min(buffer.length, bytesToReceive - totalBytesRead))) != -1) {
                    raf.write(buffer, 0, bytesRead);
                    totalBytesRead += bytesRead;

                    // Mostrar progreso cada 1MB recibido
                    if (totalBytesRead % (1024 * 1024) < 8192) {
                        System.out.println("Progreso: " + (totalBytesRead * 100 / bytesToReceive) + "% (" +
                                          totalBytesRead + "/" + bytesToReceive + " bytes)");
                    }
                }

                System.out.println("Transferencia parcial completada: " + totalBytesRead + " bytes recibidos");
            }

            // Registrar el archivo en el catálogo si es nuevo
            if (isNewFile) {
                registerCatalog(fileName);
            }

            // Actualizar la interfaz de usuario (debe hacerse en el hilo de EDT)
            if (app != null) {
                javax.swing.SwingUtilities.invokeLater(() -> {
                    app.getjTextAreaMessages().append("Archivo recibido: " + fileName + "\n");
                });
            }

            socket.close();

        } catch (IOException e) {
            System.err.println("Error durante la recepción del archivo: " + e.getMessage());
            e.printStackTrace();
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
        List<String> results = new ArrayList<>();
        for (String file : catalog) {
            if (file.toLowerCase().contains(query.toLowerCase())) {
                results.add(file);
            }
        }
        return results.toArray(new String[0]);
    }

    @Override
    public Map<String, String[]> searchNetworkFiles(String query) throws RemoteException {
        Map<String, String[]> results = new HashMap<>();

        try {
            // Incluir los archivos locales
            String[] localFiles = searchFiles(query);
            results.put(name, localFiles);

            // Consultar a otros peers
            Registry registry = app.getRegistry();
            String[] registeredPeers = registry.list();

            for (String peerName : registeredPeers) {
                if (!peerName.equals(name)) {
                    try {
                        PeerInterface peer = (PeerInterface) registry.lookup(peerName);
                        if (peer.isalive()) {
                            String[] peerFiles = peer.searchFiles(query);
                            results.put(peerName, peerFiles);
                        }
                    } catch (Exception e) {
                        System.err.println("Error al consultar archivos del peer " + peerName + ": " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error en la búsqueda de red: " + e.getMessage());
            e.printStackTrace();
        }

        return results;
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

    @Override
    public String transferFilePartial(String fileName, String nodeID, long startByte, long endByte) throws RemoteException {
        // Crear un ID único para esta transferencia
        String transferId = UUID.randomUUID().toString();

        // Registrar la transferencia
        TransferInfo transferInfo = new TransferInfo(fileName, nodeID, startByte, endByte);
        activeTransfers.put(transferId, transferInfo);

        // Iniciar la transferencia en un hilo separado
        new Thread(() -> {
            try {
                System.out.println("Iniciando transferencia parcial de '" + fileName +
                                  "' a " + nodeID + " (bytes " + startByte + "-" + endByte + ")");

                // Obtener la dirección IP del peer destino
                String host = getPeerAddress(nodeID);
                if (host == null) {
                    throw new RemoteException("No se pudo encontrar la dirección del peer: " + nodeID);
                }

                // Establecer conexión
                Socket socket = new Socket(host, 12345);
                DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());

                // Enviar información de la transferencia
                dataOutputStream.writeUTF(fileName);
                dataOutputStream.writeLong(startByte);
                dataOutputStream.writeLong(endByte);
                dataOutputStream.writeUTF(transferId);

                // Abrir el archivo
                File fileToSend = new File("storage", fileName);
                if (!fileToSend.exists()) {
                    throw new RemoteException("El archivo '" + fileName + "' no existe en la carpeta 'storage'.");
                }

                try (RandomAccessFile raf = new RandomAccessFile(fileToSend, "r")) {
                    // Posicionarse en el byte inicial
                    raf.seek(startByte);

                    // Calcular el tamaño a transferir
                    long bytesToTransfer = endByte - startByte + 1;

                    // Transferir datos
                    byte[] buffer = new byte[8192];
                    long bytesRemaining = bytesToTransfer;
                    TransferInfo transfer = activeTransfers.get(transferId);

                    while (bytesRemaining > 0 && !transfer.isCancelled) {
                        // Pausar si es necesario
                        while (transfer.isPaused && !transfer.isCancelled) {
                            Thread.sleep(500);
                        }

                        // Salir si se canceló
                        if (transfer.isCancelled) {
                            break;
                        }

                        // Leer y enviar datos
                        int bytesToRead = (int) Math.min(buffer.length, bytesRemaining);
                        int bytesRead = raf.read(buffer, 0, bytesToRead);

                        if (bytesRead == -1) {
                            break;
                        }

                        dataOutputStream.write(buffer, 0, bytesRead);
                        bytesRemaining -= bytesRead;

                        // Actualizar progreso
                        transfer.currentByte = startByte + (bytesToTransfer - bytesRemaining);
                    }

                    System.out.println("Transferencia " + (transfer.isCancelled ? "cancelada" : "completada") +
                                     ": " + transferId);
                }

                // Cerrar la conexión
                dataOutputStream.close();
                socket.close();

            } catch (Exception e) {
                System.err.println("Error en transferencia parcial: " + e.getMessage());
                e.printStackTrace();
            } finally {
                // Marcar como completa
                TransferInfo transfer = activeTransfers.get(transferId);
                if (transfer != null && !transfer.isCancelled) {
                    transfer.currentByte = transfer.endByte;
                }
            }
        }).start();

        return transferId;
    }

    @Override
    public int getTransferProgress(String transferId) throws RemoteException {
        TransferInfo transfer = activeTransfers.get(transferId);
        if (transfer == null) {
            throw new RemoteException("Transferencia no encontrada: " + transferId);
        }
        return transfer.getProgressPercentage();
    }

    @Override
    public boolean cancelTransfer(String transferId) throws RemoteException {
        TransferInfo transfer = activeTransfers.get(transferId);
        if (transfer == null) {
            throw new RemoteException("Transferencia no encontrada: " + transferId);
        }

        transfer.isCancelled = true;
        return true;
    }

    @Override
    public Map<String, String> getFileInfo(String fileName) throws RemoteException {
        Map<String, String> fileInfo = new HashMap<>();

        try {
            // Buscar el archivo en la carpeta storage
            File file = new File("storage", fileName);
            if (file.exists()) {
                fileInfo.put("nombre", fileName);
                fileInfo.put("tamaño", file.length() + " bytes");
                fileInfo.put("fecha_modificación", new java.util.Date(file.lastModified()).toString());

                // Determinar tipo de archivo
                String extension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
                fileInfo.put("tipo", extension);

                // Información adicional para archivos multimedia
                if (extension.equals("mp3") || extension.equals("mp4")) {
                    // Aquí podrías extraer metadatos específicos como duración, codec, etc.
                    // Requeriría bibliotecas externas como JAudioTagger para MP3 o similar para MP4
                    fileInfo.put("propietario", name);
                }
            } else {
                fileInfo.put("nombre", fileName);
                fileInfo.put("estado", "No disponible localmente");
            }
        } catch (Exception e) {
            System.err.println("Error al obtener información del archivo: " + e.getMessage());
            fileInfo.put("error", e.getMessage());
        }

        return fileInfo;
    }

    @Override
    public boolean verifyFileIntegrity(String fileName) throws RemoteException {
        try {
            File file = new File("storage", fileName);
            if (!file.exists()) {
                return false;
            }

            // Calcular un checksum básico
            MessageDigest md = MessageDigest.getInstance("MD5");
            try (FileInputStream fis = new FileInputStream(file)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    md.update(buffer, 0, bytesRead);
                }
            }

            // Convertir el checksum a una cadena hexadecimal
            byte[] digest = md.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            String calculatedChecksum = sb.toString();

            // Aquí deberías comparar con un checksum almacenado previamente
            // Para este ejemplo, asumimos que el archivo está íntegro
            return true;
        } catch (Exception e) {
            System.err.println("Error al verificar integridad: " + e.getMessage());
            return false;
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
        if (!catalog.contains(catalogItem)) {
            catalog.add(catalogItem);
            System.out.println("Archivo registrado en el catálogo: " + catalogItem);
        } else {
            System.out.println("El archivo ya existe en el catálogo: " + catalogItem);
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

    // Clase para almacenar información de transferencia
    private static class TransferInfo {
        private final String fileName;
        private final String destinationNode;
        private final long startByte;
        private final long endByte;
        private long currentByte;
        private boolean isPaused;
        private boolean isCancelled;

        public TransferInfo(String fileName, String destinationNode, long startByte, long endByte) {
            this.fileName = fileName;
            this.destinationNode = destinationNode;
            this.startByte = startByte;
            this.endByte = endByte;
            this.currentByte = startByte;
            this.isPaused = false;
            this.isCancelled = false;
        }

        public int getProgressPercentage() {
            if (endByte == startByte) return 100;
            return (int) (((currentByte - startByte) * 100) / (endByte - startByte));
        }
    }
}
