package org.example.main;

import org.example.server.Gui.ServerGUI;
import org.example.server.RMIChat.ChatRemote;
import org.example.server.RMIChat.ChatServer;

import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;

public class ServerMain {
    public static void main(String[] args) {
        try {
            // Crear el registro RMI en el puerto 1099
            LocateRegistry.createRegistry(1099);

            // Crear una instancia del servidor de chat
            ChatServer server = new ChatServer();

            // Registrar el servidor de chat en el registro RMI usando la dirección IP del servidor
            Naming.rebind("rmi://192.168.100.89:1099/ChatServer", server);
            System.out.println("Servidor de chat iniciado.");

            // Iniciar la interfaz gráfica
            new ServerGUI();
        } catch (Exception e) {
            System.err.println("Error al iniciar el servidor: " + e.getMessage());
        }
    }
}
