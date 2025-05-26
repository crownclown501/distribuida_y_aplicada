package com.taskmanager.dts.service;

import com.taskmanager.dts.entity.Node;
import com.taskmanager.dts.entity.Task;
import com.taskmanager.dts.repository.NodeRepository;
import com.taskmanager.dts.repository.TaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class HeartbeatService {
    @Autowired
    private NodeRepository nodeRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Scheduled(fixedRate = 10000) // Cada 5 segundos
    public void checkNodeStatus() {
        // Obtener la lista de nodos activos
        List<Node> activeNodes = nodeRepository.findByStatus("activo");

        // Verificar el último heartbeat de cada nodo
        for (Node node : activeNodes) {
            long currentTime = System.currentTimeMillis();
            long lastHeartbeat = node.getLastHeartbeat();

            // Si el último heartbeat fue hace más de 10 segundos, marcar el nodo como inactivo
            if (currentTime - lastHeartbeat > 20000) {
                node.setStatus("inactivo");
                nodeRepository.save(node);

                // Reasignar las tareas del nodo inactivo
                reassignTasks(node.getNodeId());
            }
        }
    }

    public void reassignTasks(String failedNodeId) {
        // Obtener las tareas asignadas al nodo que falló
        List<Task> tasks = taskRepository.findByResponsibleNode(failedNodeId);

        // Reasignar las tareas pendientes a otros nodos
        for (Task task : tasks) {
            if (task.getStatus().equals("procesando")) {
                task.setStatus("pendiente");
                task.setResponsibleNode(null);
                taskRepository.save(task);
            }
        }
    }

    public void updateHeartbeat(String nodeId) {
        // Actualizar el último heartbeat del nodo
        Node node = nodeRepository.findByNodeId(nodeId);
        if (node != null) {
            node.setLastHeartbeat(System.currentTimeMillis());
            nodeRepository.save(node);
        }
    }
}
