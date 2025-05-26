package org.example.main;

import org.example.client.GUI.ClientGUI;

import javax.swing.*;
import java.rmi.RemoteException;

public class ClientMain {
    public static void main(String[] args) {
        // Iniciar la interfaz gráfica del cliente
        SwingUtilities.invokeLater(() -> {
            try {
                new ClientGUI();
            } catch (RemoteException e) {
                // Manejar la excepción si ocurre al iniciar la interfaz gráfica
                JOptionPane.showMessageDialog(null, "Error al iniciar la interfaz gráfica: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        });
    }
}
