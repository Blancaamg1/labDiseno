import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';


@Injectable({
  providedIn: 'root'
})
export class Espectaculos {
  
  private readonly http = inject(HttpClient);

  getEscenarios(){
    return this.http.get('http://localhost:8080/busqueda/getEscenarios');
  }

  getEspectaculos() {
      return this.http.get('http://localhost:8080/busqueda/getEspectaculos/${escenario.id}');
  }

  getEntradas() {
      return this.http.get('http://localhost:8080/busqueda/getEntradas?${espectaculo.id}');
  }
}