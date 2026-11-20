package com.bd.manageusers.repository;

import com.bd.manageusers.model.Usuario;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    // Prueba 2 - Variante HQL
    @Query("SELECT u FROM Usuario u WHERE u.id BETWEEN :min AND :max")
    List<Usuario> findByIdEntreHQL(@Param("min") int min, @Param("max") int max);

    // Prueba 2 - Variante Derivada
    List<Usuario> findByIdBetween(int min, int max);

    // Prueba 2 - Variante JOIN FETCH
    @Query("SELECT DISTINCT u FROM Usuario u LEFT JOIN FETCH u.conexiones WHERE u.id BETWEEN :min AND :max")
    List<Usuario> findByIdEntreJoinFetch(@Param("min") int min, @Param("max") int max);

    // Prueba 2 - Variante EntityGraph
    @EntityGraph(attributePaths = {"conexiones"})
    List<Usuario> findWithGraphByIdBetween(int min, int max);
}
