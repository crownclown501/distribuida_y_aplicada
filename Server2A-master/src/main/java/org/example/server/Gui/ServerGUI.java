package org.example.server.Gui;

import org.example.server.RMIChat.ChatServer;

import javax.swing.*;
import java.awt.*;
import java.net.MalformedURLException;
import java.rmi.AlreadyBoundException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Map;

public class ServerGUI {
    private ChatServer chatServer;
    private JFrame frame;
    private JButton startButton;
    private JButton stopButton;
    private JTextArea logArea;
    private JTextArea clientListArea;
    private boolean serverRunning = false;

    public ServerGUI() {
        frame = new JFrame("Chat Server");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 400);
        frame.setLayout(new BorderLayout());

        startButton = new JButton("Start Server");
        stopButton = new JButton("Stop Server");
        stopButton.setEnabled(false);
        logArea = new JTextArea();
        logArea.setEditable(false);
        clientListArea = new JTextArea();
        clientListArea.setEditable(false);

        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(startButton);
        buttonPanel.add(stopButton);

        frame.add(buttonPanel, BorderLayout.NORTH);
        frame.add(new JScrollPane(logArea), BorderLayout.CENTER);
        frame.add(new JScrollPane(clientListArea), BorderLayout.SOUTH);

        startButton.addActionListener(e -> startServer());
        stopButton.addActionListener(e -> stopServer());

        frame.setVisible(true);
    }

    private void startServer() {
        if (serverRunning) {
            log("El servidor ya está en ejecución.");
            return;
        }

        try {
            Registry registry = LocateRegistry.createRegistry(1099);
            chatServer = new ChatServer();
            try {
                // Usar la dirección IP del servidor
                Naming.rebind("rmi://192.168.100.89:1099/ChatServer", chatServer);
                log("Servidor de chat iniciado en el puerto 1099.");
                serverRunning = true;
                startButton.setEnabled(false);
                stopButton.setEnabled(true);
            } catch (MalformedURLException ex) {
                log("El servidor ya está registrado: " + ex.getMessage());
            }
        } catch (RemoteException ex) {
            log("Error al iniciar el servidor: " + ex.getMessage());
        }
    }

    private void stopServer() {
        if (!serverRunning) {
            log("El servidor no está en ejecución.");
            return;
        }

        try {
            Naming.unbind("rmi://192.168.100.89:1099/ChatServer");
            log("Servidor de chat detenido.");
            serverRunning = false;
            startButton.setEnabled(true);
            stopButton.setEnabled(false);
        } catch (Exception ex) {
            log("Error al detener el servidor: " + ex.getMessage());
        }
    }

    private void log(String message) {
        logArea.append(message + "\n");
    }

    private void updateClientList() {
        clientListArea.setText("");
        if (chatServer != null) {
            for (Map.Entry<String, ChatServer.ClientInfo> entry : chatServer.getClientInfo().entrySet()) {
                String clientInfo = "Usuario: " + entry.getKey() +
                        "\nIP: " + entry.getValue().getIpAddress() +
                        "\nHora de conexión: " + entry.getValue().getConnectTime() +
                        "\nHora de desconexión: " + (entry.getValue().getDisconnectTime() != null ? entry.getValue().getDisconnectTime() : "Aún conectado") +
                        "\n";
                clientListArea.append(clientInfo + "\n");
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ServerGUI gui = new ServerGUI();
            // Actualizar la lista de clientes periódicamente
            Timer timer = new Timer(5000, e -> gui.updateClientList());
            timer.start();
        });
    }
}
