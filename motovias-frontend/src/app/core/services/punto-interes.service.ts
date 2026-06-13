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
  private readonly _actualizados = signal<PuntoInteres[]>([]);
  private readonly _eliminadosIds = signal<Set<number>>(new Set());

  readonly categoriasActivas = this._categoriasActivas.asReadonly();

  readonly puntosFiltrados = computed(() => {
    const activas = this._categoriasActivas();
    const eliminados = this._eliminadosIds();
    const actualizadosMap = new Map(this._actualizados().map((p) => [p.id, p]));
    const todos = [...this._todos(), ...this._nuevosReportes()]
      .filter((p) => !eliminados.has(p.id))
      .map((p) => actualizadosMap.get(p.id) ?? p);
    return todos.filter((p) => activas.includes(p.categoria));
  });

  upsertReporte(punto: PuntoInteres): void {
    const existeEnTodos = this._todos().some((p) => p.id === punto.id);
    const existeEnNuevos = this._nuevosReportes().some((p) => p.id === punto.id);

    if (existeEnTodos || existeEnNuevos) {
      this._actualizados.update((list) => [...list.filter((p) => p.id !== punto.id), punto]);
    } else {
      this._nuevosReportes.update((list) => [...list, punto]);
    }
  }

  eliminarReporte(id: number): void {
    this._eliminadosIds.update((set) => new Set([...set, id]));
    this._nuevosReportes.update((list) => list.filter((p) => p.id !== id));
    this._actualizados.update((list) => list.filter((p) => p.id !== id));
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
