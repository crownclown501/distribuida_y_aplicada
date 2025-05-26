package org.example.server.RMIChat;

import java.io.*;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.RemoteServer;
import java.rmi.server.ServerNotActiveException;
import java.rmi.server.UnicastRemoteObject;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatServer extends UnicastRemoteObject implements ChatRemote {
    private Map<String, ChatRemote> clients = new HashMap<>();
    private Map<String, ClientInfo> clientInfo = new HashMap<>();
    private static final String LOG_FILE_PATH = "client_history.txt";
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // Lista para mantener el historial completo (incluyendo sesiones anteriores)
    private List<ClientInfo> clientHistory = new ArrayList<>();

    public ChatServer() throws RemoteException {
        super(0); // Permitir conexiones desde cualquier host
        loadClientHistory(); // Cargar el historial previo desde el archivo
    }

    @Override
    public void sendMessage(String username, String message) throws RemoteException {
        for (ChatRemote client : clients.values()) {
            try {
                client.receiveMessage(username, message);
            } catch (RemoteException e) {
                log("Error al enviar mensaje a un cliente: " + e.getMessage());
                // Podríamos considerar eliminar este cliente si no está disponible
            }
        }
    }

    @Override
    public void registerUser(String username) throws RemoteException {
        System.out.println("Usuario registrado: " + username);
    }

    @Override
    public void receiveMessage(String username, String message) throws RemoteException {
        System.out.println("Mensaje recibido en el servidor de: " + username + " - " + message);
    }

    @Override
    public void registerClient(String username, ChatRemote client) throws RemoteException {
        clients.put(username, client);
        String clientIP = "Desconocida";

        try {
            // Obtener la IP real del cliente que se conecta
            clientIP = RemoteServer.getClientHost();
            log("Cliente conectado desde IP: " + clientIP);
        } catch (ServerNotActiveException e) {
            log("No se pudo obtener la IP del cliente remoto: " + e.getMessage());
            try {
                clientIP = InetAddress.getLocalHost().getHostAddress();
                log("Usando IP local como alternativa: " + clientIP);
            } catch (UnknownHostException ex) {
                log("No se pudo obtener la dirección IP local: " + ex.getMessage());
            }
        }

        LocalDateTime connectTime = LocalDateTime.now();
        ClientInfo info = new ClientInfo(username, clientIP, connectTime);
        clientInfo.put(username, info);
        clientHistory.add(info);

        // Guardar la información del cliente en el archivo
        saveClientInfo(info);

        System.out.println("Cliente registrado: " + username + " desde IP: " + clientIP + " en " + connectTime);
    }

    public void unregisterClient(String username) throws RemoteException {
        clients.remove(username);
        ClientInfo info = clientInfo.get(username);
        if (info != null) {
            info.setDisconnectTime(LocalDateTime.now());
            // Actualizar el archivo con la hora de desconexión
            updateClientDisconnectTime(info);
            System.out.println("Cliente desregistrado: " + username + " a las " + info.getDisconnectTime());
        }
    }

    public Map<String, ClientInfo> getClientInfo() {
        return clientInfo;
    }

    public List<ClientInfo> getClientHistory() {
        return clientHistory;
    }

    private void log(String message) {
        System.out.println(message);
    }

    // Método para guardar la información del cliente en un archivo de texto
    private void saveClientInfo(ClientInfo info) {
        try (FileWriter fw = new FileWriter(LOG_FILE_PATH, true);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw)) {

            out.println(info.getUsername() + "," +
                    info.getIpAddress() + "," +
                    info.getConnectTime().format(formatter) + "," +
                    (info.getDisconnectTime() != null ? info.getDisconnectTime().format(formatter) : ""));

        } catch (IOException e) {
            log("Error al guardar información del cliente: " + e.getMessage());
        }
    }

    // Método para actualizar la hora de desconexión en el archivo
    private void updateClientDisconnectTime(ClientInfo info) {
        try {
            // Leer el archivo completo
            List<String> lines = new ArrayList<>();
            try (BufferedReader br = new BufferedReader(new FileReader(LOG_FILE_PATH))) {
                String line;
                while ((line = br.readLine()) != null) {
                    lines.add(line);
                }
            }

            // Actualizar la línea correspondiente al cliente
            try (FileWriter fw = new FileWriter(LOG_FILE_PATH);
                 BufferedWriter bw = new BufferedWriter(fw);
                 PrintWriter out = new PrintWriter(bw)) {

                for (String line : lines) {
                    String[] parts = line.split(",");
                    if (parts.length >= 3 && parts[0].equals(info.getUsername()) &&
                            parts[1].equals(info.getIpAddress()) &&
                            parts[2].equals(info.getConnectTime().format(formatter))) {

                        // Actualizar esta línea con la hora de desconexión
                        out.println(info.getUsername() + "," +
                                info.getIpAddress() + "," +
                                info.getConnectTime().format(formatter) + "," +
                                info.getDisconnectTime().format(formatter));
                    } else {
                        // Mantener la línea sin cambios
                        out.println(line);
                    }
                }
            }

        } catch (IOException e) {
            log("Error al actualizar hora de desconexión: " + e.getMessage());
        }
    }

    // Método para cargar el historial previo desde el archivo
    private void loadClientHistory() {
        clientHistory.clear();
        File file = new File(LOG_FILE_PATH);

        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                log("Error al crear archivo de historial: " + e.getMessage());
            }
            return;
        }

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 3) {
                    String username = parts[0];
                    String ipAddress = parts[1];
                    LocalDateTime connectTime = LocalDateTime.parse(parts[2], formatter);
                    LocalDateTime disconnectTime = null;

                    if (parts.length >= 4 && !parts[3].isEmpty()) {
                        disconnectTime = LocalDateTime.parse(parts[3], formatter);
                    }

                    ClientInfo info = new ClientInfo(username, ipAddress, connectTime);
                    if (disconnectTime != null) {
                        info.setDisconnectTime(disconnectTime);
                    }

                    clientHistory.add(info);
                }
            }
        } catch (IOException e) {
            log("Error al cargar historial de clientes: " + e.getMessage());
        }
    }

    public static class ClientInfo {
        private String username;
        private String ipAddress;
        private LocalDateTime connectTime;
        private LocalDateTime disconnectTime;

        public ClientInfo(String username, String ipAddress, LocalDateTime connectTime) {
            this.username = username;
            this.ipAddress = ipAddress;
            this.connectTime = connectTime;
        }

        public String getUsername() {
            return username;
        }

        public String getIpAddress() {
            return ipAddress;
        }

        public LocalDateTime getConnectTime() {
            return connectTime;
        }

        public LocalDateTime getDisconnectTime() {
            return disconnectTime;
        }

        public void setDisconnectTime(LocalDateTime disconnectTime) {
            this.disconnectTime = disconnectTime;
        }
    }

    public static void main(String[] args) {
        try {
            // Establecer la propiedad de hostname para RMI
            System.setProperty("java.rmi.server.hostname", "192.168.100.89");

            // Crear el registro RMI en el puerto 1099
            LocateRegistry.createRegistry(1099);

            // Crear una instancia del servidor de chat
            ChatRemote chatServer = new ChatServer();

            // Registrar el servidor de chat en el registro RMI
            Naming.rebind("rmi://192.168.100.89:1099/ChatServer", chatServer);

            System.out.println("Servidor de chat iniciado en el puerto 1099.");
        } catch (RemoteException | MalformedURLException ex) {
            System.err.println("Error al iniciar el servidor: " + ex.getMessage());
        }
    }
}
