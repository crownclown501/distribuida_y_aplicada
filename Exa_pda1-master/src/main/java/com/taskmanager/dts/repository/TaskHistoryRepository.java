package com.taskmanager.dts.repository;

import com.taskmanager.dts.entity.Task;
import com.taskmanager.dts.entity.TaskHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TaskHistoryRepository extends JpaRepository<TaskHistory, Long> {

    List<TaskHistory> findByTaskOrderByTimestampDesc(Task task);
}