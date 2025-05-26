package com.taskmanager.dts.service;

import com.taskmanager.dts.dto.TaskCreateDTO;
import com.taskmanager.dts.dto.TaskDTO;
import com.taskmanager.dts.entity.Task;
import java.util.List;

public interface TaskService {
    TaskDTO createTask(TaskCreateDTO taskCreateDTO);
    TaskDTO getTaskById(Long id);
    List<TaskDTO> getAllTasks();
    void deleteTask(Long id);
    TaskDTO updateTaskStatus(Long id, String status);
    List<TaskDTO> getTasksByStatus(String status);
    void reassignTasksFromInactiveNodes();
}