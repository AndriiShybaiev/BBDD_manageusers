package com.bd.manageusers.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConexionId implements Serializable {

    @Column(name = "id_usuario")
    private Integer idUsuario;

    @Column(name = "momento_entrada")
    private LocalDateTime momentoEntrada;
}
