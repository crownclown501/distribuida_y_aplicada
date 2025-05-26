import javax.swing.*;
import java.rmi.AlreadyBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class ServerMain {

    public static final String UNIQUE_BINDING_NAME = "server.calculator";
    public static final int REGISTRY_PORT = 1099;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ServerGUI frame = new ServerGUI();
            frame.setVisible(true);
            startServer(frame);
        });
    }

    private static void startServer(ServerGUI frame) {
        try {
            final RemoteCalculationServer server = new RemoteCalculationServer(frame);
            final Registry registry = LocateRegistry.createRegistry(REGISTRY_PORT);
            Remote stub = UnicastRemoteObject.exportObject(server, 0);
            registry.bind(UNIQUE_BINDING_NAME, stub);
            System.out.println("Server is ready.");
        } catch (RemoteException | AlreadyBoundException e) {
            System.err.println("Server exception: " + e.toString());
            e.printStackTrace();
        }
    }
}
