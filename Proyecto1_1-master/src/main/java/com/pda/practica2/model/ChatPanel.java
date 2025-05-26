package com.pda.practica2.model;

import com.pda.practica2.interfaces.PeerInterface;
import javax.swing.*;
import java.awt.*;

public class ChatPanel extends JPanel {
    private JTextArea chatArea;
    private JTextField messageField;
    private JButton sendButton;
    private PeerInterface peer; // Referencia a la interfaz del peer

    public ChatPanel(PeerInterface peer) { // Aceptar la referencia de PeerInterface en el constructor
        this.peer = peer;
        setLayout(new BorderLayout());

        // Área para mostrar los mensajes
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(chatArea);
        add(scrollPane, BorderLayout.CENTER);

        // Panel para el campo de texto y el botón de enviar
        JPanel inputPanel = new JPanel(new BorderLayout());
        messageField = new JTextField();
        sendButton = new JButton("Enviar");

        inputPanel.add(messageField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);
        add(inputPanel, BorderLayout.SOUTH);

        // ActionListener para enviar mensajes
        sendButton.addActionListener(e -> {
            String message = messageField.getText();
            if (!message.trim().isEmpty()) {
                try {
                    peer.sendChatMessage(peer.getName(), message);
                    chatArea.append("Tú: " + message + "\n");
                    messageField.setText("");
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
    }

    public JTextArea getChatArea() {
        return chatArea;
    }

    public JTextField getMessageField() {
        return messageField;
    }

    public JButton getSendButton() {
        return sendButton;
    }
}
