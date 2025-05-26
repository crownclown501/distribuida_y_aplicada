package com.taskmanager.dts.service.impl;

import com.taskmanager.dts.dto.TaskCreateDTO;
import com.taskmanager.dts.dto.TaskDTO;
import com.taskmanager.dts.entity.Node;
import com.taskmanager.dts.entity.NodeStatus;
import com.taskmanager.dts.entity.Task;
import com.taskmanager.dts.entity.TaskHistory;
import com.taskmanager.dts.entity.TaskStatus;
import com.taskmanager.dts.exception.ResourceNotFoundException;
import com.taskmanager.dts.repository.NodeRepository;
import com.taskmanager.dts.repository.TaskHistoryRepository;
import com.taskmanager.dts.repository.TaskRepository;
import com.taskmanager.dts.service.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class TaskServiceImpl implements TaskService {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private NodeRepository nodeRepository;

    @Autowired
    private TaskHistoryRepository taskHistoryRepository;

    @Override
    @Transactional
    public TaskDTO createTask(TaskCreateDTO taskCreateDTO) {
        // Buscar el nodo con menos tareas activas
        List<Node> availableNodes = nodeRepository.findByStatus(NodeStatus.AVAILABLE);
        if (availableNodes.isEmpty()) {
            throw new IllegalStateException("No hay nodos disponibles para asignar la tarea");
        }

        Node selectedNode = availableNodes.stream()
                .min(Comparator.comparing(Node::getActiveTasksCount))
                .orElseThrow(() -> new IllegalStateException("Error al seleccionar nodo"));

        Task task = new Task();
        task.setTitle(taskCreateDTO.getTitle());
        task.setDescription(taskCreateDTO.getDescription());
        task.setAssignedNode(selectedNode);

        Task savedTask = taskRepository.save(task);

        // Incrementar contador de tareas activas en el nodo
        selectedNode.setActiveTasksCount(selectedNode.getActiveTasksCount() + 1);
        if (selectedNode.getActiveTasksCount() >= 10) { // Umbral arbitrario
            selectedNode.setStatus(NodeStatus.BUSY);
        }
        nodeRepository.save(selectedNode);

        // Registrar en historial
        TaskHistory history = new TaskHistory();
        history.setTask(savedTask);
        history.setNode(selectedNode);
        history.setPreviousStatus(null);
        history.setNewStatus(TaskStatus.PENDING);
        history.setComments("Tarea creada y asignada al nodo " + selectedNode.getName());
        taskHistoryRepository.save(history);

        return convertToDTO(savedTask);
    }

    @Override
    public TaskDTO getTaskById(Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tarea no encontrada con ID: " + id));
        return convertToDTO(task);
    }

    @Override
    public List<TaskDTO> getAllTasks() {
        return taskRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteTask(Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tarea no encontrada con ID: " + id));

        // Actualizar contador del nodo si la tarea estaba asignada
        if (task.getAssignedNode() != null) {
            Node node = task.getAssignedNode();
            node.setActiveTasksCount(Math.max(0, node.getActiveTasksCount() - 1));

            // Si el nodo estaba ocupado y ahora tiene menos tareas, marcarlo como disponible
            if (node.getStatus() == NodeStatus.BUSY && node.getActiveTasksCount() < 10) {
                node.setStatus(NodeStatus.AVAILABLE);
            }
            nodeRepository.save(node);
        }

        taskRepository.delete(task);
    }

    @Override
    @Transactional
    public TaskDTO updateTaskStatus(Long id, String status) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tarea no encontrada con ID: " + id));

        TaskStatus previousStatus = task.getStatus();
        TaskStatus newStatus = TaskStatus.valueOf(status.toUpperCase());
        task.setStatus(newStatus);

        // Si la tarea se completa o falla, actualizar contador del nodo
        if ((newStatus == TaskStatus.COMPLETED || newStatus == TaskStatus.FAILED) &&
                (previousStatus == TaskStatus.PENDING || previousStatus == TaskStatus.PROCESSING)) {

            Node node = task.getAssignedNode();
            if (node != null) {
                node.setActiveTasksCount(Math.max(0, node.getActiveTasksCount() - 1));

                // Si el nodo estaba ocupado y ahora tiene menos tareas, marcarlo como disponible
                if (node.getStatus() == NodeStatus.BUSY && node.getActiveTasksCount() < 10) {
                    node.setStatus(NodeStatus.AVAILABLE);
                }
                nodeRepository.save(node);
            }
        }

        // Si la tarea falla, incrementar contador de fallos
        if (newStatus == TaskStatus.FAILED) {
            task.setFailureCount(task.getFailureCount() + 1);
        }

        Task updatedTask = taskRepository.save(task);

        // Registrar en historial
        TaskHistory history = new TaskHistory();
        history.setTask(updatedTask);
        history.setNode(updatedTask.getAssignedNode());
        history.setPreviousStatus(previousStatus);
        history.setNewStatus(newStatus);
        history.setComments("Cambio de estado de " + previousStatus + " a " + newStatus);
        taskHistoryRepository.save(history);

        return convertToDTO(updatedTask);
    }

    @Override
    public List<TaskDTO> getTasksByStatus(String status) {
        TaskStatus taskStatus = TaskStatus.valueOf(status.toUpperCase());
        return taskRepository.findByStatus(taskStatus).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void reassignTasksFromInactiveNodes() {
        List<Node> availableNodes = nodeRepository.findByStatus(NodeStatus.AVAILABLE);
        if (availableNodes.isEmpty()) {
            throw new IllegalStateException("No hay nodos disponibles para reasignar tareas");
        }

        List<Node> inactiveNodes = nodeRepository.findByStatus(NodeStatus.INACTIVE);
        for (Node inactiveNode : inactiveNodes) {
            List<Task> tasks = taskRepository.findByAssignedNodeAndStatus(inactiveNode, TaskStatus.PROCESSING);
            tasks.addAll(taskRepository.findByAssignedNodeAndStatus(inactiveNode, TaskStatus.PENDING));

            for (Task task : tasks) {
                // Seleccionar un nodo disponible con menos carga
                Node targetNode = availableNodes.stream()
                        .min(Comparator.comparing(Node::getActiveTasksCount))
                        .orElseThrow(() -> new IllegalStateException("Error al seleccionar nodo para reasignación"));

                TaskStatus previousStatus = task.getStatus();
                task.setStatus(TaskStatus.PENDING);
                task.setAssignedNode(targetNode);
                taskRepository.save(task);

                // Incrementar contador en el nodo destino
                targetNode.setActiveTasksCount(targetNode.getActiveTasksCount() + 1);
                if (targetNode.getActiveTasksCount() >= 10) {
                    targetNode.setStatus(NodeStatus.BUSY);
                    availableNodes.remove(targetNode);
                }
                nodeRepository.save(targetNode);

                // Registrar en historial
                TaskHistory history = new TaskHistory();
                history.setTask(task);
                history.setNode(targetNode);
                history.setPreviousStatus(previousStatus);
                history.setNewStatus(TaskStatus.PENDING);
                history.setComments("Tarea reasignada del nodo inactivo " + inactiveNode.getName() +
                        " al nodo " + targetNode.getName());
                taskHistoryRepository.save(history);

                if (availableNodes.isEmpty()) {
                    break;
                }
            }
        }
    }

    private TaskDTO convertToDTO(Task task) {
        TaskDTO dto = new TaskDTO();
        dto.setId(task.getId());
        dto.setTitle(task.getTitle());
        dto.setDescription(task.getDescription());
        dto.setStatus(task.getStatus().name());
        dto.setCreatedAt(task.getCreatedAt());
        dto.setUpdatedAt(task.getUpdatedAt());
        dto.setFailureCount(task.getFailureCount());

        if (task.getAssignedNode() != null) {
            dto.setAssignedNodeId(task.getAssignedNode().getId());
            dto.setAssignedNodeName(task.getAssignedNode().getName());
        }

        return dto;
    }
}