package com.taskmanager.dts.controller;

import com.taskmanager.dts.dto.NodeCreateDTO;
import com.taskmanager.dts.dto.NodeDTO;
import com.taskmanager.dts.service.NodeService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class NodeController {

    @Autowired
    private NodeService nodeService;

    @PostMapping("/nodes")
    public ResponseEntity<NodeDTO> createNode(@Valid @RequestBody NodeCreateDTO nodeCreateDTO) {
        NodeDTO createdNode = nodeService.createNode(nodeCreateDTO);
        return new ResponseEntity<>(createdNode, HttpStatus.CREATED);
    }

    @GetMapping("/nodes/{id}")
    public ResponseEntity<NodeDTO> getNodeById(@PathVariable Long id) {
        NodeDTO node = nodeService.getNodeById(id);
        return ResponseEntity.ok(node);
    }

    @GetMapping("/nodes")
    public ResponseEntity<List<NodeDTO>> getAllNodes() {
        List<NodeDTO> nodes = nodeService.getAllNodes();
        return ResponseEntity.ok(nodes);
    }

    @DeleteMapping("/nodes/{id}")
    public ResponseEntity<Void> deleteNode(@PathVariable Long id) {
        nodeService.deleteNode(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/nodes/{id}/status")
    public ResponseEntity<NodeDTO> updateNodeStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        NodeDTO updatedNode = nodeService.updateNodeStatus(id, status);
        return ResponseEntity.ok(updatedNode);
    }

    @GetMapping("/nodes/status")
    public ResponseEntity<List<NodeDTO>> getAllNodesStatus() {
        List<NodeDTO> nodes = nodeService.getAllNodesStatus();
        return ResponseEntity.ok(nodes);
    }

    @PutMapping("/nodes/{id}/heartbeat")
    public ResponseEntity<Void> updateNodeHeartbeat(@PathVariable Long id) {
        nodeService.updateNodeHeartbeat(id);
        return ResponseEntity.noContent().build();
    }
}