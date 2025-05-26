package org.example.server.RMIChat;

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
import java.util.HashMap;
import java.util.Map;

public class ChatServer extends UnicastRemoteObject implements ChatRemote {
    private Map<String, ChatRemote> clients = new HashMap<>();
    private Map<String, ClientInfo> clientInfo = new HashMap<>();

    public ChatServer() throws RemoteException {
        super(0); // Permitir conexiones desde cualquier host
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
        clientInfo.put(username, new ClientInfo(clientIP, connectTime));
        System.out.println("Cliente registrado: " + username + " desde IP: " + clientIP + " en " + connectTime);
    }

    public void unregisterClient(String username) throws RemoteException {
        clients.remove(username);
        ClientInfo info = clientInfo.get(username);
        if (info != null) {
            info.setDisconnectTime(LocalDateTime.now());
            System.out.println("Cliente desregistrado: " + username + " a las " + info.getDisconnectTime());
        }
    }

    public Map<String, ClientInfo> getClientInfo() {
        return clientInfo;
    }

    private void log(String message) {
        System.out.println(message);
    }

    public static class ClientInfo {
        private String ipAddress;
        private LocalDateTime connectTime;
        private LocalDateTime disconnectTime;

        public ClientInfo(String ipAddress, LocalDateTime connectTime) {
            this.ipAddress = ipAddress;
            this.connectTime = connectTime;
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