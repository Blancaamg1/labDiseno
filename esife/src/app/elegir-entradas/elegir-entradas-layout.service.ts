import { Injectable } from '@angular/core';

type Rect = {
  x: number;
  y: number;
  width: number;
  height: number;
  marginX: number;
  marginY: number;
};

type SeatLayoutStrategy = {
  getSeatPosition: (
    planta: number,
    fila: number,
    columna: number,
    totalFilas: number,
    totalColumnas: number
  ) => { x: number; y: number };
  getZoneRect: (zona: number) => Rect | null;
};

@Injectable({
  providedIn: 'root'
})
export class ElegirEntradasLayoutService {
  private readonly strategies: Record<string, SeatLayoutStrategy> = {
    AUDITORIO_PRINCIPAL: {
      getSeatPosition: (planta, fila, columna, totalFilas, totalColumnas) => {
        const rect = this.getAuditorioSeatRect(planta);
        return this.placeInGrid(rect, fila, columna, totalFilas, totalColumnas);
      },
      getZoneRect: (zona) => {
        if (zona === 1) return { x: 70, y: 150, width: 240, height: 260, marginX: 16, marginY: 16 };
        if (zona === 2) return { x: 330, y: 150, width: 240, height: 260, marginX: 16, marginY: 16 };
        if (zona === 3) return { x: 590, y: 150, width: 240, height: 260, marginX: 16, marginY: 16 };
        return { x: 220, y: 470, width: 460, height: 85, marginX: 16, marginY: 16 };
      }
    },
    TEATRO_CLASICO: {
      getSeatPosition: (planta, fila, columna, totalFilas, totalColumnas) => {
        const rect = this.getTeatroSeatRect(planta);
        return this.placeInGrid(rect, fila, columna, totalFilas, totalColumnas);
      },
      getZoneRect: (zona) => {
        if (zona === 1) return { x: 120, y: 160, width: 660, height: 245, marginX: 16, marginY: 16 };
        if (zona === 2) return { x: 170, y: 455, width: 560, height: 72, marginX: 16, marginY: 16 };
        if (zona === 3) return { x: 55, y: 180, width: 75, height: 120, marginX: 16, marginY: 16 };
        return { x: 770, y: 180, width: 75, height: 120, marginX: 16, marginY: 16 };
      }
    },
    SALA_EXPERIMENTAL: {
      getSeatPosition: (_planta, fila, columna, totalFilas, totalColumnas) => {
        const columnasTotales = Math.max(1, totalColumnas);
        const columnasPorLadoIzquierdo = Math.max(1, Math.ceil(columnasTotales / 2));
        const columnasPorLadoDerecho = Math.max(1, columnasTotales - columnasPorLadoIzquierdo);

        if (columna <= columnasPorLadoIzquierdo) {
          return this.placeInGrid(
            { x: 195, y: 195, width: 190, height: 170, marginX: 0, marginY: 0 },
            fila,
            columna,
            totalFilas,
            columnasPorLadoIzquierdo
          );
        }

        return this.placeInGrid(
          { x: 515, y: 195, width: 190, height: 170, marginX: 0, marginY: 0 },
          fila,
          columna - columnasPorLadoIzquierdo,
          totalFilas,
          columnasPorLadoDerecho
        );
      },
      getZoneRect: (zona) => {
        if (zona === 1) return { x: 170, y: 175, width: 240, height: 210, marginX: 16, marginY: 16 };
        return { x: 490, y: 175, width: 240, height: 210, marginX: 16, marginY: 16 };
      }
    }
  };

  getSeatPosition(
    tipoMapa: string,
    planta: number,
    fila: number,
    columna: number,
    totalFilas: number,
    totalColumnas: number
  ): { x: number; y: number } {
    const strategy = this.strategies[tipoMapa];
    if (!strategy) {
      return { x: 100, y: 100 };
    }

    return strategy.getSeatPosition(planta, fila, columna, totalFilas, totalColumnas);
  }

  getZoneRect(tipoMapa: string, zona: number): { x: number; y: number; width: number; height: number } | null {
    const strategy = this.strategies[tipoMapa];
    const rect = strategy?.getZoneRect(zona) ?? null;
    if (!rect) {
      return null;
    }

    return {
      x: rect.x,
      y: rect.y,
      width: rect.width,
      height: rect.height
    };
  }

  private getAuditorioSeatRect(planta: number): Rect {
    if (planta === 0) return { x: 70, y: 150, width: 240, height: 260, marginX: 26, marginY: 24 };
    if (planta === 1) return { x: 330, y: 150, width: 240, height: 260, marginX: 26, marginY: 24 };
    if (planta === 2) return { x: 590, y: 150, width: 240, height: 260, marginX: 26, marginY: 24 };

    return { x: 220, y: 470, width: 460, height: 85, marginX: 28, marginY: 18 };
  }

  private getTeatroSeatRect(planta: number): Rect {
    if (planta === 1) return { x: 55, y: 180, width: 75, height: 120, marginX: 16, marginY: 16 };
    if (planta === 2) return { x: 770, y: 180, width: 75, height: 120, marginX: 16, marginY: 16 };
    if (planta === 3) return { x: 120, y: 160, width: 660, height: 245, marginX: 34, marginY: 26 };

    return { x: 170, y: 455, width: 560, height: 72, marginX: 28, marginY: 18 };
  }

  private placeInGrid(
    rect: Rect,
    fila: number,
    columna: number,
    totalFilas: number,
    totalColumnas: number
  ): { x: number; y: number } {
    const usableW = rect.width - rect.marginX * 2;
    const usableH = rect.height - rect.marginY * 2;

    const pasoX = totalColumnas > 1 ? usableW / (totalColumnas - 1) : 0;
    const pasoY = totalFilas > 1 ? usableH / (totalFilas - 1) : 0;

    return {
      x: rect.x + rect.marginX + (columna - 1) * pasoX,
      y: rect.y + rect.marginY + (fila - 1) * pasoY
    };
  }
}