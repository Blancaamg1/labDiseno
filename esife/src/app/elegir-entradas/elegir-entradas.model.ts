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