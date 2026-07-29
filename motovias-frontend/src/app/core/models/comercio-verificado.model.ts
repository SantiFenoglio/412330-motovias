export type CategoriaComercio = 'TALLER_MECANICO' | 'GOMERIA' | 'REPUESTOS';

export interface CategoriaComercioConfig {
  label: string;
  color: string;
}

export const TODAS_LAS_CATEGORIAS_COMERCIO: CategoriaComercio[] = [
  'TALLER_MECANICO',
  'GOMERIA',
  'REPUESTOS',
];

export const CATEGORIA_COMERCIO_CONFIG: Record<CategoriaComercio, CategoriaComercioConfig> = {
  TALLER_MECANICO: { label: 'Taller mecánico', color: '#b45309' },
  GOMERIA:         { label: 'Gomería',         color: '#0f766e' },
  REPUESTOS:       { label: 'Repuestos',       color: '#7c3aed' },
};

export interface ComercioVerificado {
  id: number;
  nombre: string;
  direccion: string;
  telefono?: string;
  categoria: CategoriaComercio;
  latitud: number;
  longitud: number;
  activo: boolean;
  verificado: boolean;
  fechaAlta?: string;
  fechaModificacion?: string;
}

export interface ComercioVerificadoRequest {
  nombre: string;
  direccion: string;
  telefono?: string;
  categoria: CategoriaComercio;
  latitud: number;
  longitud: number;
}
