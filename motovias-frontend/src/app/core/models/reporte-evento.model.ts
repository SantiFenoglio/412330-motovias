export type TipoEventoReporte = 'CREACION' | 'EDICION' | 'VOTO' | 'CAMBIO_ESTADO';

export interface TipoEventoConfig {
  label: string;
  icon: string;
}

export const TIPO_EVENTO_CONFIG: Record<TipoEventoReporte, TipoEventoConfig> = {
  CREACION: { label: 'Creación', icon: 'pi pi-plus-circle' },
  EDICION: { label: 'Edición', icon: 'pi pi-pencil' },
  VOTO: { label: 'Voto', icon: 'pi pi-thumbs-up' },
  CAMBIO_ESTADO: { label: 'Cambio de estado', icon: 'pi pi-refresh' },
};

export interface ReporteEvento {
  id: number;
  tipoEvento: TipoEventoReporte;
  descripcion: string;
  nombreUsuario?: string;
  timestamp: string;
}
