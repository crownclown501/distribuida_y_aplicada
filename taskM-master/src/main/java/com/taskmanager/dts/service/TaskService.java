package com.taskmanager.dts.service;

import com.taskmanager.dts.entity.Node;
import com.taskmanager.dts.entity.Task;
import com.taskmanager.dts.repository.NodeRepository;
import com.taskmanager.dts.repository.TaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Random;

@Service
public class TaskService {
    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private NodeRepository nodeRepository;

    public List<Task> getAllTasks() {
        return taskRepository.findAll();
    }

    public Task saveTask(Task task) {
        // Configurar valores iniciales para una nueva tarea
        if (task.getStatus() == null) {
            task.setStatus("pendiente");
        }

        // Guardar la tarea primero
        Task savedTask = taskRepository.save(task);

        // Intentar asignar la tarea a un nodo automáticamente
        assignTaskToAvailableNode(savedTask);

        return savedTask;
    }

    private void assignTaskToAvailableNode(Task task) {
        // Obtener nodos activos
        List<Node> activeNodes = nodeRepository.findByStatus("activo");

        if (activeNodes != null && !activeNodes.isEmpty()) {
            // Seleccionar un nodo de forma aleatoria o por algún criterio de balanceo
            Random random = new Random();
            Node selectedNode = activeNodes.get(random.nextInt(activeNodes.size()));

            // Asignar la tarea al nodo seleccionado
            task.setResponsibleNode(selectedNode.getNodeId());
            task.setStatus("procesando");
            taskRepository.save(task);
        }
        // Si no hay nodos activos, la tarea permanece en estado "pendiente"
    }

    public void deleteTask(Long id) {
        taskRepository.deleteById(id);
    }

    public Task getTaskById(Long id) {
        return taskRepository.findById(id).orElse(null);
    }

    public void registerTaskAsResponsible(Long taskId, String nodeId) {
        Task task = taskRepository.findById(taskId).orElse(null);
        if (task != null) {
            task.setResponsibleNode(nodeId);
            task.setStatus("procesando");
            taskRepository.save(task);
        }
    }
}