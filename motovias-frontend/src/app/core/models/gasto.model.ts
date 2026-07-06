export type CategoriaGasto = 'COMBUSTIBLE' | 'PEAJE' | 'ALOJAMIENTO' | 'OTRO';

export const CATEGORIA_LABELS: Record<CategoriaGasto, string> = {
  COMBUSTIBLE: 'Combustible',
  PEAJE: 'Peaje',
  ALOJAMIENTO: 'Alojamiento',
  OTRO: 'Otro',
};

export const CATEGORIA_OPTIONS: { value: CategoriaGasto; label: string }[] = (
  Object.entries(CATEGORIA_LABELS) as [CategoriaGasto, string][]
).map(([value, label]) => ({ value, label }));

export interface GastoRequestDTO {
  descripcion: string;
  monto: number;
  categoria: CategoriaGasto;
  pagadorId: number | null;
}

export interface GastoResponseDTO {
  id: number;
  descripcion: string;
  monto: number;
  categoria: CategoriaGasto;
  fechaCreacion: string;
  pagadorNombre: string;
  pagadorEmail: string;
}

export interface TransferenciaSimplificadaDTO {
  deudorNombre: string;
  deudorEmail: string;
  acreedorNombre: string;
  acreedorEmail: string;
  monto: number;
}

export interface ParticipanteGastoDTO {
  id: number;
  nombre: string;
  email: string;
}
