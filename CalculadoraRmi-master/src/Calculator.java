import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Calculator extends Remote {
   CalculationObject multiply(CalculationObject obj) throws RemoteException;
   CalculationObject add(CalculationObject obj) throws RemoteException;
   CalculationObject subtract(CalculationObject obj) throws RemoteException;
   CalculationObject divide(CalculationObject obj) throws RemoteException;
   String getOperations() throws RemoteException;
   void registerClient(String clientName) throws RemoteException;
}
