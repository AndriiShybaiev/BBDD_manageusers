package com.bd.manageusers;

import com.bd.manageusers.service.DataGeneratorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ManageusersApplication implements CommandLineRunner {

    @Autowired
    private DataGeneratorService dataGeneratorService;

    public static void main(String[] args) {
        SpringApplication.run(ManageusersApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("Iniciando la carga masiva de datos...");

        dataGeneratorService.generateAllData();

        System.out.println("¡Carga finalizada con éxito!");
    }
}
