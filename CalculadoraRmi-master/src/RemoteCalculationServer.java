import java.rmi.RemoteException;

public class RemoteCalculationServer implements Calculator {

    private ServerGUI serverFrame;

    public RemoteCalculationServer(ServerGUI serverFrame) {
        this.serverFrame = serverFrame;
    }

    @Override
    public CalculationObject multiply(CalculationObject obj) throws RemoteException {
        serverFrame.logOperation("Multiplying " + obj.getX() + " and " + obj.getY());
        return new CalculationObject(obj.getX() * obj.getY(), 0);
    }

    @Override
    public CalculationObject add(CalculationObject obj) throws RemoteException {
        serverFrame.logOperation("Adding " + obj.getX() + " and " + obj.getY());
        return new CalculationObject(obj.getX() + obj.getY(), 0);
    }

    @Override
    public CalculationObject subtract(CalculationObject obj) throws RemoteException {
        serverFrame.logOperation("Subtracting " + obj.getY() + " from " + obj.getX());
        return new CalculationObject(obj.getX() - obj.getY(), 0);
    }

    @Override
    public CalculationObject divide(CalculationObject obj) throws RemoteException {
        serverFrame.logOperation("Dividing " + obj.getX() + " by " + obj.getY());
        if (obj.getY() == 0) {
            throw new RemoteException("Division by zero is not allowed.");
        }
        return new CalculationObject(obj.getX() / obj.getY(), 0);
    }

    @Override
    public String getOperations() throws RemoteException {
        return "Operations: add, subtract, multiply, divide";
    }

    @Override
    public void registerClient(String clientName) throws RemoteException {
        serverFrame.addClient(clientName);
    }
}
