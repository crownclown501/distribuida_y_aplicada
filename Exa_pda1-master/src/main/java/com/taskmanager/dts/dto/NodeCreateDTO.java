package com.taskmanager.dts.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NodeCreateDTO {
    @NotBlank(message = "El nombre del nodo es obligatorio")
    private String name;

    @NotBlank(message = "El host es obligatorio")
    private String host;

    @NotNull(message = "El puerto es obligatorio")
    @Min(value = 1, message = "El puerto debe ser mayor que 0")
    private Integer port;
}