import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root',
})
export class Pagos {

    constructor(private http: HttpClient){}

    prepararPago(info: any){
      return this.http.post('http://localhost:8080/pagos/prepararPago', info, {responseType: 'text'});
    }

    confirmarPago() {
      return this.http.get('http://localhost:8080/pagos/confirmarPago', {
    responseType: 'text'
  });
}

}
