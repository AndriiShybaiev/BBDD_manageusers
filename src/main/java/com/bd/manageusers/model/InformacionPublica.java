package com.bd.manageusers.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.springframework.data.domain.Persistable;

import java.io.Serializable;

@Entity
@Table(name = "informacion_publica")
@Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InformacionPublica implements Persistable<Integer>, Serializable {

    private static final long serialVersionUID = 1L;

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

    @Transient
    private boolean isNew = true;

    public void setMostrar_email(boolean mostrar_email) {
        this.mostrarEmail = mostrar_email;
    }

    public void setMostrar_nacido(boolean mostrar_nacido) {
        this.mostrarNacido = mostrar_nacido;
    }

    public void setMostrar_nombre(boolean mostrar_nombre) {
        this.mostrarNombre = mostrar_nombre;
    }

    public boolean isMostrar_email() {
        return mostrarEmail;
    }

    public boolean isMostrar_nacido() {
        return mostrarNacido;
    }

    public boolean isMostrar_nombre() {
        return mostrarNombre;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        this.isNew = false;
    }
}
