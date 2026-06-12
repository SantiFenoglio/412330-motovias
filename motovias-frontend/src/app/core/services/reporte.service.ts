import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { PuntoInteres, PuntoInteresRequest } from '../models/punto-interes.model';

const BASE_URL = 'http://localhost:8080/api/reportes';

@Injectable({ providedIn: 'root' })
export class ReporteService {
  private readonly http = inject(HttpClient);

  crear(request: PuntoInteresRequest): Observable<PuntoInteres> {
    return this.http.post<PuntoInteres>(BASE_URL, request);
  }
}
