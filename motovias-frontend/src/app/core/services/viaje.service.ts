import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ParticipanteResponse, ViajeRequest, ViajeResponse } from '../models/viaje.model';

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

  obtenerViajeActivo(): Observable<ViajeResponse | null> {
    return this.http.get<ViajeResponse | null>(`${BASE_URL}/api/viajes/activo`);
  }

  listarParticipantes(viajeId: number): Observable<ParticipanteResponse[]> {
    return this.http.get<ParticipanteResponse[]>(`${BASE_URL}/api/viajes/${viajeId}/participantes`);
  }

  unirseAViaje(codigo: string): Observable<ViajeResponse> {
    return this.http.post<ViajeResponse>(`${BASE_URL}/api/viajes/${codigo}/unirse`, {});
  }

  salirDeViaje(codigo: string): Observable<void> {
    return this.http.delete<void>(`${BASE_URL}/api/viajes/${codigo}/salir`);
  }

  eliminarViaje(codigo: string): Observable<void> {
    return this.http.delete<void>(`${BASE_URL}/api/viajes/${codigo}`);
  }
}
