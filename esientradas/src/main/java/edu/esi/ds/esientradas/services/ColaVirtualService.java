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

    @Autowired
    private ColaVirtualDao colaVirtualDao;

    @Autowired
    private EspectaculoDao espectaculoDao;

    @Autowired
    private UsuarioService usuarioService;

    @Transactional
    public DtoColaEstado entrarEnCola(Long idEspectaculo) {
        Espectaculo espectaculo = this.espectaculoDao.findById(idEspectaculo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Espectáculo no encontrado"));

        if (espectaculo.getUsaColaVirtual() == null || !espectaculo.getUsaColaVirtual()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Este espectáculo no usa cola virtual");
        }

        this.actualizarCola(idEspectaculo);

        ColaVirtual nueva = new ColaVirtual();
        nueva.setIdEspectaculo(idEspectaculo);
        nueva.setIdUsuario(null);
        nueva.setFechaEntrada(LocalDateTime.now());
        nueva.setTokenTurno(UUID.randomUUID().toString());
        nueva.setFechaFinTurno(null);
        nueva.setEstado("ESPERANDO");
        nueva.setPersonasDelante(0);
        nueva.setPosicion(0);

        this.colaVirtualDao.save(nueva);
        
        // Al entrar, llamamos de nuevo a actualizarCola por si la cola estaba vacía y podemos activarlo.
        this.actualizarCola(idEspectaculo);
        
        ColaVirtual actualizada = this.colaVirtualDao.findById(nueva.getId()).orElse(nueva);
        return this.construirDto(actualizada, idEspectaculo);
    }

    @Transactional
    public DtoColaEstado consultarEstado(Long idEspectaculo, String tokenTurno) {
        this.actualizarCola(idEspectaculo);

        Optional<ColaVirtual> colaOpt = this.colaVirtualDao.findByTokenTurno(tokenTurno);

        if (!colaOpt.isPresent() || !Arrays.asList("ESPERANDO", "ACTIVO").contains(colaOpt.get().getEstado()) || !colaOpt.get().getIdEspectaculo().equals(idEspectaculo)) {
            DtoColaEstado dto = new DtoColaEstado();
            dto.setIdEspectaculo(idEspectaculo);
            dto.setIdUsuario(null);
            dto.setEstado("SIN_COLA");
            dto.setPosicion(null);
            dto.setPersonasDelante(null);
            dto.setPuedeComprar(false);
            dto.setTokenTurno(null);
            dto.setSegundosRestantes(0L);
            dto.setMensaje("Todavía no estás en la cola virtual.");
            return dto;
        }

        return this.construirDto(colaOpt.get(), idEspectaculo);
    }

    @Transactional
    public void salirDeCola(Long idEspectaculo, String tokenTurno) {
        Optional<ColaVirtual> colaOpt = this.colaVirtualDao.findByTokenTurno(tokenTurno);
        if (colaOpt.isPresent() && Arrays.asList("ESPERANDO", "ACTIVO").contains(colaOpt.get().getEstado()) && colaOpt.get().getIdEspectaculo().equals(idEspectaculo)) {
            ColaVirtual cola = colaOpt.get();
            cola.setEstado("FINALIZADO");
            cola.setFechaFinTurno(null);
            this.colaVirtualDao.save(cola);
        }

        this.actualizarCola(idEspectaculo);
    }

    private synchronized void actualizarCola(Long idEspectaculo) {
        List<ColaVirtual> activos = this.colaVirtualDao.findByEspectaculo_IdAndEstadoOrderByFechaEntradaAsc(
                idEspectaculo,
                "ACTIVO"
        );

        // Expirar activos si su turno ha terminado
        for (ColaVirtual activo : activos) {
            if (activo.getFechaFinTurno() != null && activo.getFechaFinTurno().isBefore(LocalDateTime.now())) {
                activo.setEstado("FINALIZADO");
                activo.setFechaFinTurno(null);
                this.colaVirtualDao.save(activo);
            }
        }

        List<ColaVirtual> activosDespues = this.colaVirtualDao.findByEspectaculo_IdAndEstadoOrderByFechaEntradaAsc(
                idEspectaculo,
                "ACTIVO"
        );

        // Si sigue habiendo alguien activo, no activamos a nadie más. (Cola estricta 1-a-1)
        if (!activosDespues.isEmpty()) {
            return;
        }

        List<ColaVirtual> esperando = this.colaVirtualDao.findByEspectaculo_IdAndEstadoOrderByFechaEntradaAsc(
                idEspectaculo,
                "ESPERANDO"
        );

        if (esperando.isEmpty()) {
            return;
        }

        // Si no hay nadie activo, verificamos si el primero que espera ya pasó el "tiempo de persona falsa"
        ColaVirtual primero = esperando.get(0);
        long segundosEsperados = Duration.between(primero.getFechaEntrada(), LocalDateTime.now()).getSeconds();

        if (segundosEsperados >= SEGUNDOS_POR_PERSONA) {
            primero.setEstado("ACTIVO");
            primero.setPosicion(1);
            primero.setPersonasDelante(0);
            primero.setFechaFinTurno(LocalDateTime.now().plusSeconds(DURACION_TURNO_ACTIVO));
            this.colaVirtualDao.save(primero);
        }
    }

    private DtoColaEstado construirDto(ColaVirtual cola, Long idEspectaculo) {
        DtoColaEstado dto = new DtoColaEstado();
        dto.setIdEspectaculo(idEspectaculo);
        dto.setIdUsuario(cola.getIdUsuario());
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
            dto.setPosicion(1);
            dto.setPersonasDelante(0);
        } else if ("ESPERANDO".equals(cola.getEstado())) {
            List<ColaVirtual> activos = this.colaVirtualDao.findByEspectaculo_IdAndEstadoOrderByFechaEntradaAsc(
                    idEspectaculo, "ACTIVO");
            List<ColaVirtual> esperando = this.colaVirtualDao.findByEspectaculo_IdAndEstadoOrderByFechaEntradaAsc(
                    idEspectaculo, "ESPERANDO");

            int activosCount = activos.isEmpty() ? 0 : 1;
            
            // Verificamos si existe la persona falsa conceptual
            boolean hayPersonaFalsa = false;
            if (activosCount == 0 && !esperando.isEmpty()) {
                ColaVirtual primero = esperando.get(0);
                long segs = Duration.between(primero.getFechaEntrada(), LocalDateTime.now()).getSeconds();
                if (segs < SEGUNDOS_POR_PERSONA) {
                    hayPersonaFalsa = true;
                }
            }

            int countEsperandoAntesQueYo = 0;
            for (ColaVirtual esp : esperando) {
                if (esp.getId().equals(cola.getId())) {
                    break;
                }
                countEsperandoAntesQueYo++;
            }

            int personasDelante = activosCount + countEsperandoAntesQueYo + (hayPersonaFalsa ? 1 : 0);
            
            dto.setPosicion(personasDelante + 1);
            dto.setPersonasDelante(personasDelante);
            
            // Segundos restantes aproximados (cada persona = 20s)
            long segundosAproximados = personasDelante * SEGUNDOS_POR_PERSONA;
            if (hayPersonaFalsa && activosCount == 0 && countEsperandoAntesQueYo == 0) {
                 // Si soy el primero y estoy esperando a la persona falsa, cuento los segundos que faltan
                 long segs = Duration.between(cola.getFechaEntrada(), LocalDateTime.now()).getSeconds();
                 segundosAproximados = Math.max(SEGUNDOS_POR_PERSONA - segs, 0);
            }
            
            dto.setSegundosRestantes(segundosAproximados);
            dto.setMensaje("Debes esperar tu turno");
        } else {
            dto.setSegundosRestantes(0L);
            dto.setMensaje("No estás en cola");
        }

        return dto;
    }
}