package edu.esi.ds.esientradas.dao;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.esi.ds.esientradas.model.ColaVirtual;

public interface ColaVirtualDao extends JpaRepository<ColaVirtual, Long> {

    Optional<ColaVirtual> findByIdEspectaculoAndIdUsuarioAndEstadoIn(
            Long idEspectaculo,
            Long idUsuario,
            List<String> estados);

    List<ColaVirtual> findByIdEspectaculoAndEstadoOrderByFechaEntradaAsc(
            Long idEspectaculo,
            String estado);

    Optional<ColaVirtual> findByIdEspectaculoAndEstado(
            Long idEspectaculo,
            String estado);

    List<ColaVirtual> findByEstadoAndFechaFinTurnoBefore(
            String estado,
            LocalDateTime fecha);

    List<ColaVirtual> findByIdEspectaculoAndEstadoInOrderByFechaEntradaAsc(
            Long idEspectaculo,
            List<String> estados);
}