package com.bd.manageusers.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "usuario")
@Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Usuario implements Serializable {

    private static final long serialVersionUID = 1L;

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

    @Column(name = "nacido", nullable = false)
    private LocalDate nacido;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_perfil")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Perfil perfil;

    @OneToOne(mappedBy = "usuario")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private InformacionPublica informacionPublica;

    @OneToMany(mappedBy = "usuario", fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Conexion> conexiones = new ArrayList<>();

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
