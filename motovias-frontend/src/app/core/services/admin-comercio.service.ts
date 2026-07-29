import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ComercioVerificado, ComercioVerificadoRequest } from '../models/comercio-verificado.model';

const BASE_URL = 'http://localhost:8080/api/admin/comercios';

@Injectable({ providedIn: 'root' })
export class AdminComercioService {
  private readonly http = inject(HttpClient);

  listar(): Observable<ComercioVerificado[]> {
    return this.http.get<ComercioVerificado[]>(BASE_URL);
  }

  crear(request: ComercioVerificadoRequest): Observable<ComercioVerificado> {
    return this.http.post<ComercioVerificado>(BASE_URL, request);
  }

  editar(id: number, request: ComercioVerificadoRequest): Observable<ComercioVerificado> {
    return this.http.put<ComercioVerificado>(`${BASE_URL}/${id}`, request);
  }

  cambiarEstado(id: number, activo: boolean): Observable<ComercioVerificado> {
    return this.http.patch<ComercioVerificado>(`${BASE_URL}/${id}/estado`, { activo });
  }
}
