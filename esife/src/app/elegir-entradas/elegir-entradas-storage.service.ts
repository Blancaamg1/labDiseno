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
  private readonly reservaExpiracionKeyPrefix = 'reservaExpiracion';
  private readonly entradasSeleccionadasKeyPrefix = 'entradasSeleccionadas';
  private readonly zonaSeleccionadaKeyPrefix = 'zonaSeleccionada';
  private readonly idEntradaZonaReservadaKeyPrefix = 'idEntradaZonaReservada';
  private readonly tokenReservaKeyPrefix = 'tokenReserva';

  isAvailable(): boolean {
    return typeof localStorage !== 'undefined';
  }

  getAuthToken(): string {
    if (!this.isAvailable()) {
      return '';
    }
    return localStorage.getItem('authToken') || '';
  }

  private buildKey(prefix: string, idEspectaculo?: number | null): string {
    return idEspectaculo == null ? prefix : `${prefix}:${idEspectaculo}`;
  }

  getReservaExpiracion(idEspectaculo?: number | null): number | null {
    if (!this.isAvailable()) {
      return null;
    }

    const exp = localStorage.getItem(this.buildKey(this.reservaExpiracionKeyPrefix, idEspectaculo));
    if (!exp) {
      return null;
    }

    const parsed = parseInt(exp, 10);
    return Number.isNaN(parsed) ? null : parsed;
  }

  setReservaExpiracion(expiracionMs: number, idEspectaculo?: number | null): void {
    if (!this.isAvailable()) {
      return;
    }

    localStorage.setItem(this.buildKey(this.reservaExpiracionKeyPrefix, idEspectaculo), expiracionMs.toString());
  }

  clearReservaExpiracion(idEspectaculo?: number | null): void {
    if (!this.isAvailable()) {
      return;
    }

    localStorage.removeItem(this.buildKey(this.reservaExpiracionKeyPrefix, idEspectaculo));
  }

  saveSelectionState(idsSeleccionados: Set<number>, idEspectaculo?: number | null): void {
    if (!this.isAvailable()) {
      return;
    }

    localStorage.setItem(this.buildKey(this.entradasSeleccionadasKeyPrefix, idEspectaculo), JSON.stringify(Array.from(idsSeleccionados)));
  }

  loadSelectionState(idEspectaculo?: number | null): number[] {
    if (!this.isAvailable()) {
      return [];
    }

    const idsRaw = localStorage.getItem(this.buildKey(this.entradasSeleccionadasKeyPrefix, idEspectaculo));
    return idsRaw ? (JSON.parse(idsRaw) as number[]) : [];
  }

  clearSelectionState(idEspectaculo?: number | null): void {
    if (!this.isAvailable()) {
      return;
    }

    localStorage.removeItem(this.buildKey(this.entradasSeleccionadasKeyPrefix, idEspectaculo));
    localStorage.removeItem(this.buildKey(this.zonaSeleccionadaKeyPrefix, idEspectaculo));
    localStorage.removeItem(this.buildKey(this.idEntradaZonaReservadaKeyPrefix, idEspectaculo));
  }

  clearAllReservationState(idEspectaculo?: number | null): void {
    this.clearReservaExpiracion(idEspectaculo);
    this.clearSelectionState(idEspectaculo);
    this.clearTokenReserva(idEspectaculo);
  }

  setTokenReserva(token: string, idEspectaculo?: number | null): void {
    if (!this.isAvailable()) return;
    localStorage.setItem(this.buildKey(this.tokenReservaKeyPrefix, idEspectaculo), token);
  }

  getTokenReserva(idEspectaculo?: number | null): string | null {
    if (!this.isAvailable()) return null;
    return localStorage.getItem(this.buildKey(this.tokenReservaKeyPrefix, idEspectaculo));
  }

  clearTokenReserva(idEspectaculo?: number | null): void {
    if (!this.isAvailable()) return;
    localStorage.removeItem(this.buildKey(this.tokenReservaKeyPrefix, idEspectaculo));
  }
}