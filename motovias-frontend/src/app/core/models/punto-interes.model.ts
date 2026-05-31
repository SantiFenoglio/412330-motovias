export type Categoria =
  | 'TALLER'
  | 'GOMERIA'
  | 'ALERTA_SOS'
  | 'PUNTO_INTERES';

export interface CategoriaConfig {
  label: string;
  color: string;
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
