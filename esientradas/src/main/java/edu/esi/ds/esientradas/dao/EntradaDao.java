package edu.esi.ds.esientradas.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import edu.esi.ds.esientradas.dto.DtoEntradas;
import edu.esi.ds.esientradas.model.Entrada;
import edu.esi.ds.esientradas.model.Estado;

public interface EntradaDao extends JpaRepository<Entrada, Long>{

    List<Entrada>   findByEspectaculoId(Long espectaculoId);

    List<Entrada> findByEspectaculoIdAndEstado(Long espectaculoId, Estado estado);

    @Query(value = "UPDATE Entrada e Set e.estado = :estado WHERE e.id = :idEntrada")
    @Modifying
    void updateEstado(@Param("idEntrada") Long idEntrada, @Param("estado") Estado reservada);

    Integer countByEspectaculoId(Long idEspectaculo);

    Integer countByEspectaculoIdAndEstado(Long idEspectaculo, Estado disponible);
    

    @Query(value= """
SELECT COUNT(*) AS total,
       SUM(estado = 'DISPONIBLE') AS libres,
       SUM(estado = 'RESERVADA') AS reservadas,
       SUM(estado = 'VENDIDA') AS vendidas
FROM entrada
WHERE espectaculo_id = :idEspectaculo
""", nativeQuery = true)
    Object getNumeroDeEntradasComoDto(@Param("idEspectaculo") Long idEspectaculo);

}
