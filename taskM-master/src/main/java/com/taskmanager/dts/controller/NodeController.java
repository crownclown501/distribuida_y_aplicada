package com.taskmanager.dts.controller;

import com.taskmanager.dts.service.HeartbeatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/nodes")
public class NodeController {
    @Autowired
    private HeartbeatService heartbeatService;

    @PostMapping("/{nodeId}/heartbeat")
    public void updateHeartbeat(@PathVariable String nodeId) {
        heartbeatService.updateHeartbeat(nodeId);
    }
}
