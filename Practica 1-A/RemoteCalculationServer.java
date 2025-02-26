import java.rmi.RemoteException;

public class RemoteCalculationServer implements Calculator {

   @Override
   public int multiply(int x, int y) throws RemoteException {
       return x * y;
   }

   @Override
   public int add(int x, int y) throws RemoteException {
       return x + y;
   }

   @Override
   public int subtract(int x, int y) throws RemoteException {
       return x - y;
   }

   @Override
   public double divide(int x, int y) throws RemoteException {
       if (y == 0) {
           throw new RemoteException("Division by zero is not allowed.");
       }
       return (double) x / y;
   }
}