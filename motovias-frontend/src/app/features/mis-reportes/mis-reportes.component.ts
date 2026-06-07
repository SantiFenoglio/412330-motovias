import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
  selector: 'app-mis-reportes',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="page-shell">
      <header class="page-header">
        <h1 class="page-title">
          <i class="pi pi-list" aria-hidden="true"></i>
          Mis Reportes
        </h1>
        <p class="page-subtitle">Historial de puntos de interés que creaste</p>
      </header>

      <div class="empty-state" role="status" aria-live="polite">
        <i class="pi pi-inbox empty-icon" aria-hidden="true"></i>
        <p class="empty-title">Aún no creaste reportes</p>
        <p class="empty-body">
          Los puntos de interés, alertas y talleres que registres aparecerán aquí.
        </p>
      </div>
    </div>
  `,
  styles: [`
    .page-shell {
      max-width: 40rem;
      margin: 0 auto;
      padding: 2rem 1.25rem;
    }

    .page-header {
      margin-bottom: 2.5rem;
    }

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

    .page-subtitle {
      font-size: 0.9375rem;
      color: #64748b;
      margin: 0;
    }

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

    .empty-title {
      font-size: 1rem;
      font-weight: 600;
      color: #475569;
      margin: 0;
    }

    .empty-body {
      font-size: 0.875rem;
      max-width: 22rem;
      line-height: 1.5;
      margin: 0;
    }
  `],
})
export class MisReportesComponent {}
