
package com.mycompany.guiserverchat;
import java.awt.FlowLayout;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class LoginSignupGUI extends JFrame {
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JButton signupButton;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    public LoginSignupGUI(Socket socket) {
        this.socket = socket;
        setTitle("Login / Signup");
        setSize(300, 200);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new FlowLayout());

        usernameField = new JTextField(20);
        passwordField = new JPasswordField(20);
        loginButton = new JButton("Login");
        signupButton = new JButton("Signup");

        try {
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error establishing communication with the server.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            dispose();
        }

        loginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String username = usernameField.getText();
                String password = new String(passwordField.getPassword());

                if (username.isEmpty() || password.isEmpty()) {
                    JOptionPane.showMessageDialog(LoginSignupGUI.this,
                            "Please enter both username and password.",
                            "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                // Enviar credenciales al servidor
                out.println("LOGIN:" + username + ":" + password);

                // Leer respuesta del servidor
                try {
                    String response = in.readLine();
                    if (response.equals("INVALID")) {
                        JOptionPane.showMessageDialog(LoginSignupGUI.this,
                                "Invalid credentials. Please try again.",
                                "Login Failed", JOptionPane.ERROR_MESSAGE);
                    } else {
                        // Si el login es exitoso, abrir la ventana del chat
                        new ChatClientGUI(socket, username).setVisible(true);
                        dispose(); // Cerrar la ventana actual
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });

        signupButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String username = usernameField.getText();
                String password = new String(passwordField.getPassword());

                if (username.isEmpty() || password.isEmpty()) {
                    JOptionPane.showMessageDialog(LoginSignupGUI.this,
                            "Please enter both username and password.",
                            "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                // Enviar credenciales al servidor
                out.println("REGISTER:" + username + ":" + password);

                // Leer respuesta del servidor
                try {
                    String response = in.readLine();
                    if (response.equals("REGISTERED")) {
                        JOptionPane.showMessageDialog(LoginSignupGUI.this,
                                "Registration successful! Please log in.",
                                "Success", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(LoginSignupGUI.this,
                                "Registration failed. Please try again.",
                                "Error", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });

        add(new JLabel("Username:"));
        add(usernameField);
        add(new JLabel("Password:"));
        add(passwordField);
        add(loginButton);
        add(signupButton);

        setVisible(true);
    }
}