export interface EntradaMapaDto {
  idEntrada: number;
  idEspectaculo?: number;
  disponible: boolean;
  precio?: number;
  fila?: number;
  columna?: number;
  planta?: number;
  zona?: number;
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