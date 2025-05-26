package com.taskmanager.dts.service.impl;

import com.taskmanager.dts.dto.NodeCreateDTO;
import com.taskmanager.dts.dto.NodeDTO;
import com.taskmanager.dts.entity.Node;
import com.taskmanager.dts.entity.NodeStatus;
import com.taskmanager.dts.exception.ResourceNotFoundException;
import com.taskmanager.dts.repository.NodeRepository;
import com.taskmanager.dts.service.NodeService;
import com.taskmanager.dts.service.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class NodeServiceImpl implements NodeService {

    @Autowired
    private NodeRepository nodeRepository;

    @Autowired
    private TaskService taskService;

    @Value("${task.heartbeat.interval:30000}")
    private long heartbeatInterval;

    @Override
    @Transactional
    public NodeDTO createNode(NodeCreateDTO nodeCreateDTO) {
        Node node = new Node();
        node.setName(nodeCreateDTO.getName());
        node.setHost(nodeCreateDTO.getHost());
        node.setPort(nodeCreateDTO.getPort());
        node.setActiveTasksCount(0);
        node.setStatus(NodeStatus.AVAILABLE);
        node.setLastHeartbeat(LocalDateTime.now());

        Node savedNode = nodeRepository.save(node);
        return convertToDTO(savedNode);
    }

    @Override
    public NodeDTO getNodeById(Long id) {
        Node node = nodeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Nodo no encontrado con ID: " + id));
        return convertToDTO(node);
    }

    @Override
    public List<NodeDTO> getAllNodes() {
        return nodeRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteNode(Long id) {
        Node node = nodeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Nodo no encontrado con ID: " + id));
        nodeRepository.delete(node);
    }

    @Override
    @Transactional
    public NodeDTO updateNodeStatus(Long id, String status) {
        Node node = nodeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Nodo no encontrado con ID: " + id));

        NodeStatus newStatus = NodeStatus.valueOf(status.toUpperCase());
        node.setStatus(newStatus);

        if (newStatus == NodeStatus.INACTIVE) {
            // Si un nodo se marca como inactivo, debemos reasignar sus tareas
            try {
                taskService.reassignTasksFromInactiveNodes();
            } catch (IllegalStateException e) {
                // Manejar caso donde no hay nodos disponibles para reasignar
                System.out.println("No se pudieron reasignar tareas: " + e.getMessage());
            }
        }

        Node updatedNode = nodeRepository.save(node);
        return convertToDTO(updatedNode);
    }

    @Override
    public List<NodeDTO> getAllNodesStatus() {
        return getAllNodes();
    }

    @Override
    @Transactional
    public void updateNodeHeartbeat(Long id) {
        Node node = nodeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Nodo no encontrado con ID: " + id));

        node.setLastHeartbeat(LocalDateTime.now());

        // Si el nodo estaba inactivo, marcarlo como disponible
        if (node.getStatus() == NodeStatus.INACTIVE) {
            node.setStatus(NodeStatus.AVAILABLE);
        }

        nodeRepository.save(node);
    }

    @Override
    @Transactional
    public void checkInactiveNodes() {
        LocalDateTime threshold = LocalDateTime.now().minusSeconds(heartbeatInterval / 1000);
        List<Node> potentiallyInactiveNodes = nodeRepository.findByLastHeartbeatBefore(threshold);

        for (Node node : potentiallyInactiveNodes) {
            if (node.getStatus() != NodeStatus.INACTIVE) {
                node.setStatus(NodeStatus.INACTIVE);
                nodeRepository.save(node);
            }
        }

        if (!potentiallyInactiveNodes.isEmpty()) {
            try {
                taskService.reassignTasksFromInactiveNodes();
            } catch (IllegalStateException e) {
                // Manejar caso donde no hay nodos disponibles para reasignar
                System.out.println("No se pudieron reasignar tareas: " + e.getMessage());
            }
        }
    }

    private NodeDTO convertToDTO(Node node) {
        NodeDTO dto = new NodeDTO();
        dto.setId(node.getId());
        dto.setName(node.getName());
        dto.setHost(node.getHost());
        dto.setPort(node.getPort());
        dto.setStatus(node.getStatus().name());
        dto.setLastHeartbeat(node.getLastHeartbeat());
        dto.setActiveTasksCount(node.getActiveTasksCount());
        return dto;
    }
}