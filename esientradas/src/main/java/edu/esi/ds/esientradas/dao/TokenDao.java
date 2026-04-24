package edu.esi.ds.esientradas.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import edu.esi.ds.esientradas.model.Token;

import java.util.List;

public interface TokenDao extends JpaRepository<Token, String>{

    List<Token> findByHoraLessThan(Long hora);
}
