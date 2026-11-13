package com.bd.manageusers.repository;

import com.bd.manageusers.model.InformacionPublica;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InformacionPublicaRepository extends JpaRepository<InformacionPublica, Integer> {
}
