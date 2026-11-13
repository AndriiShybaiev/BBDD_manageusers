package com.bd.manageusers.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "usuario")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Integer id;

    @Column(name = "password", length = 64)
    private byte[] password;

    @Column(name = "apodo", length = 15)
    private String apodo;

    @Column(name = "email", nullable = false, length = 254)
    private String email;

    @Column(name = "nombre", nullable = false, length = 50)
    private String nombre;

    @Column(name = "apellidos", nullable = false, length = 75)
    private String apellidos;

    @Temporal(TemporalType.DATE)
    @Column(name = "nacido", nullable = false)
    private Date nacido;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_perfil")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Perfil perfil;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "sigue",
        joinColumns = @JoinColumn(name = "id_seguidor"),
        inverseJoinColumns = @JoinColumn(name = "id_seguido")
    )
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<Usuario> seguidos = new HashSet<>();
}
