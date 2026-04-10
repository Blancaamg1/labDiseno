import { Component, OnInit, DestroyRef, inject } from '@angular/core';
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

@Component({
  selector: 'app-elegir-entradas',
  templateUrl: './elegir-entradas.html',
  standalone: true,
  imports: [CommonModule, RouterModule],
  styleUrls: ['./elegir-entradas.css']
})
export class ElegirEntradas implements OnInit {

  infoCompra: any;
  entradasMapa: EntradaMapaDto[] = [];

  butacas: ButacaSvg[] = [];
  zonas: ZonaResumen[] = [];

  idsButacasSeleccionadas = new Set<number>();
  zonaSeleccionada: number | null = null;

  private destroyRef = inject(DestroyRef);

  constructor(
    private route: ActivatedRoute,
    private reservasService: EspectaculosService,
    private router: Router
  ) { }

  ngOnInit(): void {
    const idEspectaculo = Number(this.route.snapshot.queryParamMap.get('idEspectaculo'));

    forkJoin({
      info: this.reservasService.getInfoCompra(idEspectaculo),
      entradas: this.reservasService.obtenerEntradasMapa(idEspectaculo)
    })
      .pipe(
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe({
        next: ({ info, entradas }) => {
          this.infoCompra = info;
          // Generar butacas de relleno
          const entradasCompletas = this.generarButacasCompletas((info as any).tipoMapa, entradas);
          this.entradasMapa = entradasCompletas;
          this.prepararMapa();
        },
        error: (err) => {
          console.error('Error al obtener los escenarios', err);
        }
      });
  }

  private generarButacasCompletas(tipoMapa: string, entradasConsulta: EntradaMapaDto[]): EntradaMapaDto[] {
    if (this.infoCompra?.modoSeleccion === 'ZONA') {
      return entradasConsulta ?? [];
    }

    const plantilla = this.crearPlantillaButacas(tipoMapa);

    if (!entradasConsulta || entradasConsulta.length === 0) {
      return plantilla;
    }

    const reales = new Map<string, EntradaMapaDto>();

    for (const entrada of entradasConsulta) {
      if (entrada.planta != null && entrada.fila != null && entrada.columna != null) {
        reales.set(this.claveButaca(entrada.planta, entrada.fila, entrada.columna), entrada);
      }
    }

    return plantilla.map(base => {
      const real = reales.get(this.claveButaca(base.planta!, base.fila!, base.columna!));

      if (!real) {
        return base; // por defecto disponible
      }

      return {
        ...base,
        ...real,
        disponible: real.disponible !== false
      };
    });
  }

  prepararMapa(): void {
    if (this.infoCompra.modoSeleccion === 'PRECISA') {
      this.butacas = [];

      for (const entrada of this.entradasMapa) {
        if (entrada.fila != null && entrada.columna != null && entrada.planta != null) {
          const pos = this.calcularPosicionButaca(entrada);
          this.butacas.push({
            ...entrada,
            x: pos.x,
            y: pos.y
          });
        }
      }
    } else {
      this.prepararZonas();
    }
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
    const fila = Math.max(1, entrada.fila ?? 1);
    const columna = Math.max(1, entrada.columna ?? 1);
    const planta = entrada.planta ?? 0;
    const pad = 20; // Padding interno mínimo

    switch (this.infoCompra.tipoMapa) {

      case 'AUDITORIO_PRINCIPAL': {
        if (planta === 0) {
          return this.colocarEnRejilla(70, 150, 240, 260, fila, columna, 8, 8, 26, 24);
        }

        if (planta === 1) {
          return this.colocarEnRejilla(330, 150, 240, 260, fila, columna, 8, 8, 26, 24);
        }

        if (planta === 2) {
          return this.colocarEnRejilla(590, 150, 240, 260, fila, columna, 8, 8, 26, 24);
        }

        return this.colocarEnRejilla(220, 470, 460, 85, fila, columna, 3, 10, 28, 18);
      }
      case 'TEATRO_CLASICO': {
        if (planta === 1) {
          return this.colocarEnRejilla(55, 180, 75, 120, fila, columna, 3, 2, 16, 16);
        }

        if (planta === 2) {
          return this.colocarEnRejilla(770, 180, 75, 120, fila, columna, 3, 2, 16, 16);
        }

        if (planta === 3) {
          return this.colocarEnRejilla(120, 160, 660, 245, fila, columna, 6, 12, 34, 26);
        }

        return this.colocarEnRejilla(170, 455, 560, 72, fila, columna, 2, 10, 28, 18);
      }

      case 'SALA_EXPERIMENTAL': {
        const filasTotales = 5;
        const columnasPorLado = 4;

        if (columna <= 4) {
          return this.colocarEnRejilla(195, 195, 190, 170, fila, columna, filasTotales, columnasPorLado, 0, 0);
        }

        return this.colocarEnRejilla(515, 195, 190, 170, fila, columna - 4, filasTotales, columnasPorLado, 0, 0);
      }

      default:
        return { x: 100, y: 100 };
    }
  }

  toggleButaca(butaca: ButacaSvg): void {
    if (!butaca.disponible) {
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
    const zonaInfo = this.zonas.find(z => z.zona === zona);

    if (!zonaInfo || zonaInfo.disponibles <= 0) {
      return;
    }

    if (this.zonaSeleccionada === zona) {
      this.zonaSeleccionada = null;
    } else {
      this.zonaSeleccionada = zona;
    }
  }

  disponiblesEnZona(zona: number): number {
    const encontrada = this.zonas.find(z => z.zona === zona);
    return encontrada ? encontrada.disponibles : 0;
  }

  private crearPlantillaButacas(tipoMapa: string): EntradaMapaDto[] {
    let configGrillas: { planta: number; filas: number; columnas: number }[] = [];

    if (tipoMapa === 'AUDITORIO_PRINCIPAL') {
      configGrillas = [
        { planta: 0, filas: 8, columnas: 7 },
        { planta: 1, filas: 8, columnas: 7 },
        { planta: 2, filas: 8, columnas: 7 },
        { planta: 3, filas: 3, columnas: 12 }
      ];
    } else if (tipoMapa === 'TEATRO_CLASICO') {
      configGrillas = [
        { planta: 1, filas: 3, columnas: 2 },
        { planta: 2, filas: 3, columnas: 2 },
        { planta: 3, filas: 5, columnas: 6 },
        { planta: 4, filas: 2, columnas: 7 }
      ];
    } else if (tipoMapa === 'SALA_EXPERIMENTAL') {
      configGrillas = [
        { planta: 0, filas: 5, columnas: 8 } // 40 butacas en total
      ];
    } else {
      configGrillas = [{ planta: 0, filas: 10, columnas: 10 }];
    }

    const plantilla: EntradaMapaDto[] = [];
    let id = 1;

    for (const config of configGrillas) {
      for (let fila = 1; fila <= config.filas; fila++) {
        for (let columna = 1; columna <= config.columnas; columna++) {
          plantilla.push({
            idEntrada: id++,
            disponible: true,
            fila,
            columna,
            planta: config.planta
          });
        }
      }
    }

    return plantilla;
  }

  private claveButaca(planta: number, fila: number, columna: number): string {
    return `${planta}-${fila}-${columna}`;
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
  const idsEntradas = Array.from(this.idsButacasSeleccionadas);

  this.router.navigate(['/comprar'], {
    queryParams: {
      idEspectaculo: idEspectaculo,
      idsEntradas: idsEntradas.join(',')
    }
  });
}
}