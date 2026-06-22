import {
  ChangeDetectionStrategy,
  Component,
  inject,
  OnInit,
  signal,
} from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { DatePipe } from '@angular/common';
import { MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { Toast } from 'primeng/toast';
import { ViajeService } from '../../../core/services/viaje.service';
import { ViajeResponse } from '../../../core/models/viaje.model';

@Component({
  selector: 'app-viaje-detail',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [MessageService],
  imports: [DatePipe, ButtonModule, Toast],
  template: `
    @if (loading()) {
      <div class="loading-state" role="status" aria-live="polite">
        <i class="pi pi-spin pi-spinner loading-icon" aria-hidden="true"></i>
        <p>Cargando caravana…</p>
      </div>
    } @else if (error()) {
      <div class="error-state" role="alert">
        <i class="pi pi-exclamation-circle error-icon" aria-hidden="true"></i>
        <p class="error-msg">No se pudo cargar la caravana. Verificá el código e intentá de nuevo.</p>
      </div>
    } @else if (viaje(); as v) {
      <div class="page-shell">

        <header class="page-header">
          <h1 class="viaje-title">{{ v.titulo }}</h1>
          @if (v.descripcion) {
            <p class="viaje-desc">{{ v.descripcion }}</p>
          }
        </header>

        <div class="meta-grid" aria-label="Detalles de la caravana">
          <div class="meta-item">
            <span class="meta-label">
              <i class="pi pi-user" aria-hidden="true"></i>
              Organizador
            </span>
            <span class="meta-value">{{ v.organizadorNombre }}</span>
          </div>
          <div class="meta-item">
            <span class="meta-label">
              <i class="pi pi-calendar" aria-hidden="true"></i>
              Creada el
            </span>
            <span class="meta-value">{{ v.fechaCreacion | date:'dd/MM/yyyy HH:mm' }}</span>
          </div>
        </div>

        <section class="code-section" aria-label="Código de la caravana">
          <p class="code-section__label">Código de la caravana</p>
          <p class="code-section__hint">Compartí este código con quienes quieran unirse</p>

          <div class="code-row">
            <div
              class="code-box"
              role="text"
              [attr.aria-label]="'Código: ' + v.codigo"
            >
              {{ v.codigo }}
            </div>

            <p-button
              icon="pi pi-copy"
              severity="secondary"
              [text]="true"
              size="large"
              (onClick)="copiarCodigo(v.codigo)"
              ariaLabel="Copiar código de la caravana"
            />
          </div>
        </section>

      </div>
    }

    <p-toast position="bottom-center" />
  `,
  styles: [`
    .loading-state,
    .error-state {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 1rem;
      padding: 4rem 1.5rem;
      color: #64748b;
      text-align: center;
    }
    .loading-icon,
    .error-icon { font-size: 2.5rem; }
    .error-state { color: #dc2626; }
    .error-msg { font-size: 0.9375rem; max-width: 22rem; line-height: 1.5; margin: 0; }

    .page-shell {
      max-width: 38rem;
      margin: 0 auto;
      padding: 2rem 1.25rem;
    }

    .page-header { margin-bottom: 2rem; }

    .viaje-title {
      font-size: 1.75rem;
      font-weight: 800;
      color: #0f172a;
      margin: 0 0 0.625rem;
      line-height: 1.2;
    }

    .viaje-desc {
      font-size: 0.9375rem;
      color: #475569;
      margin: 0;
      line-height: 1.6;
    }

    /* ── Metadata ──────────────────────────────────────────── */
    .meta-grid {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 1rem;
      margin-bottom: 2.5rem;
      padding: 1.25rem;
      background: #f8fafc;
      border: 1px solid #e2e8f0;
      border-radius: 12px;
    }

    @media (max-width: 480px) {
      .meta-grid { grid-template-columns: 1fr; }
    }

    .meta-item {
      display: flex;
      flex-direction: column;
      gap: 0.25rem;
    }

    .meta-label {
      display: flex;
      align-items: center;
      gap: 0.375rem;
      font-size: 0.75rem;
      font-weight: 600;
      text-transform: uppercase;
      letter-spacing: 0.05em;
      color: #94a3b8;
    }

    .meta-value {
      font-size: 0.9375rem;
      font-weight: 600;
      color: #1e293b;
    }

    /* ── Código ────────────────────────────────────────────── */
    .code-section {
      padding: 1.5rem 1.75rem;
      background: #fff;
      border: 1.5px solid #e2e8f0;
      border-radius: 16px;
      box-shadow: 0 2px 8px rgb(0 0 0 / .06);
    }

    .code-section__label {
      font-size: 0.8125rem;
      font-weight: 700;
      text-transform: uppercase;
      letter-spacing: 0.07em;
      color: #64748b;
      margin: 0 0 0.25rem;
    }

    .code-section__hint {
      font-size: 0.8125rem;
      color: #94a3b8;
      margin: 0 0 1.25rem;
    }

    .code-row {
      display: flex;
      align-items: center;
      gap: 0.5rem;
    }

    .code-box {
      font-family: monospace;
      font-size: 2.25rem;
      font-weight: 800;
      letter-spacing: 0.3em;
      color: #1e293b;
      background: #f1f5f9;
      border: 2px solid #e2e8f0;
      border-radius: 12px;
      padding: 0.75rem 1.25rem;
      user-select: all;
      cursor: text;
      line-height: 1;
    }
  `],
})
export class ViajeDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly viajeService = inject(ViajeService);
  private readonly messageService = inject(MessageService);

  readonly loading = signal(true);
  readonly error = signal(false);
  readonly viaje = signal<ViajeResponse | null>(null);

  ngOnInit(): void {
    const codigo = this.route.snapshot.paramMap.get('codigo') ?? '';
    this.viajeService.obtenerViajePorCodigo(codigo).subscribe({
      next: (v) => {
        this.viaje.set(v);
        this.loading.set(false);
      },
      error: () => {
        this.error.set(true);
        this.loading.set(false);
      },
    });
  }

  copiarCodigo(codigo: string): void {
    navigator.clipboard.writeText(codigo).then(
      () => {
        this.messageService.add({
          severity: 'success',
          summary: '¡Código copiado!',
          detail: 'Compartilo con tus compañeros de ruta.',
          life: 4000,
        });
      },
      () => {
        this.messageService.add({
          severity: 'warn',
          summary: 'No se pudo copiar',
          detail: `Copiá el código manualmente: ${codigo}`,
          life: 5000,
        });
      },
    );
  }
}
