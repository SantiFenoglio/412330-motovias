import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { toSignal } from '@angular/core/rxjs-interop';
import { catchError, Observable, of } from 'rxjs';
import { ComercioVerificado } from '../models/comercio-verificado.model';

const BASE_URL = 'http://localhost:8080/api/comercios';

@Injectable({ providedIn: 'root' })
export class ComercioService {
  private readonly http = inject(HttpClient);

  readonly comerciosActivos = toSignal(
    this.http.get<ComercioVerificado[]>(`${BASE_URL}/activos`).pipe(catchError(() => of([]))),
    { initialValue: [] as ComercioVerificado[] },
  );

  listarActivos(): Observable<ComercioVerificado[]> {
    return this.http.get<ComercioVerificado[]>(`${BASE_URL}/activos`);
  }
}
