package com.pda.practica2.interfaces;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Map;

public interface PeerInterface extends Remote {

    // Métodos existentes
    void message(String nodeID, String message) throws RemoteException;
    void updatePeerList(String[] peers) throws RemoteException;
    String[] searchFiles(String query) throws RemoteException;
    void transferFile(String fileName, String nodeID) throws RemoteException;
    void registerCatalog(String catalogItem) throws RemoteException;
    String[] getCatalogs(String type) throws RemoteException;
    void startElection(String name, int id) throws RemoteException;
    void sendOk(String where, String to) throws RemoteException;
    void iWon(String node) throws RemoteException;
    void updateCoor(String coordinator) throws RemoteException;
    boolean isalive() throws RemoteException;
    void updatePeers(String peers) throws RemoteException;
    String getName() throws RemoteException;
    String getCoordinator() throws RemoteException;
    boolean getelectionInProgress() throws RemoteException;
    int getId() throws RemoteException;
    
    // Nuevos métodos propuestos
    
    /**
     * Realiza una búsqueda en la red completa de P2P, no solo localmente
     * @param query Término de búsqueda
     * @return Mapa con nodeID como clave y array de archivos encontrados como valor
     */
    Map<String, String[]> searchNetworkFiles(String query) throws RemoteException;
    
    /**
     * Obtiene información detallada de un archivo
     * @param fileName Nombre del archivo
     * @return Mapa con metadatos del archivo (tamaño, fecha, tipo, etc.)
     */
    Map<String, String> getFileInfo(String fileName) throws RemoteException;
    
    /**
     * Inicia una transferencia de archivo parcial, para resumir descargas
     * @param fileName Nombre del archivo
     * @param nodeID ID del nodo destinatario
     * @param startByte Byte desde donde comenzar la transferencia
     * @param endByte Byte hasta donde transferir
     * @return ID único de la transferencia para seguimiento
     */
    String transferFilePartial(String fileName, String nodeID, long startByte, long endByte) throws RemoteException;
    
    /**
     * Comprueba el estado de una transferencia
     * @param transferId ID de la transferencia
     * @return Porcentaje completado (0-100)
     */
    int getTransferProgress(String transferId) throws RemoteException;
    
    /**
     * Cancela una transferencia en curso
     * @param transferId ID de la transferencia
     * @return true si se canceló correctamente
     */
    boolean cancelTransfer(String transferId) throws RemoteException;
    
    /**
     * Verifica si un archivo está completo y no corrupto
     * @param fileName Nombre del archivo
     * @return true si el archivo está íntegro
     */
    boolean verifyFileIntegrity(String fileName) throws RemoteException;
}