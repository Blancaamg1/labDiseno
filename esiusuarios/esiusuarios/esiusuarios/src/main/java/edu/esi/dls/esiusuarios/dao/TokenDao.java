package edu.esi.dls.esiusuarios.dao;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import edu.esi.dls.esiusuarios.model.Token;
import edu.esi.dls.esiusuarios.model.User;

@Repository
public interface TokenDao extends JpaRepository<Token, Long> {
    Optional<Token> findByValue(String value);
    Optional<Token> findByValueAndPurpose(String value, String purpose);
    List<Token> findByUserAndPurposeAndUsedAtIsNull(User user, String purpose);
}
