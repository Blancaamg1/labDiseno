package edu.esi.ds.esientradas.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import edu.esi.ds.esientradas.model.PDFEntrada;

@Repository
public interface PDFDao extends JpaRepository<PDFEntrada, Long>{

}