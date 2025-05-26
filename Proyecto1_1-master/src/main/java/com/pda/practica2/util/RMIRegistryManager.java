package com.pda.practica2.util;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class RMIRegistryManager {
    private static Registry registry;

    public static Registry getRegistry() throws RemoteException {
        if (registry == null) {
            try {
                // Intentar conectar a un registro existente
                registry = LocateRegistry.getRegistry("localhost", 1099);
            } catch (RemoteException e) {
                // Si no existe, crear un nuevo registro en el puerto estándar
                registry = LocateRegistry.createRegistry(1099);
            }
        }
        return registry;
    }
}
