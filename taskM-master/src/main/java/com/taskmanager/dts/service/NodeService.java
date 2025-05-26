package com.taskmanager.dts.service;

import com.taskmanager.dts.entity.Node;
import com.taskmanager.dts.repository.NodeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class NodeService {

    @Autowired
    private NodeRepository nodeRepository;

    public Node registerNode(String nodeId) {
        // Verificar si el nodo ya existe
        Node existingNode = nodeRepository.findByNodeId(nodeId);

        if (existingNode != null) {
            // Actualizar nodo existente
            existingNode.setStatus("activo");
            existingNode.setLastHeartbeat(System.currentTimeMillis());
            return nodeRepository.save(existingNode);
        } else {
            // Crear nuevo nodo
            Node newNode = new Node();
            newNode.setNodeId(nodeId);
            newNode.setStatus("activo");
            newNode.setLastHeartbeat(System.currentTimeMillis());
            return nodeRepository.save(newNode);
        }
    }

    public void deactivateNode(String nodeId) {
        Node node = nodeRepository.findByNodeId(nodeId);
        if (node != null) {
            node.setStatus("inactivo");
            nodeRepository.save(node);
        }
    }
}