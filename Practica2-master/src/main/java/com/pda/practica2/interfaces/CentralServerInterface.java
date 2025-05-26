package com.pda.practica2.interfaces;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface CentralServerInterface extends Remote {
    void registerNode(String nodeID, List<String> resources) throws RemoteException;
    List<String> searchResources(String query) throws RemoteException;
}
