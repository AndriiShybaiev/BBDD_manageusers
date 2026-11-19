package com.bd.manageusers.repository;

import com.bd.manageusers.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {

    // Q7: Native SQL - Para cada usuario con perfil 'Estándar' y valoración <= 4,
    // mostrar id_usuario, nombre, apellidos y total de conexiones, ordenado descendentemente por total de conexiones.
    @Query(value = """
        SELECT u.id_usuario, u.nombre, u.apellidos, COUNT(c.momento_entrada) AS total_conexiones
        FROM usuario u
        INNER JOIN perfil p ON u.id_perfil = p.id_perfil
        INNER JOIN informacion_publica ip ON u.id_usuario = ip.id_usuario
        LEFT JOIN conexion c ON u.id_usuario = c.id_usuario
        WHERE p.descripcion = N'Estándar' AND ip.valoracion <= 4.0
        GROUP BY u.id_usuario, u.nombre, u.apellidos
        ORDER BY COUNT(c.momento_entrada) DESC
        """, nativeQuery = true)
    List<Object[]> findUsuariosEstandarBajaValoracionConConexionesNative();
}
