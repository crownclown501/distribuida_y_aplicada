package com.taskmanager.dts;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling  // Activar la programación de tareas para el heartbeat
public class DistributedTaskSystemApplication {

	public static void main(String[] args) {
		SpringApplication.run(DistributedTaskSystemApplication.class, args);
	}

}