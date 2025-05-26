package com.taskmanager.dts.controller;

import com.taskmanager.dts.entity.Node;
import com.taskmanager.dts.repository.NodeRepository;
import com.taskmanager.dts.service.NodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/nodes")
public class NodeStatusController {

    @Autowired
    private NodeRepository nodeRepository;

    @Autowired
    private NodeService nodeService;

    @GetMapping("/status")
    public List<Node> getNodesStatus() {
        return nodeRepository.findAll();
    }

    @PostMapping("/register/{nodeId}")
    public Node registerNode(@PathVariable String nodeId) {
        return nodeService.registerNode(nodeId);
    }

    @PostMapping("/{nodeId}/deactivate")
    public void deactivateNode(@PathVariable String nodeId) {
        nodeService.deactivateNode(nodeId);
    }
}