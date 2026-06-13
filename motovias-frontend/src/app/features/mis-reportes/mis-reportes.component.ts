import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { DatePipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ConfirmationService, MessageService, PrimeTemplate } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ConfirmDialog } from 'primeng/confirmdialog';
import { Dialog } from 'primeng/dialog';
import { Select } from 'primeng/select';
import { TextareaModule } from 'primeng/textarea';
import { Toast } from 'primeng/toast';
import { ReporteService } from '../../core/services/reporte.service';
import { ReporteWebSocketService } from '../../core/services/reporte-websocket.service';
import {
  CATEGORY_CONFIG,
  ESTADO_CONFIG,
  EstadoPunto,
  PuntoInteres,
} from '../../core/models/punto-interes.model';

interface EstadoOption {
  label: string;
  value: EstadoPunto;
}

@Component({
  selector: 'app-mis-reportes',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [ConfirmationService, MessageService],
  imports: [
    DatePipe,
    ReactiveFormsModule,
    ButtonModule,
    Dialog,
    ConfirmDialog,
    Select,
    TextareaModule,
    Toast,
    PrimeTemplate,
  ],
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
              <div class="card-actions" role="group" [attr.aria-label]="'Acciones de ' + r.titulo">
                <p-button
                  icon="pi pi-pencil"
                  label="Editar"
                  [text]="true"
                  size="small"
                  severity="secondary"
                  (onClick)="onEditClick(r)"
                  [ariaLabel]="'Editar ' + r.titulo"
                />
                <p-button
                  icon="pi pi-trash"
                  label="Eliminar"
                  [text]="true"
                  size="small"
                  severity="danger"
                  (onClick)="onDeleteClick(r)"
                  [ariaLabel]="'Eliminar ' + r.titulo"
                />
              </div>
            </li>
          }
        </ul>
      }

      <!-- ── Diálogo de edición ─────────────────────────────────────────── -->
      <p-dialog
        [header]="'Editar: ' + (editandoReporte()?.titulo ?? '')"
        [visible]="editandoReporte() !== null"
        (onHide)="cancelarEdicion()"
        [modal]="true"
        [draggable]="false"
        [resizable]="false"
        [style]="{ width: '90vw', 'max-width': '500px' }"
        [contentStyle]="{ overflow: 'visible' }"
        role="dialog"
        aria-live="polite"
      >
        <form
          id="mr-edit-form"
          [formGroup]="editForm"
          (ngSubmit)="guardarEdicion()"
          class="edit-form"
        >
          <div class="edit-field">
            <label for="mr-descripcion" class="edit-label">Descripción</label>
            <textarea
              pInputTextarea
              id="mr-descripcion"
              formControlName="descripcion"
              [rows]="4"
              [autoResize]="true"
              style="width:100%; overflow:hidden; resize:none"
              aria-label="Descripción del reporte"
            ></textarea>
          </div>

          <div class="edit-field">
            <label for="mr-estado" class="edit-label">
              Estado <span aria-hidden="true">*</span>
            </label>
            <p-select
              id="mr-estado"
              formControlName="estado"
              [options]="estadoOptions"
              optionLabel="label"
              optionValue="value"
              placeholder="Seleccioná un estado"
              [style]="{ width: '100%' }"
              aria-required="true"
              [attr.aria-invalid]="editForm.get('estado')?.invalid && editForm.get('estado')?.touched"
            />
            @if (editForm.get('estado')?.invalid && editForm.get('estado')?.touched) {
              <small class="edit-error" role="alert">El estado es obligatorio</small>
            }
          </div>

          @if (saveError()) {
            <p class="edit-save-error" role="alert">{{ saveError() }}</p>
          }
        </form>

        <ng-template pTemplate="footer">
          <button
            pButton
            type="button"
            label="Cancelar"
            icon="pi pi-times"
            severity="secondary"
            [text]="true"
            [disabled]="saving()"
            (click)="cancelarEdicion()"
          ></button>
          <button
            pButton
            type="button"
            label="Guardar cambios"
            icon="pi pi-check"
            [loading]="saving()"
            [disabled]="editForm.invalid || saving()"
            (click)="guardarEdicion()"
          ></button>
        </ng-template>
      </p-dialog>

      <!-- ── Confirm de eliminación ──────────────────────────────────────── -->
      <p-confirmDialog [style]="{ maxWidth: '420px' }" acceptButtonStyleClass="p-button-danger" />

      <!-- ── Toast de errores ────────────────────────────────────────────── -->
      <p-toast position="bottom-center" />
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
      animation: cardIn 0.2s ease both;
    }

    @keyframes cardIn {
      from { opacity: 0; transform: translateY(6px); }
      to   { opacity: 1; transform: translateY(0); }
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

    /* ── Botones de acción ────────────────────────────────────── */
    .card-actions {
      display: flex;
      gap: 0.25rem;
      justify-content: flex-end;
      margin-top: 0.875rem;
      padding-top: 0.75rem;
      border-top: 1px solid #f1f5f9;
    }

    /* ── Formulario de edición (dentro del p-dialog) ──────────── */
    .edit-form {
      display: flex;
      flex-direction: column;
      gap: 1.25rem;
      padding: 0.25rem 0;
    }

    .edit-field {
      display: flex;
      flex-direction: column;
      gap: 0.4rem;
    }

    .edit-label {
      font-size: 0.875rem;
      font-weight: 600;
      color: #374151;
    }

    .edit-error {
      font-size: 0.8rem;
      color: #dc2626;
    }

    .edit-save-error {
      font-size: 0.875rem;
      color: #dc2626;
      background: #fef2f2;
      border: 1px solid #fecaca;
      border-radius: 6px;
      padding: 0.5rem 0.75rem;
      margin: 0;
    }
  `],
})
export class MisReportesComponent implements OnInit {
  private readonly reporteService = inject(ReporteService);
  private readonly wsService = inject(ReporteWebSocketService);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly messageService = inject(MessageService);
  private readonly fb = inject(FormBuilder);

  readonly reportes = signal<PuntoInteres[]>([]);
  readonly loading = signal(false);
  readonly error = signal(false);
  readonly editandoReporte = signal<PuntoInteres | null>(null);
  readonly saving = signal(false);
  readonly saveError = signal<string | null>(null);

  readonly editForm = this.fb.group({
    descripcion: [''],
    estado: [null as EstadoPunto | null, Validators.required],
  });

  readonly estadoOptions: EstadoOption[] = [
    { label: 'Activo',   value: 'ACTIVO' },
    { label: 'Resuelto', value: 'RESUELTO' },
  ];

  constructor() {
    this.wsService.reporteUpserted$
      .pipe(takeUntilDestroyed())
      .subscribe((p) => {
        this.reportes.update((list) => list.map((r) => (r.id === p.id ? p : r)));
      });

    this.wsService.reporteEliminado$
      .pipe(takeUntilDestroyed())
      .subscribe((id) => {
        this.reportes.update((list) => list.filter((r) => r.id !== id));
      });
  }

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

  onEditClick(r: PuntoInteres): void {
    this.editForm.reset({ descripcion: r.descripcion ?? '', estado: r.estado ?? 'ACTIVO' });
    this.saveError.set(null);
    this.editandoReporte.set(r);
  }

  onDeleteClick(r: PuntoInteres): void {
    this.confirmationService.confirm({
      message: `¿Querés eliminar "<strong>${r.titulo}</strong>"? Esta acción no se puede deshacer.`,
      header: 'Confirmar eliminación',
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: 'Sí, eliminar',
      rejectLabel: 'Cancelar',
      accept: () => {
        this.reporteService.deleteReporte(r.id).subscribe({
          next: () => {
            this.reportes.update((list) => list.filter((x) => x.id !== r.id));
          },
          error: () => {
            this.messageService.add({
              severity: 'error',
              summary: 'Error',
              detail: 'No se pudo eliminar el reporte. Intentá de nuevo.',
              life: 4000,
            });
          },
        });
      },
    });
  }

  guardarEdicion(): void {
    const r = this.editandoReporte();
    if (!r || this.editForm.invalid || this.saving()) return;

    const { descripcion, estado } = this.editForm.getRawValue();
    this.saving.set(true);
    this.saveError.set(null);

    this.reporteService.updateReporte(r.id, { descripcion: descripcion ?? '', estado: estado! }).subscribe({
      next: (updated) => {
        this.reportes.update((list) => list.map((x) => (x.id === updated.id ? updated : x)));
        this.saving.set(false);
        this.editandoReporte.set(null);
      },
      error: () => {
        this.saving.set(false);
        this.saveError.set('No se pudo guardar. Intentá de nuevo.');
      },
    });
  }

  cancelarEdicion(): void {
    if (!this.editandoReporte()) return;
    this.editandoReporte.set(null);
    this.saveError.set(null);
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
