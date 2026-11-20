package com.bd.manageusers.service;

import com.bd.manageusers.model.Conexion;
import com.bd.manageusers.model.InformacionPublica;
import com.bd.manageusers.model.Perfil;
import com.bd.manageusers.model.Usuario;
import com.bd.manageusers.repository.InformacionPublicaRepository;
import com.bd.manageusers.repository.UsuarioRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.ajbrown.namemachine.Name;
import org.ajbrown.namemachine.NameGenerator;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Práctica 3 (rendimiento) dirigida por application.properties.
 *
 * El código NO cambia entre experimentos: lo único que se modifica (y se reinicia la aplicación)
 * son las propiedades de application.properties (y, en la prueba 2, las anotaciones de fetching
 * de la entidad Usuario):
 *
 *   - spring.jpa.properties.hibernate.cache.*      -> caché de segundo nivel (prueba 1.1)
 *   - spring.jpa.properties.hibernate.jdbc.batch_size, order_inserts, order_updates -> batching (prueba 1.2)
 *   - spring.jpa.properties.hibernate.default_batch_fetch_size                      -> fetching por lotes (prueba 2, ampliación)
 *   - spring.jpa.properties.hibernate.generate_statistics, spring.jpa.show-sql     -> medición
 *   - performance.*                                -> qué prueba se ejecuta y sus parámetros
 *
 *
 * Los métodos públicos NO son @Transactional: cada fase se ejecuta con TransactionTemplate para que
 * tenga su propia sesión de Hibernate (y su propio bloque "Session Metrics"), igual que los dos
 * beginTransaction()/commit() de las clases antiguas. Con @Transactional, al llamarse los métodos
 * dentro de la misma clase, no pasarían por el proxy de Spring (autoinvocación).
 */
@Service
@RequiredArgsConstructor
public class PerformanceService {

    // ---- Parámetros de la práctica (application.properties, prefijo "performance.") ----
    @Value("${performance.prueba:1.1}")
    private String prueba;

    @Value("${performance.num-usuarios:1000}")
    private int numUsuarios;

    @Value("${performance.num-modificaciones:50}")
    private int numModificaciones;

    /** Prueba 1.1: true = em.detach(info) en cada iteración (antiguo session.evict(info)). */
    @Value("${performance.detach-entidad:true}")
    private boolean detachEntidad;

    /** Prueba 1.1: true = em.flush() antes del detach, lo que lanza los UPDATE (experimento opcional). */
    @Value("${performance.flush-cambios:false}")
    private boolean flushCambios;

    // ---- Parámetros de la prueba 2 ----
    @Value("${performance.prueba2.id-min:1010}")
    private int idMin;

    @Value("${performance.prueba2.id-max:1020}")
    private int idMax;

    /** hql | derivada | join-fetch | entity-graph */
    @Value("${performance.prueba2.variante:hql}")
    private String variante;

    /** false = solo la consulta (apartado 1); true = además recorre las conexiones (apartado 2). */
    @Value("${performance.prueba2.recorrer-conexiones:true}")
    private boolean recorrerConexiones;

    /** true = imprime cada momento de conexión (como pide el enunciado); false = lo recorre sin imprimir. */
    @Value("${performance.prueba2.mostrar-conexiones:true}")
    private boolean mostrarConexiones;

    private final UsuarioRepository usuarioRepository;
    private final InformacionPublicaRepository informacionPublicaRepository;
    private final TransactionTemplate tx;
    private final EntityManagerFactory emf;
    private final Environment env;

    // EntityManager compartido (proxy ligado a la transacción en curso); equivale a la Session actual
    @PersistenceContext
    private EntityManager em;

    // =====================================================================================
    // Punto de entrada: elige la prueba según performance.prueba
    // =====================================================================================

    public void ejecutar() {
        imprimirConfiguracionActiva();
        validarConfiguracion();

        switch (prueba.trim()) {
            case "1.1" -> prueba11CacheSegundoNivel();
            case "1.2" -> prueba12Batches();
            case "2" -> prueba2Fetching();
            default -> throw new IllegalArgumentException(
                    "performance.prueba debe ser 1.1, 1.2 o 2 (valor actual: " + prueba + ")");
        }
    }

    // =====================================================================================
    // PRUEBA 1.1 - Caché de segundo nivel
    // Variables en application.properties:
    //   hibernate.cache.use_second_level_cache = true | false
    //   performance.detach-entidad             = true | false
    // =====================================================================================

    public void prueba11CacheSegundoNivel() {
        System.out.println("### PRUEBA 1.1 - caché de segundo nivel");

        // Fase (a): sesión 1 -> inserta los usuarios con su información pública (1er bloque de estadísticas)
        List<Integer> idsUsuarios = tx.execute(_ -> crearUsuariosConInfoPublica());

        // Fase (b): sesión 2 -> modificaciones (2º bloque de estadísticas, el que hay que analizar)
        estadisticas().clear();
        long inicio = System.nanoTime();
        tx.executeWithoutResult(_ -> modificarInfoPublica(idsUsuarios));
        imprimirEstadisticas("Prueba 1.1 - fase (b): modificaciones", System.nanoTime() - inicio);
    }

    private List<Integer> crearUsuariosConInfoPublica() {
        List<Name> names = new NameGenerator().generateNames(numUsuarios);
        Random rnd = new Random();
        List<Integer> createdUserIds = new ArrayList<>();

        for (Name n : names) {
            Usuario user = construirUsuario(n, rnd);
            usuarioRepository.save(user);            // id primitivo = 0 -> "nuevo" -> persist (IDENTITY: INSERT inmediato)
            createdUserIds.add(user.getId());

            InformacionPublica info = construirInfoPublica(user, rnd);
            informacionPublicaRepository.save(info); // id null (lo rellena @MapsId) -> "nueva" -> persist
        }
        return createdUserIds;
    }

    private void modificarInfoPublica(List<Integer> idsUsuarios) {
        for (int i = 0; i < numModificaciones; i++) {
            for (int idUser : idsUsuarios) {
                // em.find: consulta caché L1 -> caché L2 (si está activa en properties) -> BD
                InformacionPublica info = informacionPublicaRepository.findById(idUser).orElseThrow();
                info.setMostrar_email(Math.random() < 0.5);
                info.setMostrar_nacido(Math.random() < 0.5);
                info.setMostrar_nombre(Math.random() < 0.5);

                // No hace falta save(): la entidad ya está gestionada y Hibernate detecta el cambio
                // (dirty checking) al hacer flush. El persist() del código antiguo tampoco hacía nada.
                if (flushCambios) {
                    em.flush();
                }
                if (detachEntidad) {
                    em.detach(info);                 // equivale a session.evict(info);
                }
            }
        }
    }

    // =====================================================================================
    // PRUEBA 1.2 - Batches
    // Variables en application.properties:
    //   hibernate.jdbc.batch_size = 0 | 50   (con order_inserts / order_updates = true)
    //   hibernate.cache.use_second_level_cache = false
    // =====================================================================================

    public void prueba12Batches() {
        System.out.println("### PRUEBA 1.2 - batches");

        // Sesión 1: los usuarios (1er bloque). Con IDENTITY estos INSERT no se agrupan en batch.
        List<Usuario> usuarios = tx.execute(_ -> crearUsuarios());

        // Sesión 2: información pública que se añade y se elimina (2º bloque)
        estadisticas().clear();
        long inicio = System.nanoTime();
        tx.executeWithoutResult(_ -> crearYBorrarInfoPublica(usuarios));
        imprimirEstadisticas("Prueba 1.2 - fase (b): alta y baja de información pública", System.nanoTime() - inicio);
    }

    private List<Usuario> crearUsuarios() {
        List<Name> names = new NameGenerator().generateNames(numUsuarios);
        Random rnd = new Random();
        List<Usuario> creados = new ArrayList<>();

        for (Name n : names) {
            Usuario user = construirUsuario(n, rnd);
            usuarioRepository.save(user);
            creados.add(user);
        }
        return creados;
    }

    private void crearYBorrarInfoPublica(List<Usuario> usuarios) {
        Random rnd = new Random();
        for (Usuario curUser : usuarios) {
            InformacionPublica info = construirInfoPublica(curUser, rnd);
            em.persist(info);
            em.remove(info);
        }
    }

    // =====================================================================================
    // PRUEBA 2 - Consultas y estrategias de fetching
    // Variables en application.properties:
    //   performance.prueba2.variante            = hql | derivada | join-fetch | entity-graph
    //   performance.prueba2.recorrer-conexiones = false (apartado 1) | true (apartado 2)
    //   hibernate.default_batch_fetch_size      = (ampliación)
    // Variables en el código (anotaciones de Usuario.conexiones):
    //   fetch = LAZY | EAGER   y   @Fetch(FetchMode.SELECT) | @Fetch(FetchMode.SUBSELECT)
    //
    // Todo ocurre en UNA sola transacción (una sola sesión): sin ella, getConexiones() lanzaría
    // LazyInitializationException, porque en una aplicación de consola no hay "open in view".
    // =====================================================================================

    public void prueba2Fetching() {
        System.out.println("### PRUEBA 2 - consultas y fetching (variante=" + variante
                + ", ids " + idMin + "-" + idMax + ")");

        Statistics stats = estadisticas();
        stats.clear();
        long inicio = System.nanoTime();

        tx.executeWithoutResult(_ -> {
            // Apartado 1: la consulta de usuarios
            List<Usuario> usuarios = consultarUsuarios();
            long sentenciasTrasConsulta = stats.getPrepareStatementCount();
            System.out.println("Usuarios recuperados: " + usuarios.size());
            if (usuarios.isEmpty()) {
                System.out.println("(!) No hay usuarios en ese rango de ids; prueba con otro rango");
            }

            // Apartado 2: el bucle sobre las conexiones
            if (recorrerConexiones) {
                for (Usuario u : usuarios) {
                    for (Conexion c : u.getConexiones()) {
                        if (mostrarConexiones) {
                            System.out.println("Usuario " + u.getId() + " -> " + c.getId().getMomentoEntrada());
                        } else {
                            c.getId().getMomentoEntrada();
                        }
                    }
                }
            }
            long sentenciasTotal = stats.getPrepareStatementCount();

            System.out.println("=== Prueba 2 - resultados (variante=" + variante + ") ===");
            System.out.printf("Sentencias SQL tras la consulta de usuarios: %d%n", sentenciasTrasConsulta);
            if (recorrerConexiones) {
                System.out.printf("Sentencias SQL tras recorrer las conexiones: %d (incremento: %d)%n",
                        sentenciasTotal, sentenciasTotal - sentenciasTrasConsulta);
            }
            System.out.printf("Entidades cargadas: %d | colecciones cargadas: %d | consultas HQL/JPQL: %d%n",
                    stats.getEntityLoadCount(), stats.getCollectionLoadCount(), stats.getQueryExecutionCount());
            System.out.printf("Tiempo total: %d ms%n", (System.nanoTime() - inicio) / 1_000_000);
        });
    }

    private List<Usuario> consultarUsuarios() {
        return switch (variante.trim()) {
            case "hql" -> usuarioRepository.findByIdEntreHQL(idMin, idMax);
            case "derivada" -> usuarioRepository.findByIdBetween(idMin, idMax);
            case "join-fetch" -> usuarioRepository.findByIdEntreJoinFetch(idMin, idMax);
            case "entity-graph" -> usuarioRepository.findWithGraphByIdBetween(idMin, idMax);
            default -> throw new IllegalArgumentException(
                    "performance.prueba2.variante debe ser hql, derivada, join-fetch o entity-graph (valor actual: "
                            + variante + ")");
        };
    }

    // =====================================================================================
    // Configuración activa y validación (lo que realmente ha leído Hibernate al arrancar)
    // =====================================================================================

    private void imprimirConfiguracionActiva() {
        System.out.println("==================== CONFIGURACIÓN ACTIVA ====================");
        System.out.println("performance.prueba                     = " + prueba);
        if ("2".equals(prueba.trim())) {
            System.out.println("performance.prueba2.variante           = " + variante);
            System.out.println("performance.prueba2.id-min / id-max    = " + idMin + " / " + idMax);
            System.out.println("performance.prueba2.recorrer-conexiones= " + recorrerConexiones);
            System.out.println("performance.prueba2.mostrar-conexiones = " + mostrarConexiones);
        } else {
            System.out.println("performance.num-usuarios               = " + numUsuarios);
            System.out.println("performance.num-modificaciones         = " + numModificaciones);
            System.out.println("performance.detach-entidad             = " + detachEntidad);
            System.out.println("performance.flush-cambios              = " + flushCambios);
        }
        System.out.println("---------------- Hibernate (propiedades efectivas) ----------------");
        for (String clave : new String[]{
                "hibernate.generate_statistics",
                "hibernate.show_sql",
                "hibernate.cache.use_second_level_cache",
                "hibernate.cache.region.factory_class",
                "hibernate.javax.cache.provider",
                "hibernate.javax.cache.uri",
                "hibernate.jdbc.batch_size",
                "hibernate.order_inserts",
                "hibernate.order_updates",
                "hibernate.default_batch_fetch_size"}) {
            System.out.printf("%-38s = %s%n", clave, prop(clave));
        }
        System.out.println("logging.level.org.hibernate.SQL        = "
                + env.getProperty("logging.level.org.hibernate.SQL", "(no definida)"));
        System.out.println("==============================================================");
    }

    /** Avisa si las propiedades no cumplen los requisitos del enunciado para la prueba elegida. */
    private void validarConfiguracion() {
        List<String> avisos = new ArrayList<>();
        String nivelSql = env.getProperty("logging.level.org.hibernate.SQL", "");
        boolean sqlVisible = propActiva("hibernate.show_sql")
                || nivelSql.equalsIgnoreCase("DEBUG") || nivelSql.equalsIgnoreCase("TRACE");
        String p = prueba.trim();

        if (!propActiva("hibernate.generate_statistics")) {
            avisos.add("Activa spring.jpa.properties.hibernate.generate_statistics=true");
        }

        if ("2".equals(p)) {
            if (!sqlVisible) {
                avisos.add("Prueba 2: activa spring.jpa.show-sql=true para ver las consultas SQL");
            }
            if (propActiva("hibernate.cache.use_second_level_cache")) {
                avisos.add("Prueba 2: desactiva la caché de segundo nivel (altera el número de consultas)");
            }
        } else {
            if (sqlVisible) {
                avisos.add("Desactiva spring.jpa.show-sql y pon logging.level.org.hibernate.SQL=OFF (distorsiona los tiempos)");
            }
            if ("1.1".equals(p) && propEntero("hibernate.jdbc.batch_size") > 1) {
                avisos.add("Prueba 1.1: el enunciado pide batching desactivado (batch_size=0)");
            }
            if ("1.2".equals(p) && propActiva("hibernate.cache.use_second_level_cache")) {
                avisos.add("Prueba 1.2: el enunciado pide la caché de segundo nivel desactivada");
            }
        }

        avisos.forEach(a -> System.out.println("(!) " + a));
    }

    // =====================================================================================
    // Utilidades
    // =====================================================================================

    private Usuario construirUsuario(Name n, Random rnd) {
        Usuario user = new Usuario();
        user.setNombre(n.getFirstName());
        user.setApellidos(n.getLastName());
        user.setEmail(n.getFirstName().toLowerCase() + "." + n.getLastName().toLowerCase() + "@dlsi.ua.es");
        user.setPassword(Long.toHexString(Double.doubleToLongBits(Math.random())).getBytes());
        user.setApodo(null);

        // em.find con (short): el id de Perfil es Short, así se evita depender de la
        // conversión numérica y de los genéricos de PerfilRepository.
        Perfil perfil = em.find(Perfil.class, (short) rnd.nextInt(3));
        if (perfil == null) {
            throw new IllegalStateException("No existe el perfil con el id generado; revisa la tabla perfil");
        }
        user.setPerfil(perfil);
        user.setNacido(LocalDate.of(1950, 1, 1).plusDays(rnd.nextInt(25000)));
        return user;
    }

    private InformacionPublica construirInfoPublica(Usuario user, Random rnd) {
        InformacionPublica info = new InformacionPublica();
        info.setUsuario(user);
        info.setMostrar_email(Math.random() < 0.5);
        info.setMostrar_nacido(Math.random() < 0.5);
        info.setMostrar_nombre(Math.random() < 0.5);
        info.setValoracion(rnd.nextDouble() * 10);
        return info;
    }

    private String prop(String clave) {
        Object valor = emf.getProperties().get(clave);
        return valor == null ? "(no definida)" : String.valueOf(valor);
    }

    private boolean propActiva(String clave) {
        return Boolean.parseBoolean(prop(clave));
    }

    private int propEntero(String clave) {
        try {
            return Integer.parseInt(prop(clave).trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private Statistics estadisticas() {
        return emf.unwrap(SessionFactory.class).getStatistics();
    }

    private void imprimirEstadisticas(String titulo, long nanos) {
        Statistics s = estadisticas();
        System.out.println("=== " + titulo + " ===");
        System.out.printf("Tiempo total de la fase: %d ms%n", nanos / 1_000_000);
        System.out.printf("Sentencias JDBC preparadas: %d%n", s.getPrepareStatementCount());
        System.out.printf("Entidades cargadas: %d | insertadas: %d | actualizadas: %d | borradas: %d%n",
                s.getEntityLoadCount(), s.getEntityInsertCount(), s.getEntityUpdateCount(), s.getEntityDeleteCount());
        System.out.printf("Caché L2 -> hits: %d | misses: %d | puts: %d%n",
                s.getSecondLevelCacheHitCount(), s.getSecondLevelCacheMissCount(), s.getSecondLevelCachePutCount());
        System.out.println("(Los tiempos JDBC, de batches y de caché por sesión están en el bloque 'Session Metrics' del log)");
    }
}
