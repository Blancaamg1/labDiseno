import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { EntradaMapaDto, ColaEstadoDto } from '../elegir-entradas/elegir-entradas';
@Injectable({
  providedIn: 'root'
})
export class EspectaculosService {
  private readonly http = inject(HttpClient);

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
    return this.http.get(`http://localhost:8080/reservas/infoCompra?idEspectaculo=${idEspectaculo}`);
  }

  obtenerEntradasMapa(idEspectaculo: number) {
    return this.http.get<EntradaMapaDto[]>(
      `http://localhost:8080/reservas/entradasMapa?idEspectaculo=${idEspectaculo}`
    );
  }

  entrarEnCola(idEspectaculo: number, userToken: string) {
    return this.http.post<ColaEstadoDto>(
      `http://localhost:8080/cola/entrar?idEspectaculo=${idEspectaculo}&userToken=${userToken}`,
      {}
    );
  }

  obtenerEstadoCola(idEspectaculo: number, userToken: string) {
    return this.http.get<ColaEstadoDto>(
      `http://localhost:8080/cola/estado?idEspectaculo=${idEspectaculo}&userToken=${userToken}`
    );
  }

  reservar(idEntrada: number, userToken: string) {
    return this.http.put(
      `http://localhost:8080/reservas/reservar?idEntrada=${idEntrada}&userToken=${userToken}`, 
      {}
    );
  }

  liberar(idEntrada: number, userToken: string) {
    return this.http.put(
      `http://localhost:8080/reservas/liberar?idEntrada=${idEntrada}&userToken=${userToken}`, 
      {}
    );
  }
}