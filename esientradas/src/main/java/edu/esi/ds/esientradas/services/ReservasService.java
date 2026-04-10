package edu.esi.ds.esientradas.services;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import edu.esi.ds.esientradas.dao.EntradaDao;
import edu.esi.ds.esientradas.dao.EspectaculoDao;
import edu.esi.ds.esientradas.dao.TokenDao;
import edu.esi.ds.esientradas.dto.DtoCompraInfo;
import edu.esi.ds.esientradas.dto.DtoEntradaMapa;
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

    @Transactional
    public Long reservar(Long idEntrada, String sessionId) {
        {
            Entrada entrada = this.entradaDao.findById(idEntrada)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Entrada no encontrada"));

            if (entrada.getEstado() != Estado.DISPONIBLE) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Entrada no disponible");
            }
            // entrada.setEstado(Estado.RESERVADA);
            // this.entradaDao.save(entrada);

            Token token = new Token();
            token.setEntrada(entrada);
            token.setSessionId(sessionId);
            this.tokenDao.save(token);

            this.entradaDao.updateEstado(idEntrada, Estado.RESERVADA);
            return entrada.getPrecio();
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
        Espectaculo espectaculo = espectaculoDao.findById(idEspectaculo)
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
            String tipoMapa = resolverTipoMapa(escenario);
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
        dto.setTipoMapa(resolverTipoMapa(escenario));
        dto.setModoSeleccion(modoSeleccion);

        return dto;
    }

    @Transactional
    public List<DtoEntradaMapa> obtenerEntradasMapa(Long idEspectaculo) {
        Espectaculo espectaculo = espectaculoDao.findById(idEspectaculo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Espectáculo no encontrado"));

        List<Entrada> entradas = entradaDao.findByEspectaculoId(espectaculo.getId());
        // ajusta este método al nombre real de tu DAO

        List<DtoEntradaMapa> resultado = new ArrayList<DtoEntradaMapa>();

        for (Entrada entrada : entradas) {
            DtoEntradaMapa dto = new DtoEntradaMapa();
            dto.setIdEntrada(entrada.getId());

            // CAMBIA "LIBRE" por el estado real que uses en tu proyecto
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
}
