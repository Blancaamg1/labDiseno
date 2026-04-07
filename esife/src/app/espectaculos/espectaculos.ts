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
  loggedUser: string | null = null;
  userAvatarUrl: string | null = null;

  constructor(private espectaculosService: EspectaculosService, private router: Router) {
    this.loadUserFromStorage();
  }

  private loadUserFromStorage() {
    if (typeof localStorage !== 'undefined') {
      this.loggedUser = localStorage.getItem('loggedUserName');
      // Opcional: si quieres generar un avatar URL basado en el nombre
      if (this.loggedUser) {
        this.userAvatarUrl = `https://ui-avatars.com/api/?name=${encodeURIComponent(this.loggedUser)}&background=random`;
      }
    }
  }

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
    queryParams: { returnUrl: '/espectaculos' }
  });
}

  cerrarSesion() {
    if (typeof localStorage !== 'undefined') {
      localStorage.removeItem('loggedUserName');
    }
    this.loggedUser = null;
    this.userAvatarUrl = null;
    this.router.navigate(['/login']);
  }
}
