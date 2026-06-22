import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ViajeRequest, ViajeResponse } from '../models/viaje.model';

const BASE_URL = 'http://localhost:8080';

@Injectable({ providedIn: 'root' })
export class ViajeService {
  private readonly http = inject(HttpClient);

  crearViaje(dto: ViajeRequest): Observable<ViajeResponse> {
    return this.http.post<ViajeResponse>(`${BASE_URL}/api/viajes`, dto);
  }

  obtenerViajePorCodigo(codigo: string): Observable<ViajeResponse> {
    return this.http.get<ViajeResponse>(`${BASE_URL}/api/viajes/${codigo}`);
  }

  unirseAViaje(codigo: string): Observable<ViajeResponse> {
    return this.http.post<ViajeResponse>(`${BASE_URL}/api/viajes/${codigo}/unirse`, {});
  }
}
