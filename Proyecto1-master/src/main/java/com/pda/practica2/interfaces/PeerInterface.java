package com.pda.practica2.interfaces;

import com.pda.practica2.model.RMIPeer.SharedFile;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface PeerInterface extends Remote {

    // Método para enviar mensajes entre peers
    void message(String nodeID, String message) throws RemoteException;

    // Método para actualizar la lista de peers
    void updatePeerList(String[] peers) throws RemoteException;

    // Método para buscar archivos en el catálogo del peer
    String[] searchFiles(String query) throws RemoteException;

    // Método para transferir archivos entre peers
    void transferFile(String fileName, String nodeID) throws RemoteException;

    // Método para registrar un archivo en el catálogo del peer
    void registerCatalog(String catalogItem) throws RemoteException;

    // Método para obtener el catálogo de archivos de un tipo específico
    String[] getCatalogs(String type) throws RemoteException;

    // Método para descargar un archivo de otro peer
    void downloadFile(String fileName, String fromPeer) throws RemoteException;

    // Método para notificar sobre un nuevo archivo
    void notifyNewFile(String fileName, String peerName) throws RemoteException;

    // Método para obtener el tamaño de un archivo
    long getFileSize(String fileName) throws RemoteException;

    // Método para obtener todos los archivos compartidos
    List<SharedFile> getAllSharedFiles() throws RemoteException;

    // Método para iniciar una elección de coordinador
    void startElection(String name, int id) throws RemoteException;

    // Método para enviar un mensaje de OK durante la elección
    void sendOk(String where, String to) throws RemoteException;

    // Método para notificar que un peer ha ganado la elección
    void iWon(String node) throws RemoteException;

    // Método para actualizar el coordinador
    void updateCoor(String coordinator) throws RemoteException;

    // Método para verificar si un peer está activo
    boolean isalive() throws RemoteException;

    // Método para actualizar la lista de peers
    void updatePeers(String peers) throws RemoteException;

    // Método para obtener el nombre del peer
    String getName() throws RemoteException;

    // Método para obtener el coordinador actual
    String getCoordinator() throws RemoteException;

    // Método para obtener el estado de la elección
    boolean getelectionInProgress() throws RemoteException;

    // Método para obtener el ID del peer
    int getId() throws RemoteException;
}