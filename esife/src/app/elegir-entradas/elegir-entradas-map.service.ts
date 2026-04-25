import { Injectable } from '@angular/core';
import { ButacaSvg, EntradaMapaDto, ZonaResumen } from './elegir-entradas.model';
import { ElegirEntradasLayoutService } from './elegir-entradas-layout.service';

@Injectable({
  providedIn: 'root'
})
export class ElegirEntradasMapService {
  constructor(private layoutService: ElegirEntradasLayoutService) {}

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

  buildVisualState(infoCompra: any, entradasMapa: EntradaMapaDto[]): { butacas: ButacaSvg[]; zonas: ZonaResumen[] } {
    if (infoCompra?.modoSeleccion === 'PRECISA') {
      return {
        butacas: this.buildButacasPrecision(infoCompra, entradasMapa),
        zonas: []
      };
    }

    return {
      butacas: [],
      zonas: this.buildZonas(entradasMapa)
    };
  }

  private buildButacasPrecision(infoCompra: any, entradasMapa: EntradaMapaDto[]): ButacaSvg[] {
    const butacas: ButacaSvg[] = [];
    const zonasEnModoPreciso = entradasMapa.filter(e =>
      e.zona != null && (e.planta == null || e.fila == null || e.columna == null)
    );

    const totalPorZona = new Map<number, number>();
    const indicePorZona = new Map<number, number>();

    for (const entrada of zonasEnModoPreciso) {
      const zona = entrada.zona as number;
      totalPorZona.set(zona, (totalPorZona.get(zona) ?? 0) + 1);
    }

    for (const entrada of entradasMapa) {
      if (entrada.fila != null && entrada.columna != null && entrada.planta != null) {
        const pos = this.getSeatPosition(infoCompra, entradasMapa, entrada);
        butacas.push({ ...entrada, x: pos.x, y: pos.y });
      } else if (entrada.zona != null) {
        const zona = entrada.zona;
        const indice = indicePorZona.get(zona) ?? 0;
        indicePorZona.set(zona, indice + 1);

        const pos = this.getZoneSeatPosition(infoCompra, zona, indice, totalPorZona.get(zona) ?? 1);
        butacas.push({ ...entrada, x: pos.x, y: pos.y });
      }
    }

    return butacas;
  }

  private getSeatPosition(infoCompra: any, entradasMapa: EntradaMapaDto[], entrada: EntradaMapaDto): { x: number; y: number } {
    const planta = entrada.planta ?? 0;
    const rango = this.getPlantRange(entradasMapa, planta);
    const fila = this.normalizeIndex(entrada.fila, rango.minFila);
    const columna = this.normalizeIndex(entrada.columna, rango.minColumna);
    const dims = this.getPlantDimensions(entradasMapa, planta);

    return this.layoutService.getSeatPosition(
      infoCompra?.tipoMapa,
      planta,
      fila,
      columna,
      dims.filas,
      dims.columnas
    );
  }

  private getZoneSeatPosition(infoCompra: any, zona: number, indice: number, total: number): { x: number; y: number } {
    const rect = this.layoutService.getZoneRect(infoCompra?.tipoMapa, zona);
    if (!rect) {
      return { x: 100, y: 100 };
    }

    const totalSeguro = Math.max(1, total);
    const proporcion = rect.width / Math.max(1, rect.height);
    const columnas = Math.max(1, Math.ceil(Math.sqrt(totalSeguro * proporcion)));
    const filas = Math.max(1, Math.ceil(totalSeguro / columnas));

    const fila = Math.floor(indice / columnas) + 1;
    const columna = (indice % columnas) + 1;

    return this.placeInGrid(rect.x, rect.y, rect.width, rect.height, fila, columna, filas, columnas, 16, 16);
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

  private getPlantDimensions(entradasMapa: EntradaMapaDto[], planta: number): { filas: number; columnas: number } {
    const rango = this.getPlantRange(entradasMapa, planta);

    return {
      filas: Math.max(1, rango.maxFila - rango.minFila + 1),
      columnas: Math.max(1, rango.maxColumna - rango.minColumna + 1)
    };
  }

  private getPlantRange(entradasMapa: EntradaMapaDto[], planta: number): {
    minFila: number;
    maxFila: number;
    minColumna: number;
    maxColumna: number;
  } {
    const entradasDePlanta = entradasMapa.filter(
      e => e.planta === planta && e.fila != null && e.columna != null
    );

    if (entradasDePlanta.length === 0) {
      return { minFila: 1, maxFila: 1, minColumna: 1, maxColumna: 1 };
    }

    let minFila = Number.POSITIVE_INFINITY;
    let maxFila = Number.NEGATIVE_INFINITY;
    let minColumna = Number.POSITIVE_INFINITY;
    let maxColumna = Number.NEGATIVE_INFINITY;

    for (const entrada of entradasDePlanta) {
      const fila = entrada.fila ?? 1;
      const columna = entrada.columna ?? 1;

      minFila = Math.min(minFila, fila);
      maxFila = Math.max(maxFila, fila);
      minColumna = Math.min(minColumna, columna);
      maxColumna = Math.max(maxColumna, columna);
    }

    return { minFila, maxFila, minColumna, maxColumna };
  }

  private normalizeIndex(valor: number | undefined, minimo: number): number {
    if (valor == null) {
      return 1;
    }

    return Math.max(1, valor - minimo + 1);
  }

  private placeInGrid(
    x: number,
    y: number,
    width: number,
    height: number,
    fila: number,
    columna: number,
    totalFilas: number,
    totalColumnas: number,
    marginX = 16,
    marginY = 16
  ): { x: number; y: number } {
    const usableW = width - marginX * 2;
    const usableH = height - marginY * 2;

    const pasoX = totalColumnas > 1 ? usableW / (totalColumnas - 1) : 0;
    const pasoY = totalFilas > 1 ? usableH / (totalFilas - 1) : 0;

    return {
      x: x + marginX + (columna - 1) * pasoX,
      y: y + marginY + (fila - 1) * pasoY
    };
  }
}