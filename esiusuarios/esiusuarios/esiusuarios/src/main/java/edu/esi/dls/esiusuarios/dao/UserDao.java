package edu.esi.dls.esiusuarios.dao;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import edu.esi.dls.esiusuarios.model.User;

@Repository
public interface UserDao extends JpaRepository<User, Long> {
    Optional<User> findByNameIgnoreCase(String name);
    Optional<User> findByEmailIgnoreCase(String email);
    Optional<User> findByToken(String token);
}
