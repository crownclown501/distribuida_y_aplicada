package com.mycompany.guiserverchat;

import javax.swing.*;
import java.awt.*;
import java.util.Set;

public class ServerGUI extends JFrame {
    private JTextArea connectedClientsArea;
    private JTextArea disconnectedClientsArea;

    public ServerGUI() {
        setTitle("Chat Server");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new GridLayout(1, 2));

        connectedClientsArea = new JTextArea();
        connectedClientsArea.setEditable(false);
        JScrollPane connectedScrollPane = new JScrollPane(connectedClientsArea);
        add(new JLabel("Connected Clients"));
        add(connectedScrollPane);

        disconnectedClientsArea = new JTextArea();
        disconnectedClientsArea.setEditable(false);
        JScrollPane disconnectedScrollPane = new JScrollPane(disconnectedClientsArea);
        add(new JLabel("Disconnected Clients"));
        add(disconnectedScrollPane);

        setVisible(true);
    }

    public void updateConnectedClients(Set<String> clients) {
        connectedClientsArea.setText("");
        for (String client : clients) {
            connectedClientsArea.append(client + "\n");
        }
    }

    public void addDisconnectedClient(String clientInfo) {
        disconnectedClientsArea.append(clientInfo + "\n");
    }
}