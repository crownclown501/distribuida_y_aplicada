package com.taskmanager.dts.config;

import com.taskmanager.dts.service.NodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
public class TaskSchedulerConfig {

    @Autowired
    private NodeService nodeService;

    @Scheduled(fixedDelayString = "${task.heartbeat.interval:30000}")
    public void checkInactiveNodes() {
        nodeService.checkInactiveNodes();
    }
}