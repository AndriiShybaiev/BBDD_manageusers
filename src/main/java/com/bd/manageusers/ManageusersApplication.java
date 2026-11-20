package com.bd.manageusers;

import com.bd.manageusers.service.PerformanceService;
import com.bd.manageusers.service.QueriesService;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@RequiredArgsConstructor
@NullMarked
public class ManageusersApplication implements CommandLineRunner {

    @Value("${app.mode:idle}")
    private String appMode;

    private final QueriesService queriesService;
    private final PerformanceService performanceService;

    static void main(String[] args) {
        SpringApplication.run(ManageusersApplication.class, args);
    }

    @Override
    public void run(String... args) {
        switch (appMode.toLowerCase().trim()) {
            case "queries" -> {
                System.out.println("=== Ejecutando consultas de la Sesión 6 (QueriesService) ===");
                queriesService.ejecutarTodo();
            }
            case "performance" -> {
                System.out.println("=== Ejecutando pruebas de rendimiento de la Sesión 7 (PerformanceService) ===");
                performanceService.ejecutar();
            }
            case "idle" -> {
                System.out.println("=== Aplicación iniciada en modo seguro (IDLE) ===");
                System.out.println("Para ejecutar una práctica, configure 'app.mode' en application.properties:");
                System.out.println(" - app.mode=queries      -> Ejecuta los 13 ejercicios de consulta (Sesión 6)");
                System.out.println(" - app.mode=performance  -> Ejecuta las pruebas de rendimiento (Sesión 7)");
            }
            default -> throw new IllegalArgumentException(
                    "Modo 'app.mode=" + appMode + "' no reconocido. Opciones válidas: 'queries', 'performance', 'idle'");
        }
    }
}
