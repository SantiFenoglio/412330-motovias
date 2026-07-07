import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Categoria, EstadoPunto } from '../models/punto-interes.model';

const BASE_URL = 'http://localhost:8080/api/admin/reportes';

export interface AdminReporteFiltro {
  estado?: EstadoPunto;
  categoria?: Categoria;
  fechaInicio?: string;
  fechaFin?: string;
  page?: number;
  size?: number;
}

export interface AdminReporteItem {
  id: number;
  titulo: string;
  categoria: Categoria;
  estado: EstadoPunto;
  nombreUsuarioCreador: string | null;
  emailUsuarioCreador: string | null;
  fechaCreacion: string;
  votos: number;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

@Injectable({ providedIn: 'root' })
export class AdminService {
  private readonly http = inject(HttpClient);

  listarReportes(filtro: AdminReporteFiltro): Observable<PageResponse<AdminReporteItem>> {
    let params = new HttpParams()
      .set('page', String(filtro.page ?? 0))
      .set('size', String(filtro.size ?? 10));

    if (filtro.estado) params = params.set('estado', filtro.estado);
    if (filtro.categoria) params = params.set('categoria', filtro.categoria);
    if (filtro.fechaInicio) params = params.set('fechaInicio', filtro.fechaInicio);
    if (filtro.fechaFin) params = params.set('fechaFin', filtro.fechaFin);

    return this.http.get<PageResponse<AdminReporteItem>>(BASE_URL, { params });
  }

  cambiarEstadoReporte(id: number, estado: EstadoPunto): Observable<AdminReporteItem> {
    return this.http.patch<AdminReporteItem>(`${BASE_URL}/${id}/estado`, { estado });
  }
}
