import { Component, OnInit, OnDestroy, DestroyRef, inject, ChangeDetectorRef } from '@angular/core';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { forkJoin } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { EspectaculosService } from '../espectaculos/espectaculos.service';
import { CommonModule } from '@angular/common';

export interface EntradaMapaDto {
  idEntrada: number;
  disponible: boolean;
  fila?: number;
  columna?: number;
  planta?: number;
  zona?: number;
}

export interface ButacaSvg extends EntradaMapaDto {
  x: number;
  y: number;
}

export interface ZonaResumen {
  zona: number;
  disponibles: number;
}

export interface ColaEstadoDto {
  idEspectaculo: number;
  idUsuario: number;
  posicion: number;
  personasDelante: number;
  estado: string;
  puedeComprar: boolean;
  tokenTurno: string;
  segundosRestantes: number;
  mensaje: string;
}

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

  private destroyRef = inject(DestroyRef);

  constructor(
    private route: ActivatedRoute,
    private reservasService: EspectaculosService,
    private router: Router,
    private cdr: ChangeDetectorRef
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
  }

  ngOnDestroy(): void {
    if (this.pollingCola) {
      clearInterval(this.pollingCola);
      this.pollingCola = null;
    }
  }

  private cargarDatos(idEspectaculo: number): void {
    forkJoin({
      info: this.reservasService.getInfoCompra(idEspectaculo),
      entradas: this.reservasService.obtenerEntradasMapa(idEspectaculo)
    })
    .pipe(takeUntilDestroyed(this.destroyRef))
    .subscribe({
      next: ({ info, entradas }) => {
        this.infoCompra = this.resolverModoVisual(info, entradas ?? []);
        this.entradasMapa = this.filtrarEntradasReales(entradas ?? []);

        this.usaColaVirtual = this.infoCompra?.usaColaVirtual === true;
        this.puedeComprar = !this.usaColaVirtual;
        this.estaEnCola = false;
        this.estadoCola = null;

        if (!this.usaColaVirtual) {
          this.prepararMapa();
        }

        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Error al obtener datos de compra', err);
      }
    });
  }

  entrarEnCola(idEspectaculo: number): void {
    const userToken = localStorage.getItem('authToken');

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
          this.prepararMapa();
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
            this.prepararMapa();

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

  private resolverModoVisual(info: any, entradas: EntradaMapaDto[]): any {
    const precisas = entradas.filter(e => e.planta != null && e.fila != null && e.columna != null).length;
    const zonas = entradas.filter(e => e.zona != null).length;
    const tipoMapa = info?.tipoMapa;
    const soportaZonaVisual = tipoMapa === 'ESTADIO_MUNICIPAL' || tipoMapa === 'PLAZA_ABIERTA';

    if (soportaZonaVisual && (info?.modoSeleccion === 'PRECISA' || !info?.modoSeleccion) && zonas > precisas) {
      return { ...info, modoSeleccion: 'ZONA' };
    }

    if (info?.modoSeleccion === 'ZONA' && precisas > 0 && zonas === 0) {
      return { ...info, modoSeleccion: 'PRECISA' };
    }

    return info;
  }

  private filtrarEntradasReales(entradas: EntradaMapaDto[]): EntradaMapaDto[] {
    if (this.infoCompra?.modoSeleccion === 'PRECISA') {
      return entradas
        .filter(e =>
          (e.planta != null && e.fila != null && e.columna != null) ||
          e.zona != null
        )
        .sort((a, b) =>
          ((a.planta ?? Number.MAX_SAFE_INTEGER) - (b.planta ?? Number.MAX_SAFE_INTEGER)) ||
          ((a.fila ?? Number.MAX_SAFE_INTEGER) - (b.fila ?? Number.MAX_SAFE_INTEGER)) ||
          ((a.columna ?? Number.MAX_SAFE_INTEGER) - (b.columna ?? Number.MAX_SAFE_INTEGER)) ||
          ((a.zona ?? Number.MAX_SAFE_INTEGER) - (b.zona ?? Number.MAX_SAFE_INTEGER))
        );
    }

    return entradas
      .filter(e => e.zona != null)
      .sort((a, b) => (a.zona! - b.zona!));
  }

  prepararMapa(): void {
    if (this.infoCompra.modoSeleccion === 'PRECISA') {
      this.butacas = [];
      const zonasEnModoPreciso = this.entradasMapa.filter(e =>
        e.zona != null && (e.planta == null || e.fila == null || e.columna == null)
      );

      const totalPorZona = new Map<number, number>();
      const indicePorZona = new Map<number, number>();

      for (const entrada of zonasEnModoPreciso) {
        const zona = entrada.zona as number;
        totalPorZona.set(zona, (totalPorZona.get(zona) ?? 0) + 1);
      }

      for (const entrada of this.entradasMapa) {
        if (entrada.fila != null && entrada.columna != null && entrada.planta != null) {
          const pos = this.calcularPosicionButaca(entrada);
          this.butacas.push({ ...entrada, x: pos.x, y: pos.y });
        } else if (entrada.zona != null) {
          const zona = entrada.zona;
          const indice = indicePorZona.get(zona) ?? 0;
          indicePorZona.set(zona, indice + 1);

          const pos = this.calcularPosicionButacaPorZona(
            zona,
            indice,
            totalPorZona.get(zona) ?? 1
          );

          this.butacas.push({ ...entrada, x: pos.x, y: pos.y });
        }
      }
    } else {
      this.prepararZonas();
    }
  }

  private calcularPosicionButacaPorZona(zona: number, indice: number, total: number): { x: number; y: number } {
    const rect = this.obtenerRectanguloZona(zona);

    if (!rect) {
      return { x: 100, y: 100 };
    }

    return this.colocarPorIndiceEnRectangulo(rect.x, rect.y, rect.width, rect.height, indice, total);
  }

  private obtenerRectanguloZona(zona: number): { x: number; y: number; width: number; height: number } | null {
    switch (this.infoCompra?.tipoMapa) {
      case 'AUDITORIO_PRINCIPAL':
        if (zona === 1) return { x: 70, y: 150, width: 240, height: 260 };
        if (zona === 2) return { x: 330, y: 150, width: 240, height: 260 };
        if (zona === 3) return { x: 590, y: 150, width: 240, height: 260 };
        return { x: 220, y: 470, width: 460, height: 85 };

      case 'TEATRO_CLASICO':
        if (zona === 1) return { x: 120, y: 160, width: 660, height: 245 };
        if (zona === 2) return { x: 170, y: 455, width: 560, height: 72 };
        if (zona === 3) return { x: 55, y: 180, width: 75, height: 120 };
        return { x: 770, y: 180, width: 75, height: 120 };

      case 'SALA_EXPERIMENTAL':
        if (zona === 1) return { x: 170, y: 175, width: 240, height: 210 };
        return { x: 490, y: 175, width: 240, height: 210 };

      default:
        return null;
    }
  }

  private colocarPorIndiceEnRectangulo(
    x: number,
    y: number,
    width: number,
    height: number,
    indice: number,
    total: number
  ): { x: number; y: number } {
    const totalSeguro = Math.max(1, total);
    const proporcion = width / Math.max(1, height);
    const columnas = Math.max(1, Math.ceil(Math.sqrt(totalSeguro * proporcion)));
    const filas = Math.max(1, Math.ceil(totalSeguro / columnas));

    const fila = Math.floor(indice / columnas) + 1;
    const columna = (indice % columnas) + 1;

    return this.colocarEnRejilla(x, y, width, height, fila, columna, filas, columnas, 16, 16);
  }

  prepararZonas(): void {
    const acumulado: { [zona: number]: number } = {};

    for (const entrada of this.entradasMapa) {
      if (entrada.zona != null && entrada.disponible) {
        if (!acumulado[entrada.zona]) {
          acumulado[entrada.zona] = 0;
        }
        acumulado[entrada.zona]++;
      }
    }

    this.zonas = [];
    for (const clave of Object.keys(acumulado)) {
      this.zonas.push({
        zona: Number(clave),
        disponibles: acumulado[Number(clave)]
      });
    }
  }

  calcularPosicionButaca(entrada: EntradaMapaDto): { x: number, y: number } {
    const planta = entrada.planta ?? 0;
    const rango = this.obtenerRangoPlanta(planta);
    const fila = this.normalizarIndice(entrada.fila, rango.minFila);
    const columna = this.normalizarIndice(entrada.columna, rango.minColumna);

    switch (this.infoCompra.tipoMapa) {
      case 'AUDITORIO_PRINCIPAL': {
        const dims = this.obtenerDimensionesPlanta(planta);

        if (planta === 0) return this.colocarEnRejilla(70, 150, 240, 260, fila, columna, dims.filas, dims.columnas, 26, 24);
        if (planta === 1) return this.colocarEnRejilla(330, 150, 240, 260, fila, columna, dims.filas, dims.columnas, 26, 24);
        if (planta === 2) return this.colocarEnRejilla(590, 150, 240, 260, fila, columna, dims.filas, dims.columnas, 26, 24);

        return this.colocarEnRejilla(220, 470, 460, 85, fila, columna, dims.filas, dims.columnas, 28, 18);
      }

      case 'TEATRO_CLASICO': {
        const dims = this.obtenerDimensionesPlanta(planta);

        if (planta === 1) return this.colocarEnRejilla(55, 180, 75, 120, fila, columna, dims.filas, dims.columnas, 16, 16);
        if (planta === 2) return this.colocarEnRejilla(770, 180, 75, 120, fila, columna, dims.filas, dims.columnas, 16, 16);
        if (planta === 3) return this.colocarEnRejilla(120, 160, 660, 245, fila, columna, dims.filas, dims.columnas, 34, 26);

        return this.colocarEnRejilla(170, 455, 560, 72, fila, columna, dims.filas, dims.columnas, 28, 18);
      }

      case 'SALA_EXPERIMENTAL': {
        const dims = this.obtenerDimensionesPlanta(planta);
        const columnasTotales = Math.max(1, dims.columnas);
        const columnasPorLadoIzquierdo = Math.max(1, Math.ceil(columnasTotales / 2));
        const columnasPorLadoDerecho = Math.max(1, columnasTotales - columnasPorLadoIzquierdo);

        if (columna <= columnasPorLadoIzquierdo) {
          return this.colocarEnRejilla(195, 195, 190, 170, fila, columna, dims.filas, columnasPorLadoIzquierdo, 0, 0);
        }

        return this.colocarEnRejilla(515, 195, 190, 170, fila, columna - columnasPorLadoIzquierdo, dims.filas, columnasPorLadoDerecho, 0, 0);
      }

      default:
        return { x: 100, y: 100 };
    }
  }

  toggleButaca(butaca: ButacaSvg): void {
    if (!butaca.disponible || (this.usaColaVirtual && !this.puedeComprar)) {
      return;
    }

    if (this.idsButacasSeleccionadas.has(butaca.idEntrada)) {
      this.idsButacasSeleccionadas.delete(butaca.idEntrada);
    } else {
      this.idsButacasSeleccionadas.add(butaca.idEntrada);
    }
  }

  estaSeleccionada(idEntrada: number): boolean {
    return this.idsButacasSeleccionadas.has(idEntrada);
  }

  seleccionarZona(zona: number): void {
    if (this.usaColaVirtual && !this.puedeComprar) {
      return;
    }

    const zonaInfo = this.zonas.find(z => z.zona === zona);

    if (!zonaInfo || zonaInfo.disponibles <= 0) {
      return;
    }

    this.zonaSeleccionada = this.zonaSeleccionada === zona ? null : zona;
  }

  disponiblesEnZona(zona: number): number {
    const encontrada = this.zonas.find(z => z.zona === zona);
    return encontrada ? encontrada.disponibles : 0;
  }

  private obtenerDimensionesPlanta(planta: number): { filas: number; columnas: number } {
    const rango = this.obtenerRangoPlanta(planta);

    return {
      filas: Math.max(1, rango.maxFila - rango.minFila + 1),
      columnas: Math.max(1, rango.maxColumna - rango.minColumna + 1)
    };
  }

  private obtenerRangoPlanta(planta: number): {
    minFila: number;
    maxFila: number;
    minColumna: number;
    maxColumna: number;
  } {
    const entradasDePlanta = this.entradasMapa.filter(
      e => e.planta === planta && e.fila != null && e.columna != null
    );

    if (entradasDePlanta.length === 0) {
      return { minFila: 1, maxFila: 1, minColumna: 1, maxColumna: 1 };
    }

    let minFila = Number.POSITIVE_INFINITY;
    let maxFila = Number.NEGATIVE_INFINITY;
    let minColumna = Number.POSITIVE_INFINITY;
    let maxColumna = Number.NEGATIVE_INFINITY;

    for (const entrada of entradasDePlanta) {
      const fila = entrada.fila ?? 1;
      const columna = entrada.columna ?? 1;

      minFila = Math.min(minFila, fila);
      maxFila = Math.max(maxFila, fila);
      minColumna = Math.min(minColumna, columna);
      maxColumna = Math.max(maxColumna, columna);
    }

    return { minFila, maxFila, minColumna, maxColumna };
  }

  private normalizarIndice(valor: number | undefined, minimo: number): number {
    if (valor == null) {
      return 1;
    }

    return Math.max(1, valor - minimo + 1);
  }

  private colocarEnRejilla(
    x: number,
    y: number,
    width: number,
    height: number,
    fila: number,
    columna: number,
    totalFilas: number,
    totalColumnas: number,
    marginX = 16,
    marginY = 16
  ): { x: number; y: number } {
    const usableW = width - marginX * 2;
    const usableH = height - marginY * 2;

    const pasoX = totalColumnas > 1 ? usableW / (totalColumnas - 1) : 0;
    const pasoY = totalFilas > 1 ? usableH / (totalFilas - 1) : 0;

    return {
      x: x + marginX + (columna - 1) * pasoX,
      y: y + marginY + (fila - 1) * pasoY
    };
  }

  irAComprarEntradas(idEspectaculo: any): void {
    if (this.usaColaVirtual && !this.puedeComprar) {
      alert('Todavía no es tu turno para comprar.');
      return;
    }

    let idsEntradas: number[] = [];

    if (this.infoCompra?.modoSeleccion === 'ZONA') {
      if (this.zonaSeleccionada == null) {
        alert('Selecciona una zona antes de continuar.');
        return;
      }

      const entradasZona = this.entradasMapa
        .filter(e => e.zona === this.zonaSeleccionada && e.disponible)
        .map(e => e.idEntrada)
        .filter(id => id != null);

      if (entradasZona.length === 0) {
        alert('No hay entradas disponibles en la zona seleccionada.');
        return;
      }

      idsEntradas = [entradasZona[0]];
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