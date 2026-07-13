import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Categoria } from '../models/punto-interes.model';

const BASE_URL = 'http://localhost:8080/api/admin/metricas';

export interface CategoriaConteo {
  categoria: Categoria;
  cantidad: number;
}

export interface ZonaActividad {
  latitud: number | null;
  longitud: number | null;
  cantidad: number;
}

export interface MetricasResponse {
  totalActivos: number;
  reportesUltimaSemana: number;
  usuariosNuevosMes: number;
  reportesPorCategoria: CategoriaConteo[];
  zonasConMasActividad: ZonaActividad[];
}

@Injectable({ providedIn: 'root' })
export class AdminMetricasService {
  private readonly http = inject(HttpClient);

  obtenerMetricas(): Observable<MetricasResponse> {
    return this.http.get<MetricasResponse>(BASE_URL);
  }
}
