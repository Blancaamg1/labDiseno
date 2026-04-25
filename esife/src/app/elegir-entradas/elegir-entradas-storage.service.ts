import { Injectable } from '@angular/core';

type SeleccionPersistida = {
  idsSeleccionados: number[];
  zonaSeleccionada: number | null;
  idEntradaZonaReservada: number | null;
};

@Injectable({
  providedIn: 'root'
})
export class ElegirEntradasStorageService {
  private readonly reservaExpiracionKey = 'reservaExpiracion';
  private readonly entradasSeleccionadasKey = 'entradasSeleccionadas';
  private readonly zonaSeleccionadaKey = 'zonaSeleccionada';
  private readonly idEntradaZonaReservadaKey = 'idEntradaZonaReservada';

  isAvailable(): boolean {
    return typeof localStorage !== 'undefined';
  }

  getAuthToken(): string {
    if (!this.isAvailable()) {
      return '';
    }
    return localStorage.getItem('authToken') || '';
  }

  getReservaExpiracion(): number | null {
    if (!this.isAvailable()) {
      return null;
    }

    const exp = localStorage.getItem(this.reservaExpiracionKey);
    if (!exp) {
      return null;
    }

    const parsed = parseInt(exp, 10);
    return Number.isNaN(parsed) ? null : parsed;
  }

  setReservaExpiracion(expiracionMs: number): void {
    if (!this.isAvailable()) {
      return;
    }

    localStorage.setItem(this.reservaExpiracionKey, expiracionMs.toString());
  }

  clearReservaExpiracion(): void {
    if (!this.isAvailable()) {
      return;
    }

    localStorage.removeItem(this.reservaExpiracionKey);
  }

  saveSelectionState(idsSeleccionados: Set<number>, zonaSeleccionada: number | null, idEntradaZonaReservada: number | null): void {
    if (!this.isAvailable()) {
      return;
    }

    localStorage.setItem(this.entradasSeleccionadasKey, JSON.stringify(Array.from(idsSeleccionados)));

    if (zonaSeleccionada != null) {
      localStorage.setItem(this.zonaSeleccionadaKey, zonaSeleccionada.toString());
    } else {
      localStorage.removeItem(this.zonaSeleccionadaKey);
    }

    if (idEntradaZonaReservada != null) {
      localStorage.setItem(this.idEntradaZonaReservadaKey, idEntradaZonaReservada.toString());
    } else {
      localStorage.removeItem(this.idEntradaZonaReservadaKey);
    }
  }

  loadSelectionState(): SeleccionPersistida {
    if (!this.isAvailable()) {
      return {
        idsSeleccionados: [],
        zonaSeleccionada: null,
        idEntradaZonaReservada: null
      };
    }

    const idsRaw = localStorage.getItem(this.entradasSeleccionadasKey);
    const idsSeleccionados = idsRaw ? (JSON.parse(idsRaw) as number[]) : [];

    const zonaRaw = localStorage.getItem(this.zonaSeleccionadaKey);
    const zonaSeleccionada = zonaRaw ? parseInt(zonaRaw, 10) : null;

    const idZonaRaw = localStorage.getItem(this.idEntradaZonaReservadaKey);
    const idEntradaZonaReservada = idZonaRaw ? parseInt(idZonaRaw, 10) : null;

    return {
      idsSeleccionados,
      zonaSeleccionada,
      idEntradaZonaReservada
    };
  }

  clearSelectionState(): void {
    if (!this.isAvailable()) {
      return;
    }

    localStorage.removeItem(this.entradasSeleccionadasKey);
    localStorage.removeItem(this.zonaSeleccionadaKey);
    localStorage.removeItem(this.idEntradaZonaReservadaKey);
  }

  clearAllReservationState(): void {
    this.clearReservaExpiracion();
    this.clearSelectionState();
  }
}