package org.example.client.GUI;

import org.example.client.RMIChat.ChatClient;
import org.example.server.RMIChat.ChatRemote;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class ClientGUI extends UnicastRemoteObject implements ChatRemote {
    private ChatClient chatClient;
    private JFrame frame;
    private JTextField usernameField;
    private JTextField messageField;
    private JTextArea chatArea;
    private JButton connectButton;
    private JButton sendButton;
    private JTextField serverIPField;

    public ClientGUI() throws RemoteException {
        super();

        // Inicializar la ventana principal
        frame = new JFrame("Chat Client");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 350);
        frame.setLayout(new BorderLayout());

        // Inicializar componentes
        usernameField = new JTextField(15);
        serverIPField = new JTextField("192.168.100.89", 15); // IP del servidor por defecto
        messageField = new JTextField(20);
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        connectButton = new JButton("Connect");
        sendButton = new JButton("Send");
        sendButton.setEnabled(false);

        // Añadir componentes al marco
        JPanel topPanel = new JPanel(new FlowLayout());
        topPanel.add(new JLabel("Username:"));
        topPanel.add(usernameField);
        topPanel.add(new JLabel("Server IP:"));
        topPanel.add(serverIPField);
        topPanel.add(connectButton);

        JPanel bottomPanel = new JPanel(new FlowLayout());
        bottomPanel.add(messageField);
        bottomPanel.add(sendButton);

        frame.add(topPanel, BorderLayout.NORTH);
        frame.add(new JScrollPane(chatArea), BorderLayout.CENTER);
        frame.add(bottomPanel, BorderLayout.SOUTH);

        // Añadir listeners a los botones
        connectButton.addActionListener(new ConnectButtonListener());
        sendButton.addActionListener(new SendButtonListener());

        // Mostrar la ventana
        frame.setVisible(true);
    }

    private class ConnectButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            String username = usernameField.getText().trim();
            String serverIP = serverIPField.getText().trim();
            if (username.isEmpty() || serverIP.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "Please enter a username and server IP.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                chatClient = new ChatClient(username, ClientGUI.this, serverIP);
                chatArea.append("Connected as " + username + "\n");
                usernameField.setEditable(false);
                serverIPField.setEditable(false);
                connectButton.setEnabled(false);
                sendButton.setEnabled(true);
                messageField.requestFocus();
            } catch (RemoteException ex) {
                JOptionPane.showMessageDialog(frame, "Error connecting to the server: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private class SendButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            String message = messageField.getText().trim();
            if (message.isEmpty()) {
                return;
            }

            try {
                chatClient.sendMessage(chatClient.getUsername(), message);
                messageField.setText(""); // Limpiar el campo de texto
            } catch (RemoteException ex) {
                JOptionPane.showMessageDialog(frame, "Error sending message: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    @Override
    public void receiveMessage(String sender, String message) throws RemoteException {
        chatArea.append(sender + ": " + message + "\n");
    }

    @Override
    public void registerUser(String username) throws RemoteException {
        // No es necesario implementar esto en el cliente
    }

    @Override
    public void registerClient(String username, ChatRemote client) throws RemoteException {
        // No es necesario implementar esto en el cliente
    }

    @Override
    public void sendMessage(String username, String message) throws RemoteException {
        chatClient.sendMessage(username, message);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                new ClientGUI();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        });
    }
}
