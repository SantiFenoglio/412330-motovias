export type Categoria =
  | 'ACCIDENTE'
  | 'OBRA'
  | 'TRAMPA_POLICIAL'
  | 'SEMAFORO_ROTO'
  | 'PIQUETE'
  | 'PELIGRO';

export interface PuntoInteres {
  id: number;
  titulo: string;
  descripcion: string;
  latitud: number;
  longitud: number;
  categoria: Categoria;
  emailUsuario?: string;
}

export interface PuntoInteresRequest {
  titulo: string;
  descripcion: string;
  latitud: number;
  longitud: number;
  categoria: Categoria;
}
