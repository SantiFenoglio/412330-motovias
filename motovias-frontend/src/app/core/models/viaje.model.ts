export interface ViajeRequest {
  titulo: string;
  descripcion?: string;
}

export interface ViajeResponse {
  id: number;
  titulo: string;
  descripcion?: string;
  fechaCreacion: string;
  codigo: string;
  organizadorNombre: string;
  organizadorEmail: string;
}
