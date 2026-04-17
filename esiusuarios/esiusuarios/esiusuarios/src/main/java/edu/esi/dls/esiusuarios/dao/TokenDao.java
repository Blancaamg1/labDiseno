package edu.esi.dls.esiusuarios.dao;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import edu.esi.dls.esiusuarios.model.Token;

@Repository
public interface TokenDao extends JpaRepository<Token, Long> {
    Optional<Token> findByValue(String value);
}
