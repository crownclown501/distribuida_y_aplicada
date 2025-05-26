package com.taskmanager.dts.repository;

import com.taskmanager.dts.entity.Node;
import com.taskmanager.dts.entity.Task;
import com.taskmanager.dts.entity.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByStatus(TaskStatus status);

    List<Task> findByAssignedNode(Node node);

    List<Task> findByAssignedNodeAndStatus(Node node, TaskStatus status);
}