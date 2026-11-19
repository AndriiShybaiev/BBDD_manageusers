package com.bd.manageusers.service;

import com.bd.manageusers.model.Conexion;
import com.bd.manageusers.model.Usuario;
import com.bd.manageusers.repository.ConexionRepository;
import com.bd.manageusers.repository.UsuarioRepository;
import com.bd.manageusers.specification.ConexionSpecifications;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class QueriesService {

    // Nota: La secuencia IDENTITY de la base de datos P501 inició la generación en el ID 1001 (rango 1001 - 2000).
    // Por ende, de acuerdo con las instrucciones de la práctica ("99 o cualquier ID existente"),
    // se establece ID_USUARIO = 1001 para asegurar la existencia real del usuario y sus conexiones.
    private static final int ID_USUARIO = 1001;

    private final UsuarioRepository usuarioRepository;
    private final ConexionRepository conexionRepository;
    private final TransactionTemplate transactionTemplate;

    @PersistenceContext
    private EntityManager entityManager;

    public void ejecutarTodo() {
        System.out.println("================================================================================");
        System.out.println("                  INICIO DE EJECUCIÓN DE CONSULTAS (Q1 - Q13)                   ");
        System.out.println("================================================================================");

        q1_findByIdConexiones();
        q2_nativeSqlSeleccionParcial();
        q3_nativeSqlEntidadesConexiones();
        q4_jpqlFechasConexionUsuario();
        q5_jpqlObjetosConexionPosterioresA1966();
        q6_criteriaApiTuplasFechasConexion();
        q7_nativeSqlAgregacionUsuariosEstandar();
        q8_jpqlFechasConexionPremium();
        q9_criteriaSpecificationConexionPremium();
        q10_jpqlBulkDeleteBasicoConRollback();
        q11_derivedQueryEquivalenteQ2();
        q12_derivedQueryEquivalenteQ5();
        q13_derivedQueryEquivalenteQ8();

        System.out.println("================================================================================");
        System.out.println("               FINALIZACIÓN DE EJECUCIÓN DE CONSULTAS (Q1 - Q13)                ");
        System.out.println("================================================================================");
    }

    /**
     * Q1: Utilizando los métodos de Spring Data que no necesitan implementación,
     * busca un usuario por su ID y muestra sus momentos de conexión navegando por su relación.
     */
    public void q1_findByIdConexiones() {
        System.out.println("\n--------------------------------------------------------------------------------");
        System.out.println("[Q1] Método Spring Data findById() y navegación por relación usuario.getConexiones()");
        System.out.println("--------------------------------------------------------------------------------");
        transactionTemplate.executeWithoutResult(_ -> {
            Optional<Usuario> usuarioOpt = usuarioRepository.findById(ID_USUARIO);
            if (usuarioOpt.isPresent()) {
                Usuario u = usuarioOpt.get();
                System.out.println("Usuario encontrado: [ID=" + u.getId() + "] " + u.getNombre() + " " + u.getApellidos() + " (" + u.getEmail() + ")");
                List<Conexion> conexiones = u.getConexiones();
                System.out.println("Total conexiones asociadas: " + conexiones.size());
                conexiones.forEach(c -> System.out.println("  Momento entrada: " + (c.getId() != null ? c.getId().getMomentoEntrada() : "N/A")));
            } else {
                System.out.println("Usuario con ID " + ID_USUARIO + " no fue encontrado en la base de datos.");
            }
        });
    }

    /**
     * Q2: Utilizando SQL nativo, muestra momento_entrada e id_usuario de las conexiones del usuario,
     * ordenadas de la más reciente a la más antigua.
     */
    public void q2_nativeSqlSeleccionParcial() {
        System.out.println("\n--------------------------------------------------------------------------------");
        System.out.println("[Q2] Native SQL - Selección parcial (momento_entrada, id_usuario) ORDER BY momento_entrada DESC");
        System.out.println("--------------------------------------------------------------------------------");
        List<Object[]> resultados = conexionRepository.findMomentoEntradaAndIdUsuarioNative(ID_USUARIO);
        System.out.println("Total registros encontrados: " + resultados.size());
        for (Object[] fila : resultados) {
            System.out.println("  Momento entrada: " + fila[0] + " | ID Usuario: " + fila[1]);
        }
    }

    /**
     * Q3: Utilizando SQL nativo, obtiene entidades Conexion de un usuario entre 1964-03-01 y la fecha actual,
     * y muestra fecha, nombre completo del usuario y descripción de su perfil.
     */
    public void q3_nativeSqlEntidadesConexiones() {
        System.out.println("\n--------------------------------------------------------------------------------");
        System.out.println("[Q3] Native SQL - Entidades Conexion en rango de fechas [1964-03-01, GETDATE()]");
        System.out.println("--------------------------------------------------------------------------------");
        transactionTemplate.executeWithoutResult(_ -> {
            List<Conexion> conexiones = conexionRepository.findConexionesNativeByUsuarioAndFechaRange(ID_USUARIO);
            System.out.println("Total entidades encontradas: " + conexiones.size());
            for (Conexion c : conexiones) {
                Usuario u = c.getUsuario();
                String nombreCompleto = (u != null) ? (u.getNombre() + " " + u.getApellidos()) : "Desconocido";
                String perfilDesc = (u != null && u.getPerfil() != null) ? u.getPerfil().getDescripcion() : "Sin perfil";
                System.out.println("  Fecha conexión: " + (c.getId() != null ? c.getId().getMomentoEntrada() : "N/A") + " | Usuario: " + nombreCompleto + " | Perfil: " + perfilDesc);
            }
        });
    }

    /**
     * Q4: Utilizando HQL/JPQL, muestra las fechas de conexión del usuario ordenadas de forma descendente.
     */
    public void q4_jpqlFechasConexionUsuario() {
        System.out.println("\n--------------------------------------------------------------------------------");
        System.out.println("[Q4] JPQL - Fechas de conexión del usuario ordenadas descendentemente");
        System.out.println("--------------------------------------------------------------------------------");
        List<LocalDateTime> fechas = conexionRepository.findMomentosEntradaByUsuarioIdDesc(ID_USUARIO);
        System.out.println("Total fechas obtenidas: " + fechas.size());
        fechas.forEach(f -> System.out.println("  Momento entrada: " + f));
    }

    /**
     * Q5: Utilizando HQL/JPQL, lista objetos Conexion cuya fecha sea mayor al 1 de febrero de 1966,
     * y extrae de cada uno de ellos su fecha de conexión.
     */
    public void q5_jpqlObjetosConexionPosterioresA1966() {
        System.out.println("\n--------------------------------------------------------------------------------");
        System.out.println("[Q5] JPQL - Lista de entidades Conexion con fecha > 1966-02-01 (extrayendo fechas)");
        System.out.println("--------------------------------------------------------------------------------");
        LocalDateTime fechaLimite = LocalDateTime.of(1966, 2, 1, 0, 0, 0);
        List<Conexion> conexiones = conexionRepository.findByMomentoEntradaAfterJpql(fechaLimite);
        System.out.println("Total entidades recuperadas: " + conexiones.size());
        conexiones.forEach(c -> System.out.println("  Fecha de conexión: " + (c.getId() != null ? c.getId().getMomentoEntrada() : "N/A")));
    }

    /**
     * Q6: Utilizando Criteria API y EntityManager, obtiene una lista de tuplas con las fechas de conexión del usuario.
     */
    public void q6_criteriaApiTuplasFechasConexion() {
        System.out.println("\n--------------------------------------------------------------------------------");
        System.out.println("[Q6] Criteria API (EntityManager) - Lista de tuplas no tipadas con fechas de conexión");
        System.out.println("--------------------------------------------------------------------------------");
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> cq = cb.createTupleQuery();
        Root<Conexion> root = cq.from(Conexion.class);

        cq.select(cb.tuple(root.get("id").get("momentoEntrada").alias("momentoEntrada")));
        cq.where(cb.equal(root.get("id").get("idUsuario"), ID_USUARIO));

        List<Tuple> tuplas = entityManager.createQuery(cq).getResultList();
        System.out.println("Total tuplas obtenidas: " + tuplas.size());
        for (Tuple t : tuplas) {
            System.out.println("  Tupla [momentoEntrada]: " + t.get("momentoEntrada"));
        }
    }

    /**
     * Q7: Utilizando SQL nativo, muestra id, nombre, apellidos y total de conexiones para usuarios con perfil
     * 'Estándar' y valoración <= 4, ordenado por total de conexiones descendente.
     */
    public void q7_nativeSqlAgregacionUsuariosEstandar() {
        System.out.println("\n--------------------------------------------------------------------------------");
        System.out.println("[Q7] Native SQL - Agregación (Perfil 'Estándar' y Valoración <= 4.0) ORDER BY Conexiones DESC");
        System.out.println("--------------------------------------------------------------------------------");
        List<Object[]> resultados = usuarioRepository.findUsuariosEstandarBajaValoracionConConexionesNative();
        System.out.println("Total usuarios encontrados: " + resultados.size());
        for (Object[] fila : resultados) {
            System.out.println("  ID: " + fila[0] + " | Nombre: " + fila[1] + " " + fila[2] + " | Total Conexiones: " + fila[3]);
        }
    }

    /**
     * Q8: Utilizando HQL/JPQL, muestra todas las fechas de conexión de usuarios con perfil 'Premium' y valoración >= 6.
     */
    public void q8_jpqlFechasConexionPremium() {
        System.out.println("\n--------------------------------------------------------------------------------");
        System.out.println("[Q8] JPQL - Fechas de conexión de usuarios con perfil 'Premium' y valoración >= 6.0");
        System.out.println("--------------------------------------------------------------------------------");
        List<LocalDateTime> fechas = conexionRepository.findMomentosEntradaPremiumAltaValoracionJpql();
        System.out.println("Total fechas obtenidas: " + fechas.size());
        fechas.forEach(f -> System.out.println("  Fecha conexión: " + f));
    }

    /**
     * Q9: Utilizando Criteria API a través de Spring Data Specification, obtiene el mismo resultado lógico que Q8.
     */
    public void q9_criteriaSpecificationConexionPremium() {
        System.out.println("\n--------------------------------------------------------------------------------");
        System.out.println("[Q9] Spring Data Specification - Conexiones de usuarios con perfil 'Premium' y valoración >= 6.0");
        System.out.println("--------------------------------------------------------------------------------");
        List<Conexion> conexiones = conexionRepository.findAll(ConexionSpecifications.isPremiumAndHighRating());
        System.out.println("Total entidades recuperadas mediante Specification: " + conexiones.size());
        conexiones.forEach(c -> System.out.println("  Fecha conexión (Specification): " + (c.getId() != null ? c.getId().getMomentoEntrada() : "N/A")));
    }

    /**
     * Q10: Utilizando HQL/JPQL Bulk Delete, elimina las conexiones de usuarios con perfil 'Básico'.
     * Se ejecuta dentro de una transacción que realiza ROLLBACK garantizado para preservar la base de datos.
     */
    public void q10_jpqlBulkDeleteBasicoConRollback() {
        System.out.println("\n--------------------------------------------------------------------------------");
        System.out.println("[Q10] JPQL Bulk Delete - Eliminar conexiones de usuarios con perfil 'Básico' (con ROLLBACK)");
        System.out.println("--------------------------------------------------------------------------------");
        long totalAntes = conexionRepository.count();
        System.out.println("Total de registros en 'conexion' antes de la operación: " + totalAntes);

        Integer filasEliminadas = transactionTemplate.execute(status -> {
            int eliminadas = conexionRepository.deleteConexionesUsuarioBasicoJpql();
            System.out.println("  > Filas afectadas por DELETE: " + eliminadas);
            status.setRollbackOnly();
            System.out.println("  > Transacción marcada explícitamente para ROLLBACK.");
            return eliminadas;
        });
        System.out.println("  > Total de filas eliminadas registradas en la transacción: " + filasEliminadas);

        long totalDespues = conexionRepository.count();
        System.out.println("Total de registros en 'conexion' después del ROLLBACK: " + totalDespues);
        if (totalAntes == totalDespues) {
            System.out.println("✓ ÉXITO: El rollback funcionó correctamente. La base de datos no fue alterada.");
        } else {
            System.err.println("✗ ADVERTENCIA: La cantidad de registros difiere después de la operación.");
        }
    }

    /**
     * Q11: Utilizando JPA Derived Query Methods, implementa la consulta equivalente a Q2.
     */
    public void q11_derivedQueryEquivalenteQ2() {
        System.out.println("\n--------------------------------------------------------------------------------");
        System.out.println("[Q11] JPA Derived Query - Equivalente a Q2 (findById_IdUsuarioOrderById_MomentoEntradaDesc)");
        System.out.println("--------------------------------------------------------------------------------");
        List<Conexion> conexiones = conexionRepository.findById_IdUsuarioOrderById_MomentoEntradaDesc(ID_USUARIO);
        System.out.println("Total registros encontrados: " + conexiones.size());
        for (Conexion c : conexiones) {
            if (c.getId() != null) {
                System.out.println("  Momento entrada: " + c.getId().getMomentoEntrada() + " | ID Usuario: " + c.getId().getIdUsuario());
            }
        }
    }

    /**
     * Q12: Utilizando JPA Derived Query Methods, implementa la consulta equivalente a Q5.
     */
    public void q12_derivedQueryEquivalenteQ5() {
        System.out.println("\n--------------------------------------------------------------------------------");
        System.out.println("[Q12] JPA Derived Query - Equivalente a Q5 (findById_MomentoEntradaAfter)");
        System.out.println("--------------------------------------------------------------------------------");
        LocalDateTime fechaLimite = LocalDateTime.of(1966, 2, 1, 0, 0, 0);
        List<Conexion> conexiones = conexionRepository.findById_MomentoEntradaAfter(fechaLimite);
        System.out.println("Total entidades encontradas: " + conexiones.size());
        conexiones.forEach(c -> System.out.println("  Fecha de conexión (Derived Query): " + (c.getId() != null ? c.getId().getMomentoEntrada() : "N/A")));
    }

    /**
     * Q13: Utilizando JPA Derived Query Methods, implementa la consulta equivalente a Q8.
     */
    public void q13_derivedQueryEquivalenteQ8() {
        System.out.println("\n--------------------------------------------------------------------------------");
        System.out.println("[Q13] JPA Derived Query - Equivalente a Q8 (findByUsuario_Perfil_DescripcionAnd...)");
        System.out.println("--------------------------------------------------------------------------------");
        List<Conexion> conexiones = conexionRepository.findByUsuario_Perfil_DescripcionAndUsuario_InformacionPublica_ValoracionGreaterThanEqual("Premium", 6.0);
        System.out.println("Total entidades encontradas: " + conexiones.size());
        conexiones.forEach(c -> System.out.println("  Fecha conexión (Derived Query): " + (c.getId() != null ? c.getId().getMomentoEntrada() : "N/A")));
    }
}
