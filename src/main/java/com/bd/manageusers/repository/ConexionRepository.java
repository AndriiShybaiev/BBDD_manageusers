package com.bd.manageusers.repository;

import com.bd.manageusers.model.Conexion;
import com.bd.manageusers.model.ConexionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ConexionRepository extends JpaRepository<Conexion, ConexionId>, JpaSpecificationExecutor<Conexion> {

    // Q2: Native SQL - Selección parcial (momento_entrada, id_usuario) ordenada de más reciente a más antigua
    @Query(value = "SELECT momento_entrada, id_usuario FROM conexion WHERE id_usuario = :idUsuario ORDER BY momento_entrada DESC", nativeQuery = true)
    List<Object[]> findMomentoEntradaAndIdUsuarioNative(@Param("idUsuario") int idUsuario);

    // Q3: Native SQL - Entidades Conexion en rango de fechas [1964-03-01, GETDATE()]
    @Query(value = "SELECT * FROM conexion WHERE id_usuario = :idUsuario AND momento_entrada >= '1964-03-01' AND momento_entrada <= GETDATE()", nativeQuery = true)
    List<Conexion> findConexionesNativeByUsuarioAndFechaRange(@Param("idUsuario") int idUsuario);

    // Q4: JPQL - Lista de fechas de conexión para un usuario ordenadas descendentemente
    @Query("SELECT c.id.momentoEntrada FROM Conexion c WHERE c.usuario.id = :idUsuario ORDER BY c.id.momentoEntrada DESC")
    List<LocalDateTime> findMomentosEntradaByUsuarioIdDesc(@Param("idUsuario") int idUsuario);

    // Q5: JPQL - Lista de objetos Conexion con fecha posterior a un parámetro dado (> 1966-02-01)
    @Query("SELECT c FROM Conexion c WHERE c.id.momentoEntrada > :fecha")
    List<Conexion> findByMomentoEntradaAfterJpql(@Param("fecha") LocalDateTime fecha);

    // Q8: JPQL - Fechas de conexión de usuarios con perfil 'Premium' y valoración >= 6.0
    @Query("SELECT c.id.momentoEntrada FROM Conexion c WHERE c.usuario.perfil.descripcion = 'Premium' AND c.usuario.informacionPublica.valoracion >= 6.0")
    List<LocalDateTime> findMomentosEntradaPremiumAltaValoracionJpql();

    // Q10: JPQL Bulk Delete - Eliminar conexiones de usuarios con perfil 'Básico'
    @Modifying
    @Query("DELETE FROM Conexion c WHERE c.usuario.perfil.descripcion = 'Básico'")
    int deleteConexionesUsuarioBasicoJpql();

    // Q11: Derived Query Method - Equivalente a Q2 (filtrado por idUsuario y ordenado descendente por momentoEntrada)
    List<Conexion> findById_IdUsuarioOrderById_MomentoEntradaDesc(Integer idUsuario);

    // Q12: Derived Query Method - Equivalente a Q5 (filtrado por momentoEntrada > fecha)
    List<Conexion> findById_MomentoEntradaAfter(LocalDateTime fecha);

    // Q13: Derived Query Method - Equivalente a Q8 (filtrado por perfil Premium y valoración >= valoracion)
    List<Conexion> findByUsuario_Perfil_DescripcionAndUsuario_InformacionPublica_ValoracionGreaterThanEqual(String descripcion, double valoracion);
}
