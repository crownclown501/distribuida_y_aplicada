import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

public class ServerGUI extends JFrame {

    private List<String> connectedClients;
    private DefaultListModel<String> listModel;
    private JList<String> clientList;
    private JTextArea operationsArea;

    public ServerGUI() {
        connectedClients = new ArrayList<>();
        listModel = new DefaultListModel<>();
        clientList = new JList<>(listModel);
        operationsArea = new JTextArea(10, 30);

        setTitle("Server");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.add(new JLabel("Connected Clients:"), BorderLayout.NORTH);
        leftPanel.add(new JScrollPane(clientList), BorderLayout.CENTER);

        add(leftPanel, BorderLayout.WEST);

        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.add(new JLabel("Client Operations:"), BorderLayout.NORTH);
        rightPanel.add(new JScrollPane(operationsArea), BorderLayout.CENTER);

        add(rightPanel, BorderLayout.CENTER);

        JButton clearButton = new JButton("Clear");
        clearButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                listModel.clear();
                operationsArea.setText("");
            }
        });

        add(clearButton, BorderLayout.SOUTH);
    }

    public void addClient(String clientName) {
        connectedClients.add(clientName);
        listModel.addElement(clientName);
    }

    public void logOperation(String operation) {
        operationsArea.append(operation + "\n");
    }
}
