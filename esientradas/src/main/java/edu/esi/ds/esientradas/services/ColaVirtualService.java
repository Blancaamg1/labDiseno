package edu.esi.ds.esientradas.services;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import edu.esi.ds.esientradas.dao.ColaVirtualDao;
import edu.esi.ds.esientradas.dao.EspectaculoDao;
import edu.esi.ds.esientradas.dto.DtoColaEstado;
import edu.esi.ds.esientradas.dto.DtoUsuarioInfo;
import edu.esi.ds.esientradas.model.ColaVirtual;
import edu.esi.ds.esientradas.model.Espectaculo;
import jakarta.transaction.Transactional;

@Service
public class ColaVirtualService {

    private static final long SEGUNDOS_POR_PERSONA = 20L;
    private static final long DURACION_TURNO_ACTIVO = 300L;
    private static final int MIN_PERSONAS_FAKE = 2;
    private static final int MAX_PERSONAS_FAKE = 5;

    @Autowired
    private ColaVirtualDao colaVirtualDao;

    @Autowired
    private EspectaculoDao espectaculoDao;

    @Autowired
    private UsuarioService usuarioService;

    @Transactional
    public DtoColaEstado entrarEnCola(Long idEspectaculo, String userToken) {
        Espectaculo espectaculo = this.espectaculoDao.findById(idEspectaculo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Espectáculo no encontrado"));

        if (espectaculo.getUsaColaVirtual() == null || !espectaculo.getUsaColaVirtual()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Este espectáculo no usa cola virtual");
        }

        DtoUsuarioInfo usuario = this.usuarioService.getUserInfo(userToken);

        if (usuario == null || usuario.getId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no identificado");
        }

        this.actualizarCola(idEspectaculo);

        Optional<ColaVirtual> existente = this.colaVirtualDao.findByIdEspectaculoAndIdUsuarioAndEstadoIn(
                idEspectaculo,
                usuario.getId(),
                Arrays.asList("ESPERANDO", "ACTIVO")
        );

        if (existente.isPresent()) {
            return this.construirDto(existente.get(), idEspectaculo);
        }

        List<ColaVirtual> colaActual = this.colaVirtualDao.findByIdEspectaculoAndEstadoInOrderByFechaEntradaAsc(
                idEspectaculo,
                Arrays.asList("ESPERANDO", "ACTIVO")
        );

        ColaVirtual nueva = new ColaVirtual();
        nueva.setIdEspectaculo(idEspectaculo);
        nueva.setIdUsuario(usuario.getId());
        nueva.setFechaEntrada(LocalDateTime.now());
        nueva.setTokenTurno(UUID.randomUUID().toString());
        nueva.setFechaFinTurno(null);
        nueva.setEstado("ESPERANDO");

        int personasDelante;

        if (colaActual.isEmpty()) {
            personasDelante = MIN_PERSONAS_FAKE + (int) (Math.random() * (MAX_PERSONAS_FAKE - MIN_PERSONAS_FAKE + 1));
        } else {
            ColaVirtual ultimo = colaActual.get(colaActual.size() - 1);
            int delanteUltimo = ultimo.getPersonasDelante() == null ? 0 : ultimo.getPersonasDelante();
            personasDelante = delanteUltimo + 1;
        }

        nueva.setPersonasDelante(personasDelante);
        nueva.setPosicion(personasDelante + 1);

        this.colaVirtualDao.save(nueva);
        return this.construirDto(nueva, idEspectaculo);
    }

    @Transactional
    public DtoColaEstado consultarEstado(Long idEspectaculo, String userToken) {
        DtoUsuarioInfo usuario = this.usuarioService.getUserInfo(userToken);

        if (usuario == null || usuario.getId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no identificado");
        }

        this.actualizarCola(idEspectaculo);

        Optional<ColaVirtual> colaOpt = this.colaVirtualDao.findByIdEspectaculoAndIdUsuarioAndEstadoIn(
                idEspectaculo,
                usuario.getId(),
                Arrays.asList("ESPERANDO", "ACTIVO")
        );

        if (colaOpt.isEmpty()) {
            DtoColaEstado dto = new DtoColaEstado();
            dto.setIdEspectaculo(idEspectaculo);
            dto.setIdUsuario(usuario.getId());
            dto.setEstado("SIN_COLA");
            dto.setPosicion(null);
            dto.setPersonasDelante(null);
            dto.setPuedeComprar(false);
            dto.setTokenTurno(null);
            dto.setSegundosRestantes(0L);
            dto.setMensaje("Todavía no estás en la cola virtual.");
            return dto;
        }

        ColaVirtual cola = colaOpt.get();

        if ("ESPERANDO".equals(cola.getEstado())) {
            long segundosTranscurridos = Duration.between(cola.getFechaEntrada(), LocalDateTime.now()).getSeconds();
            long bloquesConsumidos = segundosTranscurridos / SEGUNDOS_POR_PERSONA;

            int personasIniciales = cola.getPersonasDelante() == null ? 0 : cola.getPersonasDelante();
            int personasRestantes = (int) Math.max(personasIniciales - bloquesConsumidos, 0);

            cola.setPersonasDelante(personasRestantes);
            cola.setPosicion(personasRestantes + 1);

            if (personasRestantes <= 0) {
                List<ColaVirtual> activos = this.colaVirtualDao.findByIdEspectaculoAndEstadoOrderByFechaEntradaAsc(
                        idEspectaculo,
                        "ACTIVO"
                );

                if (activos.isEmpty()) {
                    cola.setEstado("ACTIVO");
                    cola.setPosicion(1);
                    cola.setPersonasDelante(0);
                    cola.setFechaFinTurno(LocalDateTime.now().plusSeconds(DURACION_TURNO_ACTIVO));
                }
            }

            this.colaVirtualDao.save(cola);
        }

        return this.construirDto(cola, idEspectaculo);
    }

    @Transactional
    public void salirDeCola(Long idEspectaculo, String userToken) {
        DtoUsuarioInfo usuario = this.usuarioService.getUserInfo(userToken);

        if (usuario == null || usuario.getId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no identificado");
        }

        Optional<ColaVirtual> colaOpt = this.colaVirtualDao.findByIdEspectaculoAndIdUsuarioAndEstadoIn(
                idEspectaculo,
                usuario.getId(),
                Arrays.asList("ESPERANDO", "ACTIVO")
        );

        if (colaOpt.isPresent()) {
            ColaVirtual cola = colaOpt.get();
            cola.setEstado("FINALIZADO");
            cola.setFechaFinTurno(null);
            this.colaVirtualDao.save(cola);
        }

        this.actualizarCola(idEspectaculo);
    }

    private void actualizarCola(Long idEspectaculo) {
        List<ColaVirtual> activos = this.colaVirtualDao.findByIdEspectaculoAndEstadoOrderByFechaEntradaAsc(
                idEspectaculo,
                "ACTIVO"
        );

        for (ColaVirtual activo : activos) {
            if (activo.getFechaFinTurno() != null && activo.getFechaFinTurno().isBefore(LocalDateTime.now())) {
                activo.setEstado("FINALIZADO");
                activo.setFechaFinTurno(null);
                this.colaVirtualDao.save(activo);
            }
        }

        List<ColaVirtual> activosDespues = this.colaVirtualDao.findByIdEspectaculoAndEstadoOrderByFechaEntradaAsc(
                idEspectaculo,
                "ACTIVO"
        );

        if (!activosDespues.isEmpty()) {
            return;
        }

        List<ColaVirtual> esperando = this.colaVirtualDao.findByIdEspectaculoAndEstadoOrderByFechaEntradaAsc(
                idEspectaculo,
                "ESPERANDO"
        );

        for (ColaVirtual cola : esperando) {
            long segundosTranscurridos = Duration.between(cola.getFechaEntrada(), LocalDateTime.now()).getSeconds();
            long bloquesConsumidos = segundosTranscurridos / SEGUNDOS_POR_PERSONA;

            int personasIniciales = cola.getPersonasDelante() == null ? 0 : cola.getPersonasDelante();
            int personasRestantes = (int) Math.max(personasIniciales - bloquesConsumidos, 0);

            cola.setPersonasDelante(personasRestantes);
            cola.setPosicion(personasRestantes + 1);

            if (personasRestantes <= 0) {
                cola.setEstado("ACTIVO");
                cola.setPosicion(1);
                cola.setPersonasDelante(0);
                cola.setFechaFinTurno(LocalDateTime.now().plusSeconds(DURACION_TURNO_ACTIVO));
                this.colaVirtualDao.save(cola);
                return;
            }

            this.colaVirtualDao.save(cola);
        }
    }

    private DtoColaEstado construirDto(ColaVirtual cola, Long idEspectaculo) {
        DtoColaEstado dto = new DtoColaEstado();
        dto.setIdEspectaculo(idEspectaculo);
        dto.setIdUsuario(cola.getIdUsuario());
        dto.setPosicion(cola.getPosicion());
        dto.setPersonasDelante(cola.getPersonasDelante());
        dto.setEstado(cola.getEstado());
        dto.setTokenTurno(cola.getTokenTurno());

        boolean puedeComprar = "ACTIVO".equals(cola.getEstado())
                && cola.getFechaFinTurno() != null
                && cola.getFechaFinTurno().isAfter(LocalDateTime.now());

        dto.setPuedeComprar(puedeComprar);

        if ("ACTIVO".equals(cola.getEstado()) && cola.getFechaFinTurno() != null) {
            long segundos = Duration.between(LocalDateTime.now(), cola.getFechaFinTurno()).getSeconds();
            dto.setSegundosRestantes(Math.max(segundos, 0));
            dto.setMensaje(puedeComprar ? "Ya puedes comprar entradas" : "Tu turno ha caducado");
        } else if ("ESPERANDO".equals(cola.getEstado())) {
            long segundos = ((cola.getPersonasDelante() == null ? 0 : cola.getPersonasDelante()) + 1) * SEGUNDOS_POR_PERSONA;
            dto.setSegundosRestantes(segundos);
            dto.setMensaje("Debes esperar tu turno");
        } else {
            dto.setSegundosRestantes(0L);
            dto.setMensaje("No estás en cola");
        }

        return dto;
    }
}