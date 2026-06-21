export type TipoNotificacion = 'RECORDATORIO_CIERRE';

export interface NotificacionResponseDTO {
  id: number;
  mensaje: string;
  fechaCreacion: string;
  leida: boolean;
  tipo: TipoNotificacion;
  reporteId: number | null;
  reporteTitulo: string | null;
}
