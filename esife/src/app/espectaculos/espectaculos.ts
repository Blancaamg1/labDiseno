import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { EspectaculosService } from './espectaculos.service';
import { Router,RouterModule } from '@angular/router';
import { retry } from 'rxjs';

@Component({
  selector: 'app-espectaculos',
  imports: [CommonModule, FormsModule,RouterModule],
  standalone: true,              
  templateUrl: './espectaculos.html',
  styleUrl: './espectaculos.css',
})
export class Espectaculos implements OnInit {
  escenarios: any[] = [];
  escenariosOriginales: any[] = [];
  filtroTexto = '';
  filtroFecha = '';

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
        this.escenariosOriginales = Array.isArray(response)
          ? response.map((escenario: any) => ({ ...escenario, espectaculos: [] }))
          : [];
        this.aplicarFiltros();
        this.escenariosOriginales.forEach((escenario: any) => this.getEspectaculos(escenario));
      },
      (error: any) => {
        console.error('Error al obtener los escenarios', error);
      }
    )
  }

  getEspectaculos(escenario : any){
    this.espectaculosService.getEspectaculos(escenario.id).pipe(retry({ count: 2, delay: 800 })).subscribe(
      (response : any) => {
        escenario.espectaculos = Array.isArray(response) ? response : [];
        escenario.espectaculos.forEach((espectaculo: any) => this.getNumeroDeEntradas(espectaculo));
        this.aplicarFiltros();
      },
      (error:any) => {
        console.error('Error al obtener los escenarios', error);
      }
    )
  }

  get totalEventos(): number {
    return this.escenarios.reduce((acc, escenario) => acc + (escenario.espectaculos?.length ?? 0), 0);
  }

  get hayFiltrosActivos(): boolean {
    return !!this.filtroTexto.trim() || !!this.filtroFecha;
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
      this.aplicarFiltros();
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

  irAElegirEntradas(espectaculo: any): void {
  if (!espectaculo?.id) {
    return;
  }

  this.router.navigate(['/elegirEntradas'], {
    queryParams: { idEspectaculo: espectaculo.id }
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
      localStorage.removeItem('authToken');
    }
    this.loggedUser = null;
    this.userAvatarUrl = null;
    this.router.navigate(['/login']);
  }

  onFiltroChange(): void {
    this.aplicarFiltros();
  }

  limpiarFiltros(): void {
    this.filtroTexto = '';
    this.filtroFecha = '';
    this.aplicarFiltros();
  }

  private aplicarFiltros(): void {
    const texto = this.filtroTexto.trim().toLowerCase();
    const fechaFiltro = this.filtroFecha;
    const hayFiltros = !!texto || !!fechaFiltro;

    if (!hayFiltros) {
      this.escenarios = this.escenariosOriginales.map((escenario: any) => ({
        ...escenario,
        espectaculos: [...(escenario.espectaculos ?? [])]
      }));
      return;
    }

    this.escenarios = this.escenariosOriginales
      .map((escenario: any) => {
        const espectaculosFiltrados = (escenario.espectaculos ?? []).filter((espectaculo: any) => {
          const coincideTexto = this.coincideTexto(escenario, espectaculo, texto);
          const coincideFecha = this.coincideFecha(espectaculo?.fecha, fechaFiltro);
          return coincideTexto && coincideFecha;
        });

        return {
          ...escenario,
          espectaculos: espectaculosFiltrados
        };
      })
      .filter((escenario: any) => escenario.espectaculos.length > 0);
  }

  private coincideTexto(escenario: any, espectaculo: any, texto: string): boolean {
    if (!texto) {
      return true;
    }

    const contenido = [
      espectaculo?.nombre,
      espectaculo?.artista,
      espectaculo?.descripcion,
      escenario?.nombre,
      escenario?.descripcion,
      escenario?.lugar
    ]
      .filter(Boolean)
      .join(' ')
      .toLowerCase();

    return contenido.includes(texto);
  }

  private coincideFecha(fechaEvento: any, fechaFiltro: string): boolean {
    if (!fechaFiltro) {
      return true;
    }

    const date = this.parseFecha(fechaEvento);
    if (!date) {
      return false;
    }

    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    const fechaEventoNormalizada = `${year}-${month}-${day}`;

    return fechaEventoNormalizada === fechaFiltro;
  }
}
