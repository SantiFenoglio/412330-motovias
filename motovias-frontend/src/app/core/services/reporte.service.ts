import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { EstadoPunto, PuntoInteres, PuntoInteresRequest } from '../models/punto-interes.model';
import { ReporteEvento } from '../models/reporte-evento.model';

const BASE_URL = 'http://localhost:8080/api/reportes';

export interface ReporteUpdatePayload {
  descripcion: string;
  estado: EstadoPunto;
}

@Injectable({ providedIn: 'root' })
export class ReporteService {
  private readonly http = inject(HttpClient);

  getMisReportes(): Observable<PuntoInteres[]> {
    return this.http.get<PuntoInteres[]>(BASE_URL);
  }

  crear(request: PuntoInteresRequest): Observable<PuntoInteres> {
    return this.http.post<PuntoInteres>(BASE_URL, request);
  }

  updateReporte(id: number, data: ReporteUpdatePayload): Observable<PuntoInteres> {
    return this.http.put<PuntoInteres>(`${BASE_URL}/${id}`, data);
  }

  deleteReporte(id: number): Observable<void> {
    return this.http.delete<void>(`${BASE_URL}/${id}`);
  }

  votarReporte(id: number, tipoVoto: 'CONFIRMA' | 'REFUTA'): Observable<void> {
    return this.http.post<void>(`${BASE_URL}/${id}/votar`, { tipoVoto });
  }

  subirFotos(id: number, archivos: File[]): Observable<PuntoInteres> {
    const formData = new FormData();
    archivos.forEach((archivo) => formData.append('fotos', archivo));
    return this.http.post<PuntoInteres>(`${BASE_URL}/${id}/fotos`, formData);
  }

  obtenerHistorialReporte(id: number): Observable<ReporteEvento[]> {
    return this.http.get<ReporteEvento[]>(`${BASE_URL}/${id}/eventos`);
  }
}
