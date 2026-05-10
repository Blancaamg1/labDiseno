import { Component, OnInit, OnDestroy, DestroyRef, inject, ChangeDetectorRef } from '@angular/core';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { forkJoin, Observable } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { EspectaculosService } from '../espectaculos/espectaculos.service';
import { CommonModule } from '@angular/common';
import { ElegirEntradasStorageService } from './elegir-entradas-storage.service';
import { ElegirEntradasMapService } from './elegir-entradas-map.service';
import { FormsModule } from '@angular/forms';
import { ColaEstadoDto, EntradaMapaDto, ZonaResumen } from './elegir-entradas.model';
import { AuthService } from '../auth.service';

@Component({
  selector: 'app-elegir-entradas',
  templateUrl: './elegir-entradas.html',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  styleUrls: ['./elegir-entradas.css']
})
export class ElegirEntradas implements OnInit, OnDestroy {

  infoCompra: any;
  entradasMapa: EntradaMapaDto[] = [];
  idEspectaculoActual: number | null = null;
  tokenReserva: string = '';


  zonas: ZonaResumen[] = [];

  idsEntradasSeleccionadas = new Set<number>();

  usaColaVirtual = false;
  puedeComprar = true;
  estaEnCola = false;
  estadoCola: ColaEstadoDto | null = null;
  pollingCola: any = null;

  tiempoRestante: number = 0;
  intervalId: any = null;

  // Formulario PRECISA
  plantasDisponibles: number[] = [];
  filasDisponibles: number[] = [];
  asientosFila: EntradaMapaDto[] = [];

  selectedPlanta: number = 0;
  selectedFila: number = 0;

  get asientosSeleccionadosDetalle(): EntradaMapaDto[] {
    return this.mapService.getDetallesSeleccion(this.entradasMapa, this.idsEntradasSeleccionadas);
  }

  private destroyRef = inject(DestroyRef);

  constructor(
    private route: ActivatedRoute,
    private reservasService: EspectaculosService,
    private router: Router,
    private cdr: ChangeDetectorRef,
    private storageService: ElegirEntradasStorageService,
    private mapService: ElegirEntradasMapService,
    private authService: AuthService
  ) { }

  ngOnInit(): void {
    this.route.queryParamMap
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(params => {
        const idStr = params.get('idEspectaculo');
        if (idStr) {
          this.idEspectaculoActual = Number(idStr);
          this.cargarDatos(this.idEspectaculoActual);
          // Ahora idEspectaculoActual ya tiene valor → la clave de localStorage es correcta
          this.verificarContadorExistente();
        }
      });
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
    const exp = this.storageService.getReservaExpiracion(this.idEspectaculoActual);
    if (exp != null) {
      const remaining = Math.floor((exp - Date.now()) / 1000);
      if (remaining > 0) {
        this.tiempoRestante = remaining;
        this.tokenReserva = this.storageService.getTokenReserva(this.idEspectaculoActual) || '';

        const ids = this.storageService.loadSelectionState(this.idEspectaculoActual);
        ids.forEach((id: number) => this.idsEntradasSeleccionadas.add(id));

        this.iniciarContadorLocal();
      } else {
        this.limpiarEstadoSeleccion();
      }
    }
  }

  guardarEstadoSeleccion() {
    this.storageService.saveSelectionState(this.idsEntradasSeleccionadas, this.idEspectaculoActual);
  }

  limpiarEstadoSeleccion() {
    this.storageService.clearAllReservationState(this.idEspectaculoActual);
    if (this.intervalId) {
      clearInterval(this.intervalId);
      this.intervalId = null;
    }
    this.tiempoRestante = 0;
    this.tokenReserva = '';
  }

  iniciarContadorLocal() {
    if (!this.storageService.isAvailable()) return;
    if (this.intervalId) return;
    this.intervalId = setInterval(() => {
      const exp = this.storageService.getReservaExpiracion(this.idEspectaculoActual);
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

          const tokenTurno = this.estadoCola?.tokenTurno || '';

          const peticionesLiberar: Observable<any>[] = [];
          this.idsEntradasSeleccionadas.forEach(id => {
            peticionesLiberar.push(this.reservasService.liberar(id, tokenTurno));
          });

          this.idsEntradasSeleccionadas.clear();

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

  registrarReservaLocal(token?: string) {
    if (!this.storageService.isAvailable()) return;
    if (token) {
      this.tokenReserva = token;
      this.storageService.setTokenReserva(token, this.idEspectaculoActual);
    }
    if (this.storageService.getReservaExpiracion(this.idEspectaculoActual) == null) {
      this.storageService.setReservaExpiracion(Date.now() + 300000, this.idEspectaculoActual);
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
            this.inicializarFormularioPrecisa();
          }

          const idsGuardados = this.storageService.loadSelectionState(this.idEspectaculoActual);
          idsGuardados.forEach((id: number) => this.idsEntradasSeleccionadas.add(id));

          this.cdr.detectChanges();
        },
        error: (err) => {
          console.error('Error al obtener datos de compra', err);
        }
      });
  }

  entrarEnCola(idEspectaculo: number): void {
    this.reservasService.entrarEnCola(idEspectaculo).subscribe({
      next: (respuesta: ColaEstadoDto) => {
        this.estaEnCola = true;
        this.estadoCola = respuesta;

        if (respuesta?.estado === 'ACTIVO' || respuesta?.puedeComprar === true) {
          this.puedeComprar = true;
          this.actualizarVisualizacion();
          this.inicializarFormularioPrecisa();
        } else {
          this.puedeComprar = false;
        }

        this.iniciarPollingCola(idEspectaculo, respuesta.tokenTurno);
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Error al entrar en cola', err);
        alert('No se pudo entrar en la cola.');
      }
    });
  }

  iniciarPollingCola(idEspectaculo: number, tokenTurno: string): void {
    if (this.pollingCola) {
      clearInterval(this.pollingCola);
    }

    this.pollingCola = setInterval(() => {
      this.reservasService.obtenerEstadoCola(idEspectaculo, tokenTurno).subscribe({
        next: (estado: ColaEstadoDto) => {
          this.estadoCola = estado;
          this.estaEnCola = true;

          if (estado?.estado === 'ACTIVO' || estado?.puedeComprar === true) {
            this.puedeComprar = true;
            this.actualizarVisualizacion();
            this.inicializarFormularioPrecisa();

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
    this.zonas = visual.zonas;
  }


  estaSeleccionada(idEntrada: number): boolean {
    return this.idsEntradasSeleccionadas.has(idEntrada);
  }

  seleccionarZona(zona: number): void {
    this.incrementarEntradasZona(zona);
  }

  cantidadSeleccionadaZona(zona: number): number {
    return Array.from(this.idsEntradasSeleccionadas).filter(id => {
      const entrada = this.entradasMapa.find(e => e.idEntrada === id);
      return entrada?.zona === zona;
    }).length;
  }

  incrementarEntradasZona(zona: number): void {
    if (this.usaColaVirtual && !this.puedeComprar) return;

    const zonaInfo = this.zonas.find(z => z.zona === zona);
    if (!zonaInfo || zonaInfo.disponibles <= 0) {
      alert('No hay más entradas disponibles en esta zona.');
      return;
    }

    const entradasDisponibles = this.entradasMapa.filter(
      e => e.zona === zona && e.disponible && !this.idsEntradasSeleccionadas.has(e.idEntrada)
    );
    if (entradasDisponibles.length === 0) {
      alert('No hay más entradas disponibles en esta zona.');
      return;
    }

    const idEntrada = entradasDisponibles[0].idEntrada;

    const tokenTurno = this.estadoCola?.tokenTurno || '';
    this.reservasService.reservar(idEntrada, tokenTurno, this.tokenReserva).subscribe((res: any) => {
      this.idsEntradasSeleccionadas.add(idEntrada);
      this.registrarReservaLocal(res?.tokenReserva);
      this.guardarEstadoSeleccion();
      this.cdr.detectChanges();
    }, err => {
      console.error('Error al reservar', err);
      alert('Error al reservar la entrada.');
      this.cargarDatos(this.infoCompra?.idEspectaculo);
    });
  }

  decrementarEntradasZona(zona: number): void {
    const idParaLiberar = Array.from(this.idsEntradasSeleccionadas).find(id => {
      const entrada = this.entradasMapa.find(e => e.idEntrada === id);
      return entrada?.zona === zona;
    });

    if (!idParaLiberar) return;

    const tokenTurno = this.estadoCola?.tokenTurno || '';
    this.reservasService.liberar(idParaLiberar, tokenTurno).subscribe(() => {
      this.idsEntradasSeleccionadas.delete(idParaLiberar);
      if (this.idsEntradasSeleccionadas.size === 0) {
        this.limpiarEstadoSeleccion();
      } else {
        this.guardarEstadoSeleccion();
      }
      this.cdr.detectChanges();
    });
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

    const idsEntradas = Array.from(this.idsEntradasSeleccionadas);

    if (this.idEspectaculoActual != null && Number(idEspectaculo) !== this.idEspectaculoActual) {
      alert('El espectáculo actual no coincide con la selección activa.');
      return;
    }

    if (idsEntradas.length === 0) {
      alert('No hay entradas seleccionadas.');
      return;
    }

    if (!this.estaLogueado()) {
      const idsEntradasStr = idsEntradas.join(',');
      const tokenTurno = this.estadoCola?.tokenTurno || '';
      const returnUrl = `/comprar?idEspectaculo=${idEspectaculo}&idsEntradas=${idsEntradasStr}&tokenTurno=${tokenTurno}&tokenReserva=${this.tokenReserva}`;

      this.router.navigate(['/login'], {
        queryParams: { returnUrl }
      });
      return;
    }

    this.router.navigate(['/comprar'], {
      queryParams: {
        idEspectaculo: idEspectaculo,
        idsEntradas: idsEntradas.join(','),
        tokenTurno: this.estadoCola?.tokenTurno,
        tokenReserva: this.tokenReserva
      }
    });
  }

  // Lógica del Formulario PRECISA
  inicializarFormularioPrecisa() {
    if (this.infoCompra?.modoSeleccion !== 'PRECISA') return;

    this.plantasDisponibles = this.mapService.getPlantasDisponibles(this.entradasMapa);

    if (this.plantasDisponibles.length > 0) {
      this.selectedPlanta = this.plantasDisponibles[0];
      this.onPlantaChange();
    }
  }

  onPlantaChange() {
    this.filasDisponibles = this.mapService.getFilasDisponibles(this.entradasMapa, Number(this.selectedPlanta));

    if (this.filasDisponibles.length > 0) {
      this.selectedFila = this.filasDisponibles[0];
      this.onFilaChange();
    } else {
      this.selectedFila = 0;
      this.asientosFila = [];
    }
  }

  onFilaChange() {
    this.asientosFila = this.mapService.getTodosAsientosFila(
      this.entradasMapa,
      Number(this.selectedPlanta),
      Number(this.selectedFila)
    );
  }

  toggleButacaPorId(idEntrada: number) {
    if (this.estaSeleccionada(idEntrada)) {
      this.deseleccionarEntrada(idEntrada);
    } else {
      this.seleccionarEntrada(idEntrada);
    }
  }

  private seleccionarEntrada(idEntrada: number) {
    const tokenTurno = this.estadoCola?.tokenTurno || '';
    this.reservasService.reservar(idEntrada, tokenTurno, this.tokenReserva).subscribe({
      next: (res: any) => {
        this.idsEntradasSeleccionadas.add(idEntrada);
        this.guardarEstadoSeleccion();
        this.registrarReservaLocal(res?.tokenReserva);
        this.cdr.detectChanges();
        this.onFilaChange();
      },
      error: (err) => {
        console.error('Error al reservar', err);
        alert('No se pudo reservar la entrada.');
      }
    });
  }

  private estaLogueado(): boolean {
    return !!localStorage.getItem('authToken');
  }

  private deseleccionarEntrada(idEntrada: number) {
    if (!this.estaLogueado()) {
      this.idsEntradasSeleccionadas.delete(idEntrada);
      if (this.idsEntradasSeleccionadas.size === 0) {
        this.limpiarEstadoSeleccion();
      } else {
        this.guardarEstadoSeleccion();
      }
      this.cdr.detectChanges();
      this.onFilaChange();
      return;
    }

    const tokenTurno = this.estadoCola?.tokenTurno || '';
    this.reservasService.liberar(idEntrada, tokenTurno).subscribe({
      next: () => {
        this.idsEntradasSeleccionadas.delete(idEntrada);
        if (this.idsEntradasSeleccionadas.size === 0) {
          this.limpiarEstadoSeleccion();
        } else {
          this.guardarEstadoSeleccion();
        }
        this.cdr.detectChanges();
        this.onFilaChange();
      }
    });
  }
}