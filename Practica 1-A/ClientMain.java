import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Scanner;

public class ClientMain {

    public static final String UNIQUE_BINDING_NAME = "server.calculator";

    public static void main(String[] args) {
        try {
            // Conectar al registro RMI
            final Registry registry = LocateRegistry.getRegistry(2732);
            Calculator calculator = (Calculator) registry.lookup(UNIQUE_BINDING_NAME);

            // Crear un Scanner para la entrada del usuario
            Scanner scanner = new Scanner(System.in);

            while (true) {
                // Mostrar el menú
                System.out.println("\nSeleccione una operación:");
                System.out.println("1. Suma");
                System.out.println("2. Resta");
                System.out.println("3. Multiplicación");
                System.out.println("4. División");
                System.out.println("5. Salir");
                System.out.print("Opción: ");

                int opcion = scanner.nextInt();

                if (opcion == 5) {
                    System.out.println("Saliendo...");
                    break; // Salir del bucle
                }

                // Pedir los números al usuario
                System.out.print("Ingrese el primer número: ");
                int x = scanner.nextInt();
                System.out.print("Ingrese el segundo número: ");
                int y = scanner.nextInt();

                // Realizar la operación seleccionada
                switch (opcion) {
                    case 1:
                        int suma = calculator.add(x, y);
                        System.out.println("Resultado de la suma: " + suma);
                        break;
                    case 2:
                        int resta = calculator.subtract(x, y);
                        System.out.println("Resultado de la resta: " + resta);
                        break;
                    case 3:
                        int multiplicacion = calculator.multiply(x, y);
                        System.out.println("Resultado de la multiplicación: " + multiplicacion);
                        break;
                    case 4:
                        try {
                            double division = calculator.divide(x, y);
                            System.out.println("Resultado de la división: " + division);
                        } catch (RemoteException e) {
                            System.out.println("Error: " + e.getMessage());
                        }
                        break;
                    default:
                        System.out.println("Opción no válida. Intente de nuevo.");
                        break;
                }
            }

            // Cerrar el Scanner
            scanner.close();
        } catch (RemoteException | NotBoundException e) {
            System.err.println("Error en el cliente: " + e.getMessage());
        }
    }
}