package com.taskmanager.dts.service;

import com.taskmanager.dts.dto.NodeCreateDTO;
import com.taskmanager.dts.dto.NodeDTO;
import java.util.List;

public interface NodeService {
    NodeDTO createNode(NodeCreateDTO nodeCreateDTO);
    NodeDTO getNodeById(Long id);
    List<NodeDTO> getAllNodes();
    void deleteNode(Long id);
    NodeDTO updateNodeStatus(Long id, String status);
    List<NodeDTO> getAllNodesStatus();
    void updateNodeHeartbeat(Long id);
    void checkInactiveNodes();
}