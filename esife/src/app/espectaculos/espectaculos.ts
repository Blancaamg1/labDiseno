import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { EspectaculosService } from './espectaculos.service';
import { Router } from '@angular/router';

@Component({
  selector: 'app-espectaculos',
  imports: [CommonModule],
  standalone: true,              
  templateUrl: './espectaculos.html',
  styleUrl: './espectaculos.css',
})
export class Espectaculos {
  escenarios: any[] = [];
  constructor(private espectaculosService: EspectaculosService, private router: Router) {}

  getEscenarios(){
    this.espectaculosService.getEscenarios().subscribe(
      (response: any) => {
        this.escenarios = response
      },
      (error: any) => {
        console.error('Error al obtener los escenarios', error);
      }
    )
  }

  getEspectaculos(escenarios : any){
    this.espectaculosService.getEspectaculos(escenarios.id).subscribe(
      (response : any) => {
        escenarios.espectaculos = response
      },
      (error:any) => {
        console.error('Error al obtener los escenarios', error);
      }
    )
  }

  /* Ejemplo de una petición anidada (se envía cuando se recibe la respuesta de la primera petición)
  getNumeroDeEntradas(espectaculo: any){
  this.espectaculosService.getNumeroDeEntradas(espectaculo.id).subscribe(
    (response: any) => {
      espectaculo.entradas = response;
      this.getEntradasLibres(espectaculo)
    },
    (error:any) => {
      console.error('Error al obtener las entradas', error);
    }
  );
  } 

  getEntradasLibres(espectaculo: any) {
    this.espectaculosService.getEntradasLibres(espectaculo.id).subscribe(
    (response: any) => {
      espectaculo.getEntradasLibres = response
    },
    (error:any) => {
      console.error('Error al obtener las entradas', error);
    }
  );
  } */

  getNumeroDeEntradas(espectaculo: any){
  this.espectaculosService.getNumeroDeEntradasComoDto(espectaculo.id).subscribe(
    (response: any) => {
      espectaculo.entradas = response;
    },
    (error:any) => {
      console.error('Error al obtener las entradas', error);
    }
  );
  } 

  irAComprarEntradas(espectaculo: any){
    // Se pasa el id del espectaculo en la URL para recuperarlo en CompraComponent.
    this.router.navigate(['/comprar'], {
      queryParams: { idEspectaculo: espectaculo?.id }
    });
  }

   login() {
  this.router.navigate(['/login'], {
    queryParams: { returnUrl: '/' }
  });
}
}
