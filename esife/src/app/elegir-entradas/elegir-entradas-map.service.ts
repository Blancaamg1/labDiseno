import { Injectable } from '@angular/core';
import { EntradaMapaDto, ZonaResumen } from './elegir-entradas.model';

@Injectable({
  providedIn: 'root'
})
export class ElegirEntradasMapService {
  constructor() {}

  resolveVisualMode(info: any, entradas: EntradaMapaDto[]): any {
    const precisas = entradas.filter(e => e.planta != null && e.fila != null && e.columna != null).length;
    const zonas = entradas.filter(e => e.zona != null).length;
    const tipoMapa = info?.tipoMapa;
    const soportaZonaVisual = tipoMapa === 'ESTADIO_MUNICIPAL' || tipoMapa === 'PLAZA_ABIERTA';

    if (soportaZonaVisual && (info?.modoSeleccion === 'PRECISA' || !info?.modoSeleccion) && zonas > precisas) {
      return { ...info, modoSeleccion: 'ZONA' };
    }

    if (info?.modoSeleccion === 'ZONA' && precisas > 0 && zonas === 0) {
      return { ...info, modoSeleccion: 'PRECISA' };
    }

    return info;
  }

  filterEntradasReales(infoCompra: any, entradas: EntradaMapaDto[]): EntradaMapaDto[] {
    if (infoCompra?.modoSeleccion === 'PRECISA') {
      return entradas
        .filter(e =>
          (e.planta != null && e.fila != null && e.columna != null) ||
          e.zona != null
        )
        .sort((a, b) =>
          ((a.planta ?? Number.MAX_SAFE_INTEGER) - (b.planta ?? Number.MAX_SAFE_INTEGER)) ||
          ((a.fila ?? Number.MAX_SAFE_INTEGER) - (b.fila ?? Number.MAX_SAFE_INTEGER)) ||
          ((a.columna ?? Number.MAX_SAFE_INTEGER) - (b.columna ?? Number.MAX_SAFE_INTEGER)) ||
          ((a.zona ?? Number.MAX_SAFE_INTEGER) - (b.zona ?? Number.MAX_SAFE_INTEGER))
        );
    }

    return entradas
      .filter(e => e.zona != null)
      .sort((a, b) => (a.zona! - b.zona!));
  }

  buildVisualState(infoCompra: any, entradasMapa: EntradaMapaDto[]): { butacas: any[]; zonas: ZonaResumen[] } {
    if (infoCompra?.modoSeleccion === 'PRECISA') {
      return { butacas: [], zonas: [] };
    }

    return {
      butacas: [],
      zonas: this.buildZonas(entradasMapa)
    };
  }




  private buildZonas(entradasMapa: EntradaMapaDto[]): ZonaResumen[] {
    const acumulado: { [zona: number]: number } = {};

    for (const entrada of entradasMapa) {
      if (entrada.zona != null && entrada.disponible) {
        if (!acumulado[entrada.zona]) {
          acumulado[entrada.zona] = 0;
        }
        acumulado[entrada.zona]++;
      }
    }

    const zonas: ZonaResumen[] = [];
    for (const clave of Object.keys(acumulado)) {
      zonas.push({
        zona: Number(clave),
        disponibles: acumulado[Number(clave)]
      });
    }

    return zonas;
  }

  // --- LÓGICA DE FORMULARIO PRECISA ---
  getPlantasDisponibles(entradas: EntradaMapaDto[]): number[] {
    const plantas = new Set<number>();
    entradas.forEach(e => {
      if (e.planta !== undefined) plantas.add(e.planta);
    });
    return Array.from(plantas).sort((a, b) => a - b);
  }

  getFilasDisponibles(entradas: EntradaMapaDto[], planta: number): number[] {
    const filas = new Set<number>();
    entradas.forEach(e => {
      if (e.planta === planta && e.fila !== undefined) {
        filas.add(e.fila);
      }
    });
    return Array.from(filas).sort((a, b) => a - b);
  }

  getAsientosDisponibles(entradas: EntradaMapaDto[], planta: number, fila: number, idsSeleccionados: Set<number>): EntradaMapaDto[] {
    return entradas.filter(e => 
      e.planta === planta && 
      e.fila === fila && 
      (e.disponible || idsSeleccionados.has(e.idEntrada))
    ).sort((a, b) => (a.columna ?? 0) - (b.columna ?? 0));
  }

  getTodosAsientosFila(entradas: EntradaMapaDto[], planta: number, fila: number): EntradaMapaDto[] {
    return entradas.filter(e => 
      e.planta === planta && 
      e.fila === fila
    ).sort((a, b) => (a.columna ?? 0) - (b.columna ?? 0));
  }

  getDetallesSeleccion(entradas: EntradaMapaDto[], idsSeleccionados: Set<number>): EntradaMapaDto[] {
    return entradas.filter(e => idsSeleccionados.has(e.idEntrada));
  }
}