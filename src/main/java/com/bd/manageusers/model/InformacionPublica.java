package com.bd.manageusers.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "informacion_publica")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InformacionPublica {

    @Id
    @Column(name = "id_usuario")
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "id_usuario")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Usuario usuario;

    @Column(name = "mostrar_email", nullable = false)
    private boolean mostrarEmail;

    @Column(name = "mostrar_nacido", nullable = false)
    private boolean mostrarNacido;

    @Column(name = "mostrar_nombre", nullable = false)
    private boolean mostrarNombre;

    @Column(name = "valoracion", nullable = false)
    private double valoracion;
}
