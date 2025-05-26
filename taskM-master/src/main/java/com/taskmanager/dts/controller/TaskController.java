package com.taskmanager.dts.controller;

import com.taskmanager.dts.entity.Task;
import com.taskmanager.dts.service.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tasks")
public class TaskController {
    @Autowired
    private TaskService taskService;

    @GetMapping
    public List<Task> getAllTasks() {
        return taskService.getAllTasks();
    }

    @PostMapping
    public Task createTask(@RequestBody Task task) {
        return taskService.saveTask(task);
    }

    @DeleteMapping("/{id}")
    public void deleteTask(@PathVariable Long id) {
        taskService.deleteTask(id);
    }

    @GetMapping("/{id}")
    public Task getTaskById(@PathVariable Long id) {
        return taskService.getTaskById(id);
    }

    @PostMapping("/{taskId}/assign/{nodeId}")
    public void assignTaskToNode(@PathVariable Long taskId, @PathVariable String nodeId) {
        taskService.registerTaskAsResponsible(taskId, nodeId);
    }
}
