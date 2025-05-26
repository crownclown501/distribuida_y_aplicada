package com.mycompany.guiserverchat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

public class ChatServer {
    private static final int PORT = 12345;
    private static Set<ClientHandler> clientHandlers = new HashSet<>();
    private static ServerGUI serverGUI;

    public static void main(String[] args) {
        serverGUI = new ServerGUI();

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server is listening on port " + PORT);

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("New client connected");

                ClientHandler clientHandler = new ClientHandler(socket);
                clientHandlers.add(clientHandler);
                new Thread(clientHandler).start();

                updateConnectedClients();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public static void updateConnectedClients() {
        Set<String> clientInfo = new HashSet<>();
        for (ClientHandler clientHandler : clientHandlers) {
            clientInfo.add(clientHandler.getClientName() + " - " + clientHandler.getSocket().getInetAddress().getHostAddress());
        }
        serverGUI.updateConnectedClients(clientInfo);
    }

    public static void addDisconnectedClient(String clientInfo) {
        serverGUI.addDisconnectedClient(clientInfo);
    }

    public static void broadcastMessage(String message) {
        for (ClientHandler clientHandler : clientHandlers) {
            clientHandler.sendMessage(message);
        }
    }

    static class ClientHandler implements Runnable {
        private Socket socket;
        private BufferedReader in;
        private PrintWriter out;
        private String clientName;
        private int userId;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                String input;
                while ((input = in.readLine()) != null) {
                    if (input.startsWith("REGISTER:")) {
                        String[] credentials = input.substring(9).split(":");
                        String newUsername = credentials[0];
                        String newPassword = credentials[1];
                        if (registerUser(newUsername, newPassword)) {
                            out.println("REGISTERED");
                        } else {
                            out.println("FAILED");
                        }
                    } else if (input.startsWith("LOGIN:")) {
                        String[] credentials = input.substring(6).split(":");
                        clientName = credentials[0];
                        String password = credentials[1];
                        userId = authenticateUser(clientName, password, socket.getInetAddress().getHostAddress());
                        if (userId != -1) {
                            ChatServer.broadcastMessage(clientName + " has joined the chat.");
                            break;
                        } else {
                            out.println("INVALID");
                        }
                    }
                }

                String message;
                while ((message = in.readLine()) != null) {
                    ChatServer.broadcastMessage(clientName + ": " + message);
                    saveMessageToDatabase(userId, message);
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            } finally {
                closeConnections();
                clientHandlers.remove(this);
                String clientInfo = clientName + " - " + socket.getInetAddress().getHostAddress() + " - Disconnected at " + new java.util.Date();
                ChatServer.addDisconnectedClient(clientInfo);
                ChatServer.updateConnectedClients();
            }
        }

        private boolean registerUser(String username, String password) {
            try (Connection conn = DatabaseUtil.getConnection()) {
                String sql = "INSERT INTO users (username, password) VALUES (?, ?)";
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, username);
                    pstmt.setString(2, password);
                    pstmt.executeUpdate();
                    return true;
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            return false;
        }

        private int authenticateUser(String username, String password, String ipAddress) {
            try (Connection conn = DatabaseUtil.getConnection()) {
                String sql = "SELECT id FROM users WHERE username = ? AND password = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, username);
                    pstmt.setString(2, password);
                    ResultSet rs = pstmt.executeQuery();
                    if (rs.next()) {
                        int userId = rs.getInt("id");
                        updateUserLogin(userId, ipAddress);
                        return userId;
                    }
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            return -1;
        }

        private void updateUserLogin(int userId, String ipAddress) {
            try (Connection conn = DatabaseUtil.getConnection()) {
                String sql = "INSERT INTO user_ips (user_id, ip_address) VALUES (?, ?)";
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setInt(1, userId);
                    pstmt.setString(2, ipAddress);
                    pstmt.executeUpdate();
                }

                sql = "UPDATE users SET last_login = CURRENT_TIMESTAMP WHERE id = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setInt(1, userId);
                    pstmt.executeUpdate();
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }

        private void saveMessageToDatabase(int userId, String message) {
            try (Connection conn = DatabaseUtil.getConnection()) {
                String sql = "INSERT INTO messages (user_id, message) VALUES (?, ?)";
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setInt(1, userId);
                    pstmt.setString(2, message);
                    pstmt.executeUpdate();
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }

        private void closeConnections() {
            try {
                if (in != null) {
                    in.close();
                }
                if (out != null) {
                    out.close();
                }
                if (socket != null) {
                    socket.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        public void sendMessage(String message) {
            out.println(message);
        }

        public String getClientName() {
            return clientName;
        }

        public Socket getSocket() {
            return socket;
        }
    }
}