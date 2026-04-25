import { Component, OnInit, OnDestroy, DestroyRef, inject, ChangeDetectorRef } from '@angular/core';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { forkJoin } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { EspectaculosService } from '../espectaculos/espectaculos.service';
import { CommonModule } from '@angular/common';
import { ElegirEntradasStorageService } from './elegir-entradas-storage.service';
import { ElegirEntradasMapService } from './elegir-entradas-map.service';
import { ButacaSvg, ColaEstadoDto, EntradaMapaDto, ZonaResumen } from './elegir-entradas.model';

@Component({
  selector: 'app-elegir-entradas',
  templateUrl: './elegir-entradas.html',
  standalone: true,
  imports: [CommonModule, RouterModule],
  styleUrls: ['./elegir-entradas.css']
})
export class ElegirEntradas implements OnInit, OnDestroy {

  infoCompra: any;
  entradasMapa: EntradaMapaDto[] = [];

  butacas: ButacaSvg[] = [];
  zonas: ZonaResumen[] = [];

  idsButacasSeleccionadas = new Set<number>();
  zonaSeleccionada: number | null = null;

  usaColaVirtual = false;
  puedeComprar = true;
  estaEnCola = false;
  estadoCola: ColaEstadoDto | null = null;
  pollingCola: any = null;

  tiempoRestante: number = 0;
  intervalId: any = null;
  idEntradaZonaReservada: number | null = null;

  private destroyRef = inject(DestroyRef);

  constructor(
    private route: ActivatedRoute,
    private reservasService: EspectaculosService,
    private router: Router,
    private cdr: ChangeDetectorRef,
    private storageService: ElegirEntradasStorageService,
    private mapService: ElegirEntradasMapService
  ) { }

  ngOnInit(): void {
    this.route.queryParamMap
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(params => {
        const idStr = params.get('idEspectaculo');
        if (idStr) {
          this.cargarDatos(Number(idStr));
        }
      });
    
    this.verificarContadorExistente();
  }

  ngOnDestroy(): void {
    if (this.pollingCola) {
      clearInterval(this.pollingCola);
      this.pollingCola = null;
    }
    if (this.intervalId) {
      clearInterval(this.intervalId);
      this.intervalId = null;
    }
  }

  verificarContadorExistente() {
    const exp = this.storageService.getReservaExpiracion();
    if (exp != null) {
      const remaining = Math.floor((exp - Date.now()) / 1000);
      if (remaining > 0) {
        this.tiempoRestante = remaining;

        const estado = this.storageService.loadSelectionState();
        estado.idsSeleccionados.forEach((id: number) => this.idsButacasSeleccionadas.add(id));
        this.zonaSeleccionada = estado.zonaSeleccionada;
        this.idEntradaZonaReservada = estado.idEntradaZonaReservada;

        this.iniciarContadorLocal();
      } else {
        this.limpiarEstadoSeleccion();
      }
    }
  }

  guardarEstadoSeleccion() {
    this.storageService.saveSelectionState(
      this.idsButacasSeleccionadas,
      this.zonaSeleccionada,
      this.idEntradaZonaReservada
    );
  }

  limpiarEstadoSeleccion() {
    this.storageService.clearAllReservationState();
  }

  iniciarContadorLocal() {
    if (!this.storageService.isAvailable()) return;
    if (this.intervalId) return;
    this.intervalId = setInterval(() => {
      const exp = this.storageService.getReservaExpiracion();
      if (exp != null) {
        const remaining = Math.floor((exp - Date.now()) / 1000);
        if (remaining > 0) {
          this.tiempoRestante = remaining;
          this.cdr.detectChanges();
        } else {
          this.tiempoRestante = 0;
          clearInterval(this.intervalId);
          this.intervalId = null;
          this.limpiarEstadoSeleccion();

          const userToken = this.storageService.getAuthToken();
          
          const peticionesLiberar = [];
          this.idsButacasSeleccionadas.forEach(id => {
             peticionesLiberar.push(this.reservasService.liberar(id, userToken));
          });
          if (this.idEntradaZonaReservada != null) {
             peticionesLiberar.push(this.reservasService.liberar(this.idEntradaZonaReservada, userToken));
          }

          this.idsButacasSeleccionadas.clear();
          this.zonaSeleccionada = null;
          this.idEntradaZonaReservada = null;

          if (peticionesLiberar.length > 0) {
             forkJoin(peticionesLiberar).subscribe({
               complete: () => {
                 if (this.infoCompra?.idEspectaculo) {
                    this.cargarDatos(this.infoCompra.idEspectaculo);
                 }
               },
               error: () => {
                 if (this.infoCompra?.idEspectaculo) {
                    this.cargarDatos(this.infoCompra.idEspectaculo);
                 }
               }
             });
          } else {
             if (this.infoCompra?.idEspectaculo) {
                this.cargarDatos(this.infoCompra.idEspectaculo);
             }
          }

          this.cdr.detectChanges();
          alert('El tiempo de reserva ha expirado.');
        }
      } else {
        this.tiempoRestante = 0;
        clearInterval(this.intervalId);
        this.intervalId = null;
        this.cdr.detectChanges();
      }
    }, 1000);
  }

  registrarReservaLocal() {
    if (!this.storageService.isAvailable()) return;
    if (this.storageService.getReservaExpiracion() == null) {
      this.storageService.setReservaExpiracion(Date.now() + 300000);
      this.verificarContadorExistente();
    }
  }

  get tiempoFormateado(): string {
    const minutos = Math.floor(this.tiempoRestante / 60);
    const segundos = this.tiempoRestante % 60;
    return `${minutos}:${segundos < 10 ? '0' : ''}${segundos}`;
  }

  private cargarDatos(idEspectaculo: number): void {
    forkJoin({
      info: this.reservasService.getInfoCompra(idEspectaculo),
      entradas: this.reservasService.obtenerEntradasMapa(idEspectaculo)
    })
    .pipe(takeUntilDestroyed(this.destroyRef))
    .subscribe({
      next: ({ info, entradas }) => {
        this.infoCompra = this.mapService.resolveVisualMode(info, entradas ?? []);
        this.entradasMapa = this.mapService.filterEntradasReales(this.infoCompra, entradas ?? []);

        this.usaColaVirtual = this.infoCompra?.usaColaVirtual === true;
        this.puedeComprar = !this.usaColaVirtual;
        this.estaEnCola = false;
        this.estadoCola = null;

        if (!this.usaColaVirtual) {
          this.actualizarVisualizacion();
        }

        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Error al obtener datos de compra', err);
      }
    });
  }

  entrarEnCola(idEspectaculo: number): void {
    const userToken = this.storageService.getAuthToken();

    if (!userToken) {
      alert('No se ha encontrado el token del usuario.');
      return;
    }

    this.reservasService.entrarEnCola(idEspectaculo, userToken).subscribe({
      next: (respuesta: ColaEstadoDto) => {
        this.estaEnCola = true;
        this.estadoCola = respuesta;

        if (respuesta?.estado === 'ACTIVO' || respuesta?.puedeComprar === true) {
          this.puedeComprar = true;
          this.actualizarVisualizacion();
        } else {
          this.puedeComprar = false;
        }

        this.iniciarPollingCola(idEspectaculo, userToken);
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Error al entrar en cola', err);
        alert('No se pudo entrar en la cola.');
      }
    });
  }

  iniciarPollingCola(idEspectaculo: number, userToken: string): void {
    if (this.pollingCola) {
      clearInterval(this.pollingCola);
    }

    this.pollingCola = setInterval(() => {
      this.reservasService.obtenerEstadoCola(idEspectaculo, userToken).subscribe({
        next: (estado: ColaEstadoDto) => {
          this.estadoCola = estado;
          this.estaEnCola = true;

          if (estado?.estado === 'ACTIVO' || estado?.puedeComprar === true) {
            this.puedeComprar = true;
            this.actualizarVisualizacion();

            clearInterval(this.pollingCola);
            this.pollingCola = null;
          } else {
            this.puedeComprar = false;
          }

          this.cdr.detectChanges();
        },
        error: (err) => {
          console.error('Error consultando estado de cola', err);
        }
      });
    }, 5000);
  }

  private actualizarVisualizacion(): void {
    const visual = this.mapService.buildVisualState(this.infoCompra, this.entradasMapa);
    this.butacas = visual.butacas;
    this.zonas = visual.zonas;
  }

  toggleButaca(butaca: ButacaSvg): void {
    if ((!butaca.disponible && !this.idsButacasSeleccionadas.has(butaca.idEntrada)) || (this.usaColaVirtual && !this.puedeComprar)) {
      return;
    }

    const userToken = this.storageService.getAuthToken();

    if (this.idsButacasSeleccionadas.has(butaca.idEntrada)) {
      this.reservasService.liberar(butaca.idEntrada, userToken).subscribe(() => {
        this.idsButacasSeleccionadas.delete(butaca.idEntrada);
        if (this.idsButacasSeleccionadas.size === 0) {
           this.limpiarEstadoSeleccion();
        } else {
           this.guardarEstadoSeleccion();
        }
        this.cdr.detectChanges();
      });
    } else {
      this.reservasService.reservar(butaca.idEntrada, userToken).subscribe(() => {
        this.idsButacasSeleccionadas.add(butaca.idEntrada);
        this.registrarReservaLocal();
        this.guardarEstadoSeleccion();
        this.cdr.detectChanges();
      }, err => {
        alert('Esta entrada ya no está disponible.');
        this.cargarDatos(this.infoCompra?.idEspectaculo);
      });
    }
  }

  estaSeleccionada(idEntrada: number): boolean {
    return this.idsButacasSeleccionadas.has(idEntrada);
  }

  seleccionarZona(zona: number): void {
    if (this.usaColaVirtual && !this.puedeComprar) {
      return;
    }

    const userToken = this.storageService.getAuthToken();

    if (this.zonaSeleccionada === zona) {
      if (this.idEntradaZonaReservada != null) {
         this.reservasService.liberar(this.idEntradaZonaReservada, userToken).subscribe(() => {
            this.zonaSeleccionada = null;
            this.idEntradaZonaReservada = null;
            this.limpiarEstadoSeleccion();
            this.cdr.detectChanges();
         });
      }
    } else {
      const zonaInfo = this.zonas.find(z => z.zona === zona);
      if (!zonaInfo || zonaInfo.disponibles <= 0) {
        return;
      }
      
      const entradasZona = this.entradasMapa.filter(e => e.zona === zona && e.disponible).map(e => e.idEntrada);
      if (entradasZona.length === 0) {
         alert('No hay entradas disponibles en la zona seleccionada.');
         return;
      }
      const idEntrada = entradasZona[0];
      
      this.reservasService.reservar(idEntrada, userToken).subscribe(() => {
         if (this.idEntradaZonaReservada != null) {
            this.reservasService.liberar(this.idEntradaZonaReservada, userToken).subscribe();
         }
         this.zonaSeleccionada = zona;
         this.idEntradaZonaReservada = idEntrada;
         this.registrarReservaLocal();
         this.guardarEstadoSeleccion();
         this.cdr.detectChanges();
      }, err => {
         alert('Error al reservar la entrada en esta zona.');
         this.cargarDatos(this.infoCompra?.idEspectaculo);
      });
    }
  }

  disponiblesEnZona(zona: number): number {
    const encontrada = this.zonas.find(z => z.zona === zona);
    return encontrada ? encontrada.disponibles : 0;
  }

  irAComprarEntradas(idEspectaculo: any): void {
    if (this.usaColaVirtual && !this.puedeComprar) {
      alert('Todavía no es tu turno para comprar.');
      return;
    }

    let idsEntradas: number[] = [];

    if (this.infoCompra?.modoSeleccion === 'ZONA') {
      if (this.zonaSeleccionada == null || this.idEntradaZonaReservada == null) {
        alert('Selecciona una zona antes de continuar.');
        return;
      }

      idsEntradas = [this.idEntradaZonaReservada];
    } else {
      idsEntradas = Array.from(this.idsButacasSeleccionadas);

      if (idsEntradas.length === 0) {
        alert('No hay entradas seleccionadas.');
        return;
      }
    }

    this.router.navigate(['/comprar'], {
      queryParams: {
        idEspectaculo: idEspectaculo,
        idsEntradas: idsEntradas.join(',')
      }
    });
  }
}