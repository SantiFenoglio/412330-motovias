import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  GastoRequestDTO,
  GastoResponseDTO,
  ParticipanteGastoDTO,
  PreferenciaPagoRequestDTO,
  PreferenciaPagoResponseDTO,
  TransferenciaSimplificadaDTO,
} from '../models/gasto.model';

const BASE_URL = 'http://localhost:8080';

@Injectable({ providedIn: 'root' })
export class GastoService {
  private readonly http = inject(HttpClient);

  registrarGasto(viajeId: number, gasto: GastoRequestDTO): Observable<GastoResponseDTO> {
    return this.http.post<GastoResponseDTO>(`${BASE_URL}/api/viajes/${viajeId}/gastos`, gasto);
  }

  obtenerHistorialGastos(viajeId: number): Observable<GastoResponseDTO[]> {
    return this.http.get<GastoResponseDTO[]>(`${BASE_URL}/api/viajes/${viajeId}/gastos`);
  }

  obtenerBalanceSimplificado(viajeId: number): Observable<TransferenciaSimplificadaDTO[]> {
    return this.http.get<TransferenciaSimplificadaDTO[]>(
      `${BASE_URL}/api/viajes/${viajeId}/gastos/balance`,
    );
  }

  obtenerParticipantes(viajeId: number): Observable<ParticipanteGastoDTO[]> {
    return this.http.get<ParticipanteGastoDTO[]>(
      `${BASE_URL}/api/viajes/${viajeId}/participantes`,
    );
  }

  crearPreferenciaPago(
    viajeId: number,
    preferencia: PreferenciaPagoRequestDTO,
  ): Observable<PreferenciaPagoResponseDTO> {
    return this.http.post<PreferenciaPagoResponseDTO>(
      `${BASE_URL}/api/viajes/${viajeId}/gastos/preferencia`,
      preferencia,
    );
  }
}
