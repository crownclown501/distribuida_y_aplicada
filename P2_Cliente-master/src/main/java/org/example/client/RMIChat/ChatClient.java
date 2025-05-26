package org.example.client.RMIChat;

import org.example.server.RMIChat.ChatRemote;

import java.net.InetAddress;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class ChatClient extends UnicastRemoteObject implements ChatRemote {
    private ChatRemote chatServer;
    private String username;
    private ChatRemote clientCallback;

    public ChatClient(String username, ChatRemote clientCallback, String serverIP) throws RemoteException {
        super();
        try {
            // IMPORTANTE: Establecer la propiedad antes de llamar a super()
            System.setProperty("java.rmi.server.hostname", InetAddress.getLocalHost().getHostAddress());

            // Luego llamar a super() para que UnicastRemoteObject use la propiedad correcta


            this.username = username;
            this.clientCallback = clientCallback;
            
            // Buscar y conectar al servidor RMI utilizando la IP del servidor
            chatServer = (ChatRemote) Naming.lookup("rmi://" + serverIP + ":1099/ChatServer");
            chatServer.registerUser(username);
            chatServer.registerClient(username, this);
            System.out.println("Cliente conectado al servidor en " + serverIP);
        } catch (Exception e) {
            System.err.println("Error al conectar al servidor: " + e.getMessage());
            throw new RemoteException("Error de conexión", e);
        }
    }

    @Override
    public void sendMessage(String username, String message) throws RemoteException {
        chatServer.sendMessage(username, message);
    }

    @Override
    public void registerUser(String username) throws RemoteException {
        // No es necesario implementar esto en el cliente
    }

    @Override
    public void receiveMessage(String sender, String message) throws RemoteException {
        // Redirigir el mensaje recibido al callback del cliente
        clientCallback.receiveMessage(sender, message);
    }

    @Override
    public void registerClient(String username, ChatRemote client) throws RemoteException {
        // No es necesario implementar esto en el cliente
    }

    public String getUsername() {
        return username;
    }
}