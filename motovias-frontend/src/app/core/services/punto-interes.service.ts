import { computed, inject, Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { toSignal } from '@angular/core/rxjs-interop';
import { catchError, Observable, of } from 'rxjs';
import {
  Categoria,
  PuntoInteres,
  PuntoInteresRequest,
  TODAS_LAS_CATEGORIAS,
} from '../models/punto-interes.model';

const BASE_URL = 'http://localhost:8080/api/puntos-interes';

@Injectable({ providedIn: 'root' })
export class PuntoInteresService {
  private readonly http = inject(HttpClient);

  private readonly _todos = toSignal(
    this.http.get<PuntoInteres[]>(BASE_URL).pipe(catchError(() => of([]))),
    { initialValue: [] as PuntoInteres[] },
  );

  private readonly _categoriasActivas = signal<Categoria[]>([...TODAS_LAS_CATEGORIAS]);
  private readonly _nuevosReportes = signal<PuntoInteres[]>([]);

  readonly categoriasActivas = this._categoriasActivas.asReadonly();

  readonly puntosFiltrados = computed(() => {
    const activas = this._categoriasActivas();
    const todos = [...this._todos(), ...this._nuevosReportes()];
    return todos.filter((p) => activas.includes(p.categoria));
  });

  agregarNuevoReporte(punto: PuntoInteres): void {
    this._nuevosReportes.update((list) => {
      const yaExiste =
        list.some((p) => p.id === punto.id) ||
        this._todos().some((p) => p.id === punto.id);
      return yaExiste ? list : [...list, punto];
    });
  }

  toggleCategoria(cat: Categoria): void {
    this._categoriasActivas.update((current) =>
      current.includes(cat) ? current.filter((c) => c !== cat) : [...current, cat],
    );
  }

  esCategoriaActiva(cat: Categoria): boolean {
    return this._categoriasActivas().includes(cat);
  }

  getById(id: number): Observable<PuntoInteres> {
    return this.http.get<PuntoInteres>(`${BASE_URL}/${id}`);
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
