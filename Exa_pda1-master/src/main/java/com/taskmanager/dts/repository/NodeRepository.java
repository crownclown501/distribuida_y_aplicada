package com.taskmanager.dts.repository;

import com.taskmanager.dts.entity.Node;
import com.taskmanager.dts.entity.NodeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NodeRepository extends JpaRepository<Node, Long> {

    List<Node> findByStatus(NodeStatus status);

    List<Node> findByLastHeartbeatBefore(LocalDateTime dateTime);
}