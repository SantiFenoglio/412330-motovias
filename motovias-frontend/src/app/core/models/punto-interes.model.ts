export type Categoria =
  | 'TALLER'
  | 'GOMERIA'
  | 'ALERTA_SOS'
  | 'PUNTO_INTERES';

export type EstadoPunto = 'ACTIVO' | 'RESUELTO' | 'DUDOSO';

export interface CategoriaConfig {
  label: string;
  color: string;
}

export interface EstadoConfig {
  label: string;
  color: string;
  bg: string;
}

export const TODAS_LAS_CATEGORIAS: Categoria[] = [
  'TALLER',
  'GOMERIA',
  'ALERTA_SOS',
  'PUNTO_INTERES',
];

export const CATEGORY_CONFIG: Record<Categoria, CategoriaConfig> = {
  TALLER:        { label: 'Taller mecánico',             color: '#2563eb' },
  GOMERIA:       { label: 'Gomería',                     color: '#16a34a' },
  ALERTA_SOS:    { label: 'Emergencia S.O.S.',           color: '#dc2626' },
  PUNTO_INTERES: { label: 'Punto de interés / Parador',  color: '#ea580c' },
};

export const ESTADO_CONFIG: Record<EstadoPunto, EstadoConfig> = {
  ACTIVO:   { label: 'Activo',   color: '#15803d', bg: '#dcfce7' },
  RESUELTO: { label: 'Resuelto', color: '#1d4ed8', bg: '#dbeafe' },
  DUDOSO:   { label: 'Dudoso',   color: '#b45309', bg: '#fef3c7' },
};

export interface PuntoInteres {
  id: number;
  titulo: string;
  descripcion: string;
  latitud: number;
  longitud: number;
  categoria: Categoria;
  estado?: EstadoPunto;
  fechaCreacion?: string;
  emailUsuario?: string;
  nombreUsuario?: string;
}

export interface PuntoInteresRequest {
  titulo: string;
  descripcion: string;
  latitud: number;
  longitud: number;
  categoria: Categoria;
}
