package com.taskmanager.dts.repository;

import com.taskmanager.dts.entity.Node;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NodeRepository extends JpaRepository<Node, Long> {
    List<Node> findByStatus(String status);
    Node findByNodeId(String nodeId);
}
