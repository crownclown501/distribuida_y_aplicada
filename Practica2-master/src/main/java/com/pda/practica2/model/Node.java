package com.pda.practica2.model;

import java.util.List;

public class Node {
    private String nodeID;
    private List<String> resources;

    public Node(String nodeID, List<String> resources) {
        this.nodeID = nodeID;
        this.resources = resources;
    }

    public String getNodeID() {
        return nodeID;
    }

    public List<String> getResources() {
        return resources;
    }

    public void setResources(List<String> resources) {
        this.resources = resources;
    }
}
