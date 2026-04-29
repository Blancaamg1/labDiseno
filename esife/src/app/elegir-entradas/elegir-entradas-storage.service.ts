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

  saveSelectionState(idsSeleccionados: Set<number>): void {
    if (!this.isAvailable()) {
      return;
    }

    localStorage.setItem(this.entradasSeleccionadasKey, JSON.stringify(Array.from(idsSeleccionados)));
  }

  loadSelectionState(): number[] {
    if (!this.isAvailable()) {
      return [];
    }

    const idsRaw = localStorage.getItem(this.entradasSeleccionadasKey);
    return idsRaw ? (JSON.parse(idsRaw) as number[]) : [];
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