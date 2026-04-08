import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { EspectaculosService } from './espectaculos.service';
import { Router } from '@angular/router';
import { retry } from 'rxjs';


@Component({
  selector: 'app-espectaculos',
  imports: [CommonModule],
  standalone: true,              
  templateUrl: './espectaculos.html',
  styleUrl: './espectaculos.css',
})
export class Espectaculos implements OnInit {
  escenarios: any[] = [];
  loggedUser: string | null = null;
  userAvatarUrl: string | null = null;
  private readonly meses = ['ene', 'feb', 'mar', 'abr', 'may', 'jun', 'jul', 'ago', 'sep', 'oct', 'nov', 'dic'];
  private readonly dias = ['dom', 'lun', 'mar', 'mie', 'jue', 'vie', 'sab'];

  constructor(private espectaculosService: EspectaculosService, private router: Router) {
    this.loadUserFromStorage();
  }

  ngOnInit(): void {
    this.getEscenarios();
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
    this.espectaculosService.getEscenarios().pipe(retry({ count: 2, delay: 800 })).subscribe(
      (response: any) => {
        this.escenarios = Array.isArray(response)
          ? response.map((escenario: any) => ({ ...escenario, espectaculos: [] }))
          : [];
        this.escenarios.forEach((escenario: any) => this.getEspectaculos(escenario));
      },
      (error: any) => {
        console.error('Error al obtener los escenarios', error);
      }
    )
  }

  getEspectaculos(escenarios : any){
    this.espectaculosService.getEspectaculos(escenarios.id).pipe(retry({ count: 2, delay: 800 })).subscribe(
      (response : any) => {
        escenarios.espectaculos = Array.isArray(response) ? response : [];
        escenarios.espectaculos.forEach((espectaculo: any) => this.getNumeroDeEntradas(espectaculo));
        this.escenarios = [...this.escenarios];
      },
      (error:any) => {
        console.error('Error al obtener los escenarios', error);
      }
    )
  }

  get totalEventos(): number {
    return this.escenarios.reduce((acc, escenario) => acc + (escenario.espectaculos?.length ?? 0), 0);
  }

  private parseFecha(fecha: any): Date | null {
    if (!fecha) {
      return null;
    }

    const date = new Date(fecha);
    return Number.isNaN(date.getTime()) ? null : date;
  }

  getMesCorto(fecha: any): string {
    const date = this.parseFecha(fecha);
    return date ? this.meses[date.getMonth()] : '---';
  }

  getDiaMes(fecha: any): string {
    const date = this.parseFecha(fecha);
    return date ? String(date.getDate()).padStart(2, '0') : '--';
  }

  getDiaHora(fecha: any): string {
    const date = this.parseFecha(fecha);
    if (!date) {
      return 'Fecha por confirmar';
    }

    const hora = String(date.getHours()).padStart(2, '0');
    const minutos = String(date.getMinutes()).padStart(2, '0');
    return `${this.dias[date.getDay()]} • ${hora}:${minutos}`;
  }

  disponibilidadLimitada(espectaculo: any): boolean {
    const total = espectaculo?.entradas?.total;
    const libres = espectaculo?.entradas?.libres;

    if (typeof total !== 'number' || typeof libres !== 'number' || total <= 0) {
      return false;
    }

    return libres / total <= 0.25;
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
  this.espectaculosService.getNumeroDeEntradasComoDto(espectaculo.id).pipe(retry({ count: 1, delay: 600 })).subscribe(
    (response: any) => {
      espectaculo.entradas = response;
      this.escenarios = [...this.escenarios];
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
