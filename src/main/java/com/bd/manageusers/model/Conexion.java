package com.bd.manageusers.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "conexion")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Conexion {

    @EmbeddedId
    private ConexionId id;

    @MapsId("idUsuario")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Usuario usuario;
}
