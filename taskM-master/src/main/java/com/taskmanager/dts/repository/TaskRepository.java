package com.taskmanager.dts.repository;

import com.taskmanager.dts.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByResponsibleNode(String responsibleNode);
}
