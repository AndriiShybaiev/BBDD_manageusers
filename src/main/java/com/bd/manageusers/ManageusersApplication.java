package com.bd.manageusers;

import com.bd.manageusers.service.QueriesService;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@RequiredArgsConstructor
@NullMarked
public class ManageusersApplication implements CommandLineRunner {

    // Inyección de QueriesService para la ejecución de consultas Q1-Q13 (Sesión 6)
    private final QueriesService queriesService;

    // Nota: DataGeneratorService (Sesión 5) se conserva en el proyecto, pero no se ejecuta automáticamente
    // para evitar la sobreescritura o regeneración accidental de los datos existentes en la BD.

    static void main(String[] args) {
        SpringApplication.run(ManageusersApplication.class, args);
    }

    @Override
    public void run(String... args) {
        queriesService.ejecutarTodo();
    }
}
