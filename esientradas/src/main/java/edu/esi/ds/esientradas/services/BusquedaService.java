package edu.esi.ds.esientradas.services;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import edu.esi.ds.esientradas.dao.EntradaDao;
import edu.esi.ds.esientradas.dao.EscenarioDao;
import edu.esi.ds.esientradas.dao.EspectaculoDao;
import edu.esi.ds.esientradas.dto.DtoEntradas;
import edu.esi.ds.esientradas.model.Entrada;
import edu.esi.ds.esientradas.model.Escenario;
import edu.esi.ds.esientradas.model.Espectaculo;
import edu.esi.ds.esientradas.model.Estado;

@Service
public class BusquedaService {

    @Autowired
    private EscenarioDao dao;

    @Autowired
    private EntradaDao entradaDao;

    @Autowired
    private EspectaculoDao espectaculoDao;
    
    public List<Escenario> getEscenarios() {
        return this.dao.findAll();
    }

    public List<Espectaculo> getEspectaculos(String artista) {
        return this.espectaculoDao.findByArtista(artista);
    }

     public List<Espectaculo> getEspectaculos(Long idEscenario) {
         return this.espectaculoDao.findByEscenarioId(idEscenario);
    }

    public List<Entrada> getEntradas(Long espectaculoId) {
        return this.entradaDao.findByEspectaculoId(espectaculoId);
    }

    public Integer getNumeroDeEntradas(Long idEspectaculo) {
        return this.entradaDao.countByEspectaculoId(idEspectaculo);
    }

    public Integer getEntradasLibres(Long idEspectaculo) {
        return this.entradaDao.countByEspectaculoIdAndEstado(idEspectaculo, Estado.DISPONIBLE);
    }

    public DtoEntradas getNumeroDeEntradasComoDto(Long idEspectaculo) {
        Object result = this.entradaDao.getNumeroDeEntradasComoDto(idEspectaculo);
        Object[] row = (Object[]) result;
        DtoEntradas dto = new DtoEntradas(((Number) row[0]).intValue(), ((Number) row[1]).intValue(), ((Number) row[2]).intValue(), ((Number) row[3]).intValue()    );
        return dto;
    }
    

}
