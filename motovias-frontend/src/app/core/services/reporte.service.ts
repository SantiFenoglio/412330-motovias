import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { EstadoPunto, PuntoInteres, PuntoInteresRequest } from '../models/punto-interes.model';

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
}
