package com.pda.practica2.model;

import com.pda.practica2.interfaces.CentralServerInterface;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;

public class CentralServer extends UnicastRemoteObject implements CentralServerInterface {
    private List<Node> nodes;

    protected CentralServer() throws RemoteException {
        super();
        this.nodes = new ArrayList<>();
    }

    @Override
    public void registerNode(String nodeID, List<String> resources) throws RemoteException {
        Node node = new Node(nodeID, resources);
        nodes.add(node);
    }

    @Override
    public List<String> searchResources(String query) throws RemoteException {
        List<String> results = new ArrayList<>();
        for (Node node : nodes) {
            for (String resource : node.getResources()) {
                if (resource.contains(query)) {
                    results.add(resource);
                }
            }
        }
        return results;
    }
}
