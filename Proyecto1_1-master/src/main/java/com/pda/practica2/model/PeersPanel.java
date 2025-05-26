package com.pda.practica2.model;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.rmi.RemoteException;
import java.util.Arrays;

public class PeersPanel extends JPanel {
    private JTable peersTable;
    private DefaultTableModel tableModel;

public PeersPanel() {
    setLayout(new BorderLayout());

    // Crear el modelo de tabla con las columnas necesarias
    tableModel = new DefaultTableModel() {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false; // Hacer que todas las celdas no sean editables
        }
    };

    // Agregar columnas al modelo
    tableModel.addColumn("Nombre del Peer");
    tableModel.addColumn("Dirección IP");

    // Crear la tabla con el modelo
    peersTable = new JTable(tableModel);
    peersTable.getColumnModel().getColumn(0).setPreferredWidth(200);
    peersTable.getColumnModel().getColumn(1).setPreferredWidth(150);

    // Crear scroll pane para la tabla
    JScrollPane scrollPane = new JScrollPane(peersTable);
    add(scrollPane, BorderLayout.CENTER);
}


public void updatePeers(String[] peers) {
    System.out.println("Actualizando lista de peers en la GUI: " + Arrays.toString(peers));

    SwingUtilities.invokeLater(() -> {
        // Limpiar la tabla
        tableModel.setRowCount(0);

        // Agregar peers a la tabla
        for (String peer : peers) {
            String[] parts = peer.split(":");
            if (parts.length >= 2) {
                String peerName = parts[0];
                String peerIP = parts[1];
                tableModel.addRow(new Object[]{peerName, peerIP});
                System.out.println("Agregado peer a la tabla: " + peerName + ", " + peerIP);
            }
        }

        // Notificar a la tabla que los datos han cambiado
        tableModel.fireTableDataChanged();
    });
}

}
