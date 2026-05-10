import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { EntradaMapaDto, ColaEstadoDto } from '../elegir-entradas/elegir-entradas.model';
@Injectable({
  providedIn: 'root'
})
export class EspectaculosService {
  private readonly http = inject(HttpClient);

  /** Genera las cabeceras con el token de sesión */
  private authHeaders(): HttpHeaders {
    const token = typeof localStorage !== 'undefined' ? localStorage.getItem('authToken') : null;
    return new HttpHeaders({ 'Authorization': `Bearer ${token ?? ''}` });
  }

  getEscenarios() {
    return this.http.get('http://localhost:8080/busqueda/getEscenarios');
  }

  getEspectaculos(idEscenario: any) {
    return this.http.get(`http://localhost:8080/busqueda/getEspectaculos/${idEscenario}`);
  }

  getNumeroDeEntradas(id: any) {
    return this.http.get(`http://localhost:8080/busqueda/getNumeroDeEntradas?idEspectaculo=${id}`);
  }

  getEntradasLibres(id: any) {
    return this.http.get(`http://localhost:8080/busqueda/getEntradasLibres?idEspectaculo=${id}`);
  }

  getNumeroDeEntradasComoDto(id: any) {
    return this.http.get(`http://localhost:8080/busqueda/getNumeroDeEntradasComoDto?idEspectaculo=${id}`);
  }

  getInfoCompra(idEspectaculo: any) {
    return this.http.get(`http://localhost:8080/reservas/infoCompra?idEspectaculo=${idEspectaculo}`,
      { headers: this.authHeaders() });
  }

  obtenerEntradasMapa(idEspectaculo: number) {
    return this.http.get<EntradaMapaDto[]>(
      `http://localhost:8080/reservas/entradasMapa?idEspectaculo=${idEspectaculo}`,
      { headers: this.authHeaders() }
    );
  }

  entrarEnCola(idEspectaculo: number) {
    return this.http.post<ColaEstadoDto>(
      `http://localhost:8080/cola/entrar?idEspectaculo=${idEspectaculo}`,
      {},
      { headers: this.authHeaders() }
    );
  }

  obtenerEstadoCola(idEspectaculo: number, tokenTurno: string) {
    return this.http.get<ColaEstadoDto>(
      `http://localhost:8080/cola/estado?idEspectaculo=${idEspectaculo}&tokenTurno=${tokenTurno}`,
      { headers: this.authHeaders() }
    );
  }

  reservar(idEntrada: number, tokenTurno: string, tokenReserva: string = '') {
    const url = `http://localhost:8080/reservas/reservar?idEntrada=${idEntrada}&tokenTurno=${tokenTurno}&tokenReserva=${tokenReserva}`;
    return this.http.put<any>(url, {}, { headers: this.authHeaders() });
  }

  liberar(idEntrada: number, tokenTurno: string) {
    return this.http.put(
      `http://localhost:8080/reservas/liberar?idEntrada=${idEntrada}&tokenTurno=${tokenTurno}`,
      {},
      { headers: this.authHeaders() }
    );
  }

  salirDeCola(idEspectaculo: number, tokenTurno: string) {
    return this.http.post(
      `http://localhost:8080/cola/salir?idEspectaculo=${idEspectaculo}&tokenTurno=${tokenTurno}`,
      {},
      { headers: this.authHeaders() }
    );
  }
}