package edu.esi.ds.esientradas.dao;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.esi.ds.esientradas.model.ColaVirtual;

public interface ColaVirtualDao extends JpaRepository<ColaVirtual, Long> {

    List<ColaVirtual> findByEspectaculo_IdAndIdUsuarioAndEstadoIn(
            Long idEspectaculo,
            Long idUsuario,
            List<String> estados);

    List<ColaVirtual> findByEspectaculo_IdAndEstadoOrderByFechaEntradaAsc(
            Long idEspectaculo,
            String estado);

    Optional<ColaVirtual> findByEspectaculo_IdAndEstado(
            Long idEspectaculo,
            String estado);

    Optional<ColaVirtual> findByTokenTurno(String tokenTurno);

    List<ColaVirtual> findByEstadoAndFechaFinTurnoBefore(
            String estado,
            LocalDateTime fecha);

        List<ColaVirtual> findByEspectaculo_IdAndEstadoInOrderByFechaEntradaAsc(
            Long idEspectaculo,
            List<String> estados);
}