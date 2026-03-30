package edu.esi.ds.esientradas.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import edu.esi.ds.esientradas.model.Configuration;

@Repository
public interface ConfigurationDao extends JpaRepository<Configuration, String>{

}