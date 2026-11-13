package com.bd.manageusers.service;

import com.bd.manageusers.model.Conexion;
import com.bd.manageusers.model.ConexionId;
import com.bd.manageusers.model.InformacionPublica;
import com.bd.manageusers.model.Perfil;
import com.bd.manageusers.model.Usuario;
import com.bd.manageusers.repository.ConexionRepository;
import com.bd.manageusers.repository.InformacionPublicaRepository;
import com.bd.manageusers.repository.PerfilRepository;
import com.bd.manageusers.repository.UsuarioRepository;
import lombok.AllArgsConstructor;
import org.ajbrown.namemachine.Name;
import org.ajbrown.namemachine.NameGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@AllArgsConstructor
public class DataGeneratorService {

    public static final int NUM_USERS = 1000;

    private final UsuarioRepository usuarioRepository;
    private final PerfilRepository perfilRepository;
    private final InformacionPublicaRepository informacionPublicaRepository;
    private final ConexionRepository conexionRepository;

    // Ejecuta todo el proceso secuencialmente manteniendo la consistencia
    @Transactional
    public void generateAllData() {
        List<Integer> createdUserIds = createUsers();
        createFollowUsers(createdUserIds);
        createPublicInfo(createdUserIds);
        createConnections(createdUserIds);
    }

    public List<Integer> createUsers() {
        NameGenerator generator = new NameGenerator();
        List<Name> names = generator.generateNames(NUM_USERS);

        List<Integer> createdUserIds = new ArrayList<>();
        Random rnd = new Random();
        Set<String> usedEmails = new HashSet<>();

        for (Name n : names) {
            Usuario user = new Usuario();
            user.setNombre(n.getFirstName());
            user.setApellidos(n.getLastName());

            String baseEmail = (n.getFirstName().toLowerCase() + "." + n.getLastName().toLowerCase())
                    .replaceAll("[^a-z0-9.]", "");
            String email = baseEmail + "@dlsi.ua.es";
            int suffix = 1;
            while (usedEmails.contains(email)) {
                email = baseEmail + suffix + "@dlsi.ua.es";
                suffix++;
            }
            usedEmails.add(email);
            user.setEmail(email);

            user.setPassword(Long.toHexString(Double.doubleToLongBits(Math.random())).getBytes());
            user.setApodo(null);

            short perfilId = (short) rnd.nextInt(3);
            Perfil perfil = perfilRepository.findById(perfilId)
                    .orElseThrow(() -> new IllegalStateException("Perfil no encontrado con ID: " + perfilId));
            user.setPerfil(perfil);
            user.setNacido(new Date(-946771200000L + (Math.abs(rnd.nextLong()) % (70L * 365 * 24 * 60 * 60 * 1000))));

            user = usuarioRepository.save(user);
            createdUserIds.add(user.getId());
        }

        return createdUserIds;
    }

    public void createPublicInfo(List<Integer> createdUserIds) {
        Random rnd = new Random();

        for (int idUser : createdUserIds) {
            Usuario curUser = usuarioRepository.findById(idUser)
                    .orElseThrow(() -> new IllegalStateException("Usuario no encontrado con ID: " + idUser));

            InformacionPublica info = new InformacionPublica();
            info.setUsuario(curUser);
            info.setId(curUser.getId());
            info.setMostrarEmail(rnd.nextBoolean());
            info.setMostrarNacido(rnd.nextBoolean());
            info.setMostrarNombre(rnd.nextBoolean());
            // Valoración aleatoria entre 0.0 y 10.0 redondeada a 1 decimal
            double valoracion = Math.round(rnd.nextDouble() * 100.0) / 10.0;
            info.setValoracion(valoracion);

            informacionPublicaRepository.save(info);
        }
    }

    public void createFollowUsers(List<Integer> createdUserIds) {
        if (createdUserIds.isEmpty()) {
            return;
        }

        Random rnd = new Random();
        int totalUsers = createdUserIds.size();

        for (int idUser : createdUserIds) {
            Usuario curUser = usuarioRepository.findById(idUser)
                    .orElseThrow(() -> new IllegalStateException("Usuario no encontrado con ID: " + idUser));

            // Cantidad controlada de seguidos (entre 3 y 10 usuarios)
            int followsCount = 3 + rnd.nextInt(8);
            Set<Usuario> seguidos = curUser.getSeguidos();
            if (seguidos == null) {
                seguidos = new HashSet<>();
                curUser.setSeguidos(seguidos);
            }

            int attempts = 0;
            while (seguidos.size() < followsCount && attempts < 50) {
                attempts++;
                int targetUserId = createdUserIds.get(rnd.nextInt(totalUsers));
                if (targetUserId != idUser) {
                    Usuario targetUser = usuarioRepository.getReferenceById(targetUserId);
                    seguidos.add(targetUser);
                }
            }

            usuarioRepository.save(curUser);
        }
    }

    public void createConnections(List<Integer> createdUserIds) {
        Random rnd = new Random();
        LocalDateTime now = LocalDateTime.now();

        for (int idUser : createdUserIds) {
            Usuario curUser = usuarioRepository.findById(idUser)
                    .orElseThrow(() -> new IllegalStateException("Usuario no encontrado con ID: " + idUser));

            // Cantidad controlada de conexiones por usuario (entre 1 y 5)
            int numConnections = 1 + rnd.nextInt(5);
            Set<LocalDateTime> usedMoments = new HashSet<>();

            for (int i = 0; i < numConnections; i++) {
                long minutesAgo = rnd.nextLong(365L * 24 * 60) + (i * 60L);
                LocalDateTime momento = now.minusMinutes(minutesAgo).withNano(0);

                if (usedMoments.add(momento)) {
                    ConexionId conexionId = new ConexionId(curUser.getId(), momento);
                    Conexion conexion = new Conexion(conexionId, curUser);
                    conexionRepository.save(conexion);
                }
            }
        }
    }
}
