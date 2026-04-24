package edu.esi.ds.esientradas.services;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.scheduling.annotation.Scheduled;

import edu.esi.ds.esientradas.dao.ColaVirtualDao;
import edu.esi.ds.esientradas.dao.EntradaDao;
import edu.esi.ds.esientradas.dao.EspectaculoDao;
import edu.esi.ds.esientradas.dao.TokenDao;
import edu.esi.ds.esientradas.dto.DtoCompraInfo;
import edu.esi.ds.esientradas.dto.DtoEntradaMapa;
import edu.esi.ds.esientradas.dto.DtoUsuarioInfo;
import edu.esi.ds.esientradas.model.ColaVirtual;
import edu.esi.ds.esientradas.model.DeZona;
import edu.esi.ds.esientradas.model.Entrada;
import edu.esi.ds.esientradas.model.Escenario;
import edu.esi.ds.esientradas.model.Espectaculo;
import edu.esi.ds.esientradas.model.Estado;
import edu.esi.ds.esientradas.model.Precisa;
import edu.esi.ds.esientradas.model.Token;
import jakarta.transaction.Transactional;

@Service
public class ReservasService {

    @Autowired
    private EspectaculoDao espectaculoDao;

    @Autowired
    private EntradaDao entradaDao;

    @Autowired
    private TokenDao tokenDao;

    @Autowired
    private ColaVirtualDao colaVirtualDao;

    @Autowired
    private UsuarioService usuarioService;

    @Transactional
    public Long reservar(Long idEntrada, String sessionId, String userToken) {
        Entrada entrada = this.entradaDao.findById(idEntrada)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Entrada no encontrada"));

        this.validarAccesoPorCola(entrada, userToken);

        if (entrada.getEstado() != Estado.DISPONIBLE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Entrada no disponible");
        }

        Token token = new Token();
        token.setEntrada(entrada);
        token.setSessionId(sessionId);
        this.tokenDao.save(token);

        this.entradaDao.updateEstado(idEntrada, Estado.RESERVADA);
        return entrada.getPrecio();
    }

    @Transactional
    public void liberar(Long idEntrada, String userToken) {
        Entrada entrada = this.entradaDao.findById(idEntrada).orElse(null);
        if (entrada != null && entrada.getEstado() == Estado.RESERVADA) {
            entrada.setEstado(Estado.DISPONIBLE);
            Token token = entrada.getToken();
            if (token != null) {
                entrada.setToken(null);
                this.tokenDao.delete(token);
            }
        }
    }

    private void validarAccesoPorCola(Entrada entrada, String userToken) {
        Espectaculo espectaculo = entrada.getEspectaculo();

        if (espectaculo == null) {
            return;
        }

        if (espectaculo.getUsaColaVirtual() == null || !espectaculo.getUsaColaVirtual()) {
            return;
        }

        if (userToken == null || userToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Se requiere token de usuario");
        }

        DtoUsuarioInfo usuario = this.usuarioService.getUserInfo(userToken);

        if (usuario == null || usuario.getId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no identificado");
        }

        Optional<ColaVirtual> colaOpt = this.colaVirtualDao.findByIdEspectaculoAndIdUsuarioAndEstadoIn(
                espectaculo.getId(),
                usuario.getId(),
                Arrays.asList("ACTIVO", "ESPERANDO")
        );

        if (colaOpt.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "No estas en la cola virtual de este espectaculo"
            );
        }

        ColaVirtual cola = colaOpt.get();

        if (!"ACTIVO".equals(cola.getEstado())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Aun no ha llegado tu turno para comprar"
            );
        }

        if (cola.getFechaFinTurno() == null || cola.getFechaFinTurno().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Tu turno de compra ha caducado"
            );
        }
    }

    private String resolverTipoMapa(Escenario escenario) {
        if (escenario.getTipo() != null) {
            return escenario.getTipo().toString();
        }

        String nombre = escenario.getNombre() == null ? "" : escenario.getNombre().trim();

        if (nombre.equalsIgnoreCase("Auditorio Principal")) {
            return "AUDITORIO_PRINCIPAL";
        }
        if (nombre.equalsIgnoreCase("Teatro Clásico") || nombre.equalsIgnoreCase("Teatro Clasico")) {
            return "TEATRO_CLASICO";
        }
        if (nombre.equalsIgnoreCase("Sala Experimental")) {
            return "SALA_EXPERIMENTAL";
        }
        if (nombre.equalsIgnoreCase("Estadio Municipal")) {
            return "ESTADIO_MUNICIPAL";
        }
        if (nombre.equalsIgnoreCase("Plaza Abierta")) {
            return "PLAZA_ABIERTA";
        }

        return "SIN_MAPA";
    }

    public DtoCompraInfo obtenerInfoCompra(Long idEspectaculo) {
        Espectaculo espectaculo = this.espectaculoDao.findById(idEspectaculo)
                .orElseThrow(() -> new RuntimeException("No existe el espectáculo con id " + idEspectaculo));

        Escenario escenario = espectaculo.getEscenario();

        boolean hayPrecisas = false;
        boolean hayZonas = false;

        for (Entrada entrada : espectaculo.getEntradas()) {
            if (entrada instanceof Precisa) {
                hayPrecisas = true;
            }
            if (entrada instanceof DeZona) {
                hayZonas = true;
            }
        }

        String modoSeleccion;
        if (hayPrecisas && !hayZonas) {
            modoSeleccion = "PRECISA";
        } else if (hayZonas && !hayPrecisas) {
            modoSeleccion = "ZONA";
        } else {
            String tipoMapa = this.resolverTipoMapa(escenario);
            if ("ESTADIO_MUNICIPAL".equals(tipoMapa) || "PLAZA_ABIERTA".equals(tipoMapa)) {
                modoSeleccion = "ZONA";
            } else {
                modoSeleccion = "PRECISA";
            }
        }

        DtoCompraInfo dto = new DtoCompraInfo();
        dto.setIdEspectaculo(espectaculo.getId());
        dto.setNombreEspectaculo(espectaculo.getArtista());
        dto.setIdEscenario(escenario.getId());
        dto.setNombreEscenario(escenario.getNombre());
        dto.setTipoMapa(this.resolverTipoMapa(escenario));
        dto.setModoSeleccion(modoSeleccion);
        dto.setUsaColaVirtual(espectaculo.getUsaColaVirtual());
        dto.setFechaAperturaCola(espectaculo.getFechaAperturaCola());

        return dto;
    }

    @Transactional
    public List<DtoEntradaMapa> obtenerEntradasMapa(Long idEspectaculo) {
        Espectaculo espectaculo = this.espectaculoDao.findById(idEspectaculo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Espectáculo no encontrado"));

        List<Entrada> entradas = this.entradaDao.findByEspectaculoId(espectaculo.getId());
        List<DtoEntradaMapa> resultado = new ArrayList<DtoEntradaMapa>();

        for (Entrada entrada : entradas) {
            DtoEntradaMapa dto = new DtoEntradaMapa();
            dto.setIdEntrada(entrada.getId());
            dto.setDisponible(entrada.getEstado() == Estado.DISPONIBLE);

            if (entrada instanceof Precisa) {
                Precisa p = (Precisa) entrada;
                dto.setFila(p.getFila());
                dto.setColumna(p.getColumna());
                dto.setPlanta(p.getPlanta());
            } else if (entrada instanceof DeZona) {
                DeZona z = (DeZona) entrada;
                dto.setZona(z.getZona());
            }

            resultado.add(dto);
        }

        return resultado;
    }

    @Scheduled(fixedDelay = 10000)
    @Transactional
    public void liberarEntradasExpiradas() {
        Long tiempoExpiracion = System.currentTimeMillis() - 300000;
        List<Token> tokensExpirados = this.tokenDao.findByHoraLessThan(tiempoExpiracion);
        
        for (Token token : tokensExpirados) {
            Entrada entrada = token.getEntrada();
            if (entrada != null) {
                if (entrada.getEstado() == Estado.RESERVADA) {
                    entrada.setEstado(Estado.DISPONIBLE);
                }
                entrada.setToken(null);
            }
            this.tokenDao.delete(token);
        }
    }
}