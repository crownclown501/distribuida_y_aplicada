package com.taskmanager.dts.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NodeDTO {
    private Long id;
    private String name;
    private String host;
    private Integer port;
    private String status;
    private LocalDateTime lastHeartbeat;
    private Integer activeTasksCount;
}