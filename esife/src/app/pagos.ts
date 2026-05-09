import { HttpClient, HttpHeaders } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root',
})
export class Pagos {

    constructor(private http: HttpClient){}

    private authHeaders(): HttpHeaders {
      const token = typeof localStorage !== 'undefined' ? localStorage.getItem('authToken') : null;
      return new HttpHeaders({ 'Authorization': `Bearer ${token ?? ''}` });
    }

    prepararPago(info: any){
      return this.http.post('http://localhost:8080/pagos/prepararPago', info,
        { responseType: 'text', headers: this.authHeaders() });
    }

    confirmarCompra(info: any) {
      return this.http.post('http://localhost:8080/pagos/confirmar', info,
        { headers: this.authHeaders() });
    }

}
