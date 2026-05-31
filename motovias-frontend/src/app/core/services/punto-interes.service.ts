import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Categoria, PuntoInteres, PuntoInteresRequest } from '../models/punto-interes.model';

const BASE_URL = 'http://localhost:8080/api/puntos-interes';

@Injectable({ providedIn: 'root' })
export class PuntoInteresService {
  private readonly http = inject(HttpClient);

  listarTodos(): Observable<PuntoInteres[]> {
    return this.http.get<PuntoInteres[]>(BASE_URL);
  }

  buscarCercanos(lat: number, lon: number, radio: number): Observable<PuntoInteres[]> {
    return this.http.get<PuntoInteres[]>(`${BASE_URL}/cercanos`, {
      params: { lat, lon, radio },
    });
  }

  crear(request: PuntoInteresRequest): Observable<PuntoInteres> {
    return this.http.post<PuntoInteres>(BASE_URL, request);
  }
}
