package com.bd.manageusers.specification;

import com.bd.manageusers.model.Conexion;
import com.bd.manageusers.model.InformacionPublica;
import com.bd.manageusers.model.Perfil;
import com.bd.manageusers.model.Usuario;
import jakarta.persistence.criteria.Join;
import org.springframework.data.jpa.domain.Specification;

public class ConexionSpecifications {

    /**
     * Q9: Especificación Criteria para obtener conexiones de usuarios con perfil 'Premium' y valoración >= 6.0
     */
    public static Specification<Conexion> isPremiumAndHighRating() {
        return (root, _, cb) -> {
            Join<Conexion, Usuario> usuarioJoin = root.join("usuario");
            Join<Usuario, Perfil> perfilJoin = usuarioJoin.join("perfil");
            Join<Usuario, InformacionPublica> infoPublicaJoin = usuarioJoin.join("informacionPublica");
            return cb.and(
                cb.equal(perfilJoin.get("descripcion"), "Premium"),
                cb.greaterThanOrEqualTo(infoPublicaJoin.get("valoracion"), 6.0)
            );
        };
    }
}
