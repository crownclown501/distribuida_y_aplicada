package org.example.server.RMIChat;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ChatRemote extends Remote {
    void sendMessage(String username, String message) throws RemoteException;
    void registerUser(String username) throws RemoteException;
    void receiveMessage(String username, String message) throws RemoteException;
    void registerClient(String username, ChatRemote client) throws RemoteException;
}
