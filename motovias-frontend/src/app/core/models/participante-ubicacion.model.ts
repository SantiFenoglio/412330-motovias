export interface ParticipanteUbicacionDTO {
  email: string;
  nombre: string;
  latitud: number | null;
  longitud: number | null;
  conectado: boolean;
}
