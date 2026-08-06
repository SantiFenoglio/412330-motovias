import { inject, Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { catchError, Observable, of } from 'rxjs';
import { ComercioVerificado } from '../models/comercio-verificado.model';
import { ComercioWebSocketService } from './comercio-websocket.service';

const BASE_URL = 'http://localhost:8080/api/comercios';

@Injectable({ providedIn: 'root' })
export class ComercioService {
  private readonly http = inject(HttpClient);
  private readonly wsService = inject(ComercioWebSocketService);

  readonly comerciosActivos = signal<ComercioVerificado[]>([]);

  constructor() {
    this.listarActivos().subscribe((comercios) => this.comerciosActivos.set(comercios));

    // Canal /topic/comercios: altas, ediciones y cambios de estado del admin, en tiempo real.
    this.wsService.comercioUpserted$.subscribe((comercio) => {
      this.comerciosActivos.update((actuales) => {
        const sinComercio = actuales.filter((c) => c.id !== comercio.id);
        return comercio.activo ? [comercio, ...sinComercio] : sinComercio;
      });
    });
  }

  listarActivos(): Observable<ComercioVerificado[]> {
    return this.http.get<ComercioVerificado[]>(`${BASE_URL}/activos`).pipe(
      catchError(() => of([] as ComercioVerificado[])),
    );
  }
}
