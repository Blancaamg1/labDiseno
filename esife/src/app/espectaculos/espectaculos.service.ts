import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';

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
}