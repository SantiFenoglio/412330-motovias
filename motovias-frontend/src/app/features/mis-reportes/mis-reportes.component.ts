import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { ReporteService } from '../../core/services/reporte.service';
import {
  CATEGORY_CONFIG,
  ESTADO_CONFIG,
  PuntoInteres,
} from '../../core/models/punto-interes.model';

@Component({
  selector: 'app-mis-reportes',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DatePipe],
  template: `
    <div class="page-shell">
      <header class="page-header">
        <h1 class="page-title">
          <i class="pi pi-list" aria-hidden="true"></i>
          Mis Reportes
        </h1>
        <p class="page-subtitle">Historial de puntos de interés que creaste</p>
      </header>

      @if (loading()) {
        <div class="loading-state" role="status" aria-live="polite">
          <i class="pi pi-spin pi-spinner loading-icon" aria-hidden="true"></i>
          <p>Cargando tus reportes…</p>
        </div>
      } @else if (error()) {
        <div class="error-state" role="alert">
          <i class="pi pi-exclamation-circle error-icon" aria-hidden="true"></i>
          <p class="error-title">No se pudieron cargar los reportes</p>
          <button class="retry-btn" (click)="cargar()">Reintentar</button>
        </div>
      } @else if (reportes().length === 0) {
        <div class="empty-state" role="status" aria-live="polite">
          <i class="pi pi-inbox empty-icon" aria-hidden="true"></i>
          <p class="empty-title">Aún no creaste reportes</p>
          <p class="empty-body">
            Los puntos de interés, alertas y talleres que registres aparecerán aquí.
          </p>
        </div>
      } @else {
        <ul class="reporte-list" aria-label="Mis reportes">
          @for (r of reportes(); track r.id) {
            <li class="reporte-card">
              <div class="card-header">
                <span
                  class="badge categoria"
                  [style.background-color]="categoriaColor(r)"
                >{{ categoriaLabel(r) }}</span>
                @if (r.estado) {
                  <span
                    class="badge estado"
                    [style.color]="estadoColor(r)"
                    [style.background-color]="estadoBg(r)"
                  >{{ estadoLabel(r) }}</span>
                }
              </div>
              <h2 class="card-title">{{ r.titulo }}</h2>
              @if (r.descripcion) {
                <p class="card-desc">{{ r.descripcion }}</p>
              }
              @if (r.fechaCreacion) {
                <p class="card-date">
                  <i class="pi pi-calendar" aria-hidden="true"></i>
                  {{ r.fechaCreacion | date:'dd/MM/yyyy HH:mm' }}
                </p>
              }
            </li>
          }
        </ul>
      }
    </div>
  `,
  styles: [`
    .page-shell {
      max-width: 40rem;
      margin: 0 auto;
      padding: 2rem 1.25rem;
    }

    .page-header { margin-bottom: 2.5rem; }

    .page-title {
      display: flex;
      align-items: center;
      gap: 0.625rem;
      font-size: 1.5rem;
      font-weight: 700;
      color: #0f172a;
      margin: 0 0 0.375rem;
    }
    .page-title .pi { color: #3b82f6; font-size: 1.375rem; }

    .page-subtitle { font-size: 0.9375rem; color: #64748b; margin: 0; }

    /* loading */
    .loading-state {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 1rem;
      padding: 3rem;
      color: #64748b;
    }
    .loading-icon { font-size: 2.5rem; }

    /* error */
    .error-state {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 0.75rem;
      padding: 3rem;
      color: #dc2626;
      text-align: center;
    }
    .error-icon { font-size: 2.5rem; }
    .error-title { font-weight: 600; margin: 0; }
    .retry-btn {
      margin-top: 0.5rem;
      padding: 0.5rem 1.25rem;
      border: none;
      border-radius: 6px;
      background: #3b82f6;
      color: #fff;
      font-size: 0.875rem;
      cursor: pointer;
    }
    .retry-btn:hover { background: #2563eb; }

    /* empty */
    .empty-state {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 0.75rem;
      padding: 3.5rem 1.5rem;
      border: 1.5px dashed #cbd5e1;
      border-radius: 12px;
      text-align: center;
      color: #94a3b8;
    }
    .empty-icon { font-size: 3rem; }
    .empty-title { font-size: 1rem; font-weight: 600; color: #475569; margin: 0; }
    .empty-body { font-size: 0.875rem; max-width: 22rem; line-height: 1.5; margin: 0; }

    /* list */
    .reporte-list { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: 1rem; }

    .reporte-card {
      padding: 1.25rem 1.5rem;
      border: 1px solid #e2e8f0;
      border-radius: 12px;
      background: #fff;
      box-shadow: 0 1px 3px rgb(0 0 0 / .06);
    }

    .card-header { display: flex; gap: 0.5rem; flex-wrap: wrap; margin-bottom: 0.75rem; }

    .badge {
      display: inline-block;
      padding: 0.2rem 0.6rem;
      border-radius: 999px;
      font-size: 0.75rem;
      font-weight: 600;
      color: #fff;
    }
    .badge.estado { border: 1px solid currentColor; }

    .card-title { font-size: 1rem; font-weight: 600; color: #0f172a; margin: 0 0 0.375rem; }

    .card-desc { font-size: 0.875rem; color: #475569; margin: 0 0 0.5rem; line-height: 1.5; }

    .card-date {
      display: flex;
      align-items: center;
      gap: 0.375rem;
      font-size: 0.8125rem;
      color: #94a3b8;
      margin: 0;
    }
  `],
})
export class MisReportesComponent implements OnInit {
  private readonly reporteService = inject(ReporteService);

  readonly reportes = signal<PuntoInteres[]>([]);
  readonly loading = signal(false);
  readonly error = signal(false);

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.loading.set(true);
    this.error.set(false);
    this.reporteService.getMisReportes().subscribe({
      next: (data) => {
        this.reportes.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.error.set(true);
        this.loading.set(false);
      },
    });
  }

  categoriaLabel(r: PuntoInteres): string {
    return CATEGORY_CONFIG[r.categoria]?.label ?? r.categoria;
  }

  categoriaColor(r: PuntoInteres): string {
    return CATEGORY_CONFIG[r.categoria]?.color ?? '#64748b';
  }

  estadoLabel(r: PuntoInteres): string {
    return r.estado ? (ESTADO_CONFIG[r.estado]?.label ?? r.estado) : '';
  }

  estadoColor(r: PuntoInteres): string {
    return r.estado ? (ESTADO_CONFIG[r.estado]?.color ?? '#64748b') : '';
  }

  estadoBg(r: PuntoInteres): string {
    return r.estado ? (ESTADO_CONFIG[r.estado]?.bg ?? 'transparent') : 'transparent';
  }
}
