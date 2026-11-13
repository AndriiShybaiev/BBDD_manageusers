package com.bd.manageusers.repository;

import com.bd.manageusers.model.Conexion;
import com.bd.manageusers.model.ConexionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConexionRepository extends JpaRepository<Conexion, ConexionId> {
}
