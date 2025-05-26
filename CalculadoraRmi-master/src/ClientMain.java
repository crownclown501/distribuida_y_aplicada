import javax.swing.*;

public class ClientMain {

    public static final String UNIQUE_BINDING_NAME = "server.calculator";

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ClientGUI frame = new ClientGUI();
            frame.setVisible(true);
        });
    }
}
