import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class ClientGUI extends JFrame {

    private JTextField numberField1;
    private JTextField numberField2;
    private JComboBox<String> operationComboBox;
    private JTextField serverIPField;
    private JTextArea resultArea;
    private Calculator calculator;

    public ClientGUI() {
        setTitle("Client");
        setSize(450, 350);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new FlowLayout());

        numberField1 = new JTextField(10);
        numberField2 = new JTextField(10);
        operationComboBox = new JComboBox<>(new String[]{"add", "subtract", "multiply", "divide"});
        serverIPField = new JTextField(15);
        resultArea = new JTextArea(10, 30);

        JButton calculateButton = new JButton("Calculate");
        calculateButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                performCalculation();
            }
        });

        JButton connectButton = new JButton("Connect");
        connectButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                connectToServer();
            }
        });

        add(new JLabel("Server IP:"));
        add(serverIPField);
        add(connectButton);
        add(new JLabel("Number 1:"));
        add(numberField1);
        add(new JLabel("Number 2:"));
        add(numberField2);
        add(new JLabel("Operation:"));
        add(operationComboBox);
        add(calculateButton);
        add(new JScrollPane(resultArea));
    }

    private void performCalculation() {
        try {
            int num1 = Integer.parseInt(numberField1.getText());
            int num2 = Integer.parseInt(numberField2.getText());
            CalculationObject obj = new CalculationObject(num1, num2);
            String operation = (String) operationComboBox.getSelectedItem();
            CalculationObject resultObj = null;

            switch (operation) {
                case "add":
                    resultObj = calculator.add(obj);
                    break;
                case "subtract":
                    resultObj = calculator.subtract(obj);
                    break;
                case "multiply":
                    resultObj = calculator.multiply(obj);
                    break;
                case "divide":
                    resultObj = calculator.divide(obj);
                    break;
                default:
                    resultArea.append("Invalid operation.\n");
                    return;
            }

            resultArea.append("Result: " + resultObj.getX() + "\n");
        } catch (NumberFormatException ex) {
            resultArea.append("Please enter valid integers.\n");
        } catch (RemoteException ex) {
            resultArea.append("Error performing calculation.\n");
        }
    }

    private void connectToServer() {
        String serverIP = serverIPField.getText();
        int registryPort = 1099;

        try {
            final Registry registry = LocateRegistry.getRegistry(serverIP, registryPort);
            calculator = (Calculator) registry.lookup(ClientMain.UNIQUE_BINDING_NAME);
            calculator.registerClient("Client");
            resultArea.append("Connected to server.\n");
            resultArea.append(calculator.getOperations() + "\n");
        } catch (RemoteException | NotBoundException e) {
            resultArea.append("Failed to connect to server: " + e.getMessage() + "\n");
        }
    }
}
