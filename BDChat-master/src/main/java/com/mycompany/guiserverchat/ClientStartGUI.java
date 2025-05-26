
package com.mycompany.guiserverchat;

import java.awt.FlowLayout;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.Socket;

public class ClientStartGUI extends JFrame {
    private JTextField serverAddressField;

    public ClientStartGUI() {
        setTitle("Connect to Server");
        setSize(300, 150);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new FlowLayout());

        serverAddressField = new JTextField(20);
        JButton connectButton = new JButton("Connect");

        connectButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String serverAddress = serverAddressField.getText();
                try {
                    // Intentar conectarse al servidor
                    Socket socket = new Socket(serverAddress, 12345);
                    System.out.println("Connected to server at " + serverAddress);

                    // Si la conexión es exitosa, abrir la ventana de login/signup
                    new LoginSignupGUI(socket).setVisible(true);
                    dispose(); // Cerrar la ventana actual
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(ClientStartGUI.this,
                            "Unable to connect to the server. Please check the address and try again.",
                            "Connection Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        add(new JLabel("Server Address:"));
        add(serverAddressField);
        add(connectButton);

        setVisible(true);
    }
}