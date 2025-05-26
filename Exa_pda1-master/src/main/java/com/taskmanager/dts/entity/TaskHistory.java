package com.taskmanager.dts.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "task_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @ManyToOne
    @JoinColumn(name = "node_id")
    private Node node;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskStatus newStatus;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(length = 500)
    private String comments;

    @PrePersist
    protected void onCreate() {
        timestamp = LocalDateTime.now();
    }
}