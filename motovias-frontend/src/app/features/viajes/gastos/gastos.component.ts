import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  OnInit,
  signal,
} from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { MessageService, PrimeTemplate } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { Dialog } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { InputNumber } from 'primeng/inputnumber';
import { Select } from 'primeng/select';
import { TableModule } from 'primeng/table';
import { Toast } from 'primeng/toast';
import { GastoService } from '../../../core/services/gasto.service';
import { AuthService } from '../../../core/services/auth.service';
import {
  CATEGORIA_LABELS,
  CATEGORIA_OPTIONS,
  CategoriaGasto,
  GastoRequestDTO,
  GastoResponseDTO,
  ParticipanteGastoDTO,
  TransferenciaSimplificadaDTO,
} from '../../../core/models/gasto.model';

@Component({
  selector: 'app-gastos',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [MessageService],
  imports: [
    RouterLink,
    ReactiveFormsModule,
    CurrencyPipe,
    DatePipe,
    ButtonModule,
    Dialog,
    InputTextModule,
    InputNumber,
    Select,
    TableModule,
    PrimeTemplate,
    Toast,
  ],
  template: `
    <div class="page-shell">
      <header class="page-header">
        <a routerLink="/caravanas" class="back-link">
          <i class="pi pi-arrow-left" aria-hidden="true"></i>
          Volver a caravanas
        </a>
        <div class="header-row">
          <h1 class="page-title">
            <i class="pi pi-wallet" aria-hidden="true"></i>
            Gestión de Gastos
          </h1>
          <p-button
            label="Registrar Gasto"
            icon="pi pi-plus"
            (onClick)="abrirDialogo()"
            [disabled]="loading() || error()"
            aria-label="Abrir formulario para registrar un nuevo gasto"
          />
        </div>
      </header>

      @if (loading()) {
        <div class="loading-state" role="status" aria-live="polite">
          <i class="pi pi-spin pi-spinner loading-icon" aria-hidden="true"></i>
          <p>Cargando datos de la caravana…</p>
        </div>
      } @else if (error()) {
        <div class="error-state" role="alert">
          <i class="pi pi-exclamation-circle error-icon" aria-hidden="true"></i>
          <p class="error-msg">
            No se pudieron cargar los datos. Verificá tu conexión e intentá de nuevo.
          </p>
        </div>
      } @else {

        <!-- ── Historial de Gastos ─────────────────────────────── -->
        <section class="section" aria-labelledby="historial-heading">
          <h2 class="section-title" id="historial-heading">
            <i class="pi pi-list" aria-hidden="true"></i>
            Historial de Gastos
          </h2>

          @if (historial().length === 0) {
            <div class="empty-state" role="status" aria-live="polite">
              <i class="pi pi-inbox empty-icon" aria-hidden="true"></i>
              <p class="empty-msg">No hay gastos registrados en esta caravana.</p>
              <p class="empty-hint">
                Utilizá el botón "Registrar Gasto" para añadir el primer registro.
              </p>
            </div>
          } @else {
            <p-table
              [value]="historial()"
              [paginator]="historial().length > 10"
              [rows]="10"
              [rowsPerPageOptions]="[10, 25, 50]"
              styleClass="p-datatable-striped"
              [tableStyle]="{ 'min-width': '640px' }"
              aria-label="Historial cronológico de gastos de la caravana"
            >
              <ng-template pTemplate="header">
                <tr>
                  <th scope="col">Descripción</th>
                  <th scope="col">Categoría</th>
                  <th scope="col">Pagador</th>
                  <th scope="col">Fecha</th>
                  <th scope="col" class="col-right">Monto Total</th>
                </tr>
              </ng-template>
              <ng-template pTemplate="body" let-gasto>
                <tr>
                  <td>{{ gasto.descripcion }}</td>
                  <td>
                    <span class="categoria-badge">
                      {{ $any(categoriaLabels)[gasto.categoria] ?? gasto.categoria }}
                    </span>
                  </td>
                  <td>{{ gasto.pagadorNombre }}</td>
                  <td>{{ gasto.fechaCreacion | date:'dd/MM/yyyy HH:mm' }}</td>
                  <td class="col-right monto-cell">
                    {{ gasto.monto | currency:'ARS':'symbol':'1.2-2':'es-AR' }}
                  </td>
                </tr>
              </ng-template>
              <ng-template pTemplate="emptymessage">
                <tr>
                  <td colspan="5" class="empty-table-row">Sin registros disponibles.</td>
                </tr>
              </ng-template>
            </p-table>
          }
        </section>

        <!-- ── Balance y Liquidación ──────────────────────────── -->
        <section class="section" aria-labelledby="balance-heading">
          <h2 class="section-title" id="balance-heading">
            <i class="pi pi-chart-bar" aria-hidden="true"></i>
            Balance y Liquidación
          </h2>

          @if (balance().length === 0) {
            <div class="balance-settled" role="status" aria-live="polite">
              <i class="pi pi-check-circle settled-icon" aria-hidden="true"></i>
              <p class="settled-msg">
                Las cuentas del grupo se encuentran saldadas. No se requieren transferencias monetarias.
              </p>
            </div>
          } @else {
            <ul class="transferencias-list" aria-label="Transferencias monetarias pendientes">
              @for (t of balance(); track t.deudorEmail + t.acreedorEmail) {
                <li class="transferencia-item">
                  <div class="transferencia-principal">
                    <span class="participante-nombre">{{ t.deudorNombre }}</span>
                    <span class="transferencia-verbo">debe transferir</span>
                    <span class="transferencia-monto">
                      {{ t.monto | currency:'ARS':'symbol':'1.2-2':'es-AR' }}
                    </span>
                    <span class="transferencia-verbo">a</span>
                    <span class="participante-nombre">{{ t.acreedorNombre }}</span>
                  </div>
                  <div class="transferencia-emails" aria-label="Detalle de emails">
                    <span class="email-etiqueta">De:</span>
                    <span>{{ t.deudorEmail }}</span>
                    <span class="email-separador" aria-hidden="true">&rarr;</span>
                    <span class="email-etiqueta">Para:</span>
                    <span>{{ t.acreedorEmail }}</span>
                  </div>
                  @if (t.deudorEmail === usuarioActualEmail()) {
                    <div class="transferencia-acciones">
                      <p-button
                        label="Pagar deudas con Mercado Pago"
                        icon="pi pi-credit-card"
                        [loading]="pagandoEmail() === t.acreedorEmail"
                        [disabled]="pagandoEmail() !== null"
                        (onClick)="pagarConMercadoPago(t)"
                        aria-label="Pagar la deuda pendiente mediante Mercado Pago"
                      />
                    </div>
                  }
                </li>
              }
            </ul>
          }
        </section>

      }
    </div>

    <!-- ── Diálogo de Registro ─────────────────────────────────── -->
    <p-dialog
      header="Registrar Nuevo Gasto"
      [visible]="dialogVisible()"
      (visibleChange)="dialogVisible.set($event)"
      [modal]="true"
      [draggable]="false"
      [resizable]="false"
      [style]="{ width: '32rem', maxWidth: '95vw' }"
      (onHide)="cerrarDialogo()"
      aria-labelledby="dialog-gasto-title"
    >
      <form
        [formGroup]="gastoForm"
        (ngSubmit)="registrarGasto()"
        id="gasto-form"
        aria-label="Formulario de registro de gasto"
        novalidate
      >

        <!-- Descripción -->
        <div class="form-field">
          <label for="gasto-descripcion" class="form-label">
            Descripción <span class="required" aria-hidden="true">*</span>
          </label>
          <input
            pInputText
            id="gasto-descripcion"
            type="text"
            formControlName="descripcion"
            placeholder="Ej: Combustible en ruta 9"
            class="form-input"
            aria-required="true"
            [attr.aria-invalid]="
              gastoForm.get('descripcion')?.invalid &&
              gastoForm.get('descripcion')?.touched
            "
          />
          @if (
            gastoForm.get('descripcion')?.invalid &&
            gastoForm.get('descripcion')?.touched
          ) {
            <small class="form-error" role="alert">La descripción es obligatoria.</small>
          }
        </div>

        <!-- Monto -->
        <div class="form-field">
          <label for="gasto-monto" class="form-label">
            Monto (ARS) <span class="required" aria-hidden="true">*</span>
          </label>
          <p-inputnumber
            inputId="gasto-monto"
            formControlName="monto"
            mode="currency"
            currency="ARS"
            locale="es-AR"
            [minFractionDigits]="2"
            [maxFractionDigits]="2"
            [min]="0.01"
            placeholder="0,00"
            [inputStyle]="{ width: '100%' }"
            aria-required="true"
            [attr.aria-invalid]="
              gastoForm.get('monto')?.invalid &&
              gastoForm.get('monto')?.touched
            "
          />
          @if (
            gastoForm.get('monto')?.invalid &&
            gastoForm.get('monto')?.touched
          ) {
            <small class="form-error" role="alert">
              El monto es obligatorio y debe ser mayor a $ 0,00.
            </small>
          }
        </div>

        <!-- Categoría -->
        <div class="form-field">
          <label for="gasto-categoria" class="form-label">
            Categoría <span class="required" aria-hidden="true">*</span>
          </label>
          <p-select
            inputId="gasto-categoria"
            formControlName="categoria"
            [options]="categoriaOptions"
            optionLabel="label"
            optionValue="value"
            placeholder="Seleccioná una categoría"
            [style]="{ width: '100%' }"
            appendTo="body"
            aria-required="true"
            [attr.aria-invalid]="
              gastoForm.get('categoria')?.invalid &&
              gastoForm.get('categoria')?.touched
            "
          />
          @if (
            gastoForm.get('categoria')?.invalid &&
            gastoForm.get('categoria')?.touched
          ) {
            <small class="form-error" role="alert">La categoría es obligatoria.</small>
          }
        </div>

        <!-- Pagador -->
        <div class="form-field">
          <label for="gasto-pagador" class="form-label">
            Pagador <span class="required" aria-hidden="true">*</span>
          </label>
          <p-select
            inputId="gasto-pagador"
            formControlName="pagadorId"
            [options]="participanteOptions()"
            optionLabel="label"
            optionValue="value"
            placeholder="Seleccioná quién pagó"
            [style]="{ width: '100%' }"
            appendTo="body"
            aria-required="true"
            [attr.aria-invalid]="
              gastoForm.get('pagadorId')?.invalid &&
              gastoForm.get('pagadorId')?.touched
            "
          />
          @if (
            gastoForm.get('pagadorId')?.invalid &&
            gastoForm.get('pagadorId')?.touched
          ) {
            <small class="form-error" role="alert">Debés indicar quién realizó el pago.</small>
          }
        </div>

      </form>

      <ng-template pTemplate="footer">
        <p-button
          label="Cancelar"
          severity="secondary"
          [text]="true"
          [disabled]="submitting()"
          (onClick)="cerrarDialogo()"
          aria-label="Cancelar y cerrar el formulario"
        />
        <p-button
          label="Registrar Gasto"
          icon="pi pi-check"
          [loading]="submitting()"
          [disabled]="gastoForm.invalid || submitting()"
          (onClick)="registrarGasto()"
          aria-label="Confirmar y registrar el gasto"
        />
      </ng-template>
    </p-dialog>

    <p-toast position="bottom-center" />
  `,
  styles: [`
    .page-shell {
      max-width: 72rem;
      margin: 0 auto;
      padding: 2rem 1.25rem 3rem;
    }

    /* ── Header ──────────────────────────────────────────────── */
    .page-header { margin-bottom: 2rem; }

    .back-link {
      display: inline-flex;
      align-items: center;
      gap: 0.375rem;
      font-size: 0.875rem;
      color: #64748b;
      text-decoration: none;
      margin-bottom: 1rem;
      transition: color 0.15s;
    }
    .back-link:hover { color: #3b82f6; }
    .back-link:focus-visible {
      outline: 2px solid #3b82f6;
      outline-offset: 2px;
      border-radius: 4px;
    }

    .header-row {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 1rem;
      flex-wrap: wrap;
    }

    .page-title {
      display: flex;
      align-items: center;
      gap: 0.625rem;
      font-size: 1.625rem;
      font-weight: 800;
      color: #0f172a;
      margin: 0;
    }
    .page-title .pi { color: #3b82f6; font-size: 1.5rem; }

    /* ── Estados ─────────────────────────────────────────────── */
    .loading-state,
    .error-state {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 1rem;
      padding: 5rem 1.5rem;
      text-align: center;
    }
    .loading-state { color: #64748b; }
    .error-state { color: #dc2626; }
    .loading-icon,
    .error-icon { font-size: 2.5rem; }
    .error-msg {
      font-size: 0.9375rem;
      max-width: 28rem;
      line-height: 1.6;
      margin: 0;
    }

    /* ── Secciones ───────────────────────────────────────────── */
    .section {
      background: #ffffff;
      border: 1px solid #e2e8f0;
      border-radius: 12px;
      padding: 1.5rem;
      margin-bottom: 1.5rem;
      box-shadow: 0 1px 4px rgb(0 0 0 / .05);
      overflow-x: auto;
    }

    .section-title {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      font-size: 0.8125rem;
      font-weight: 700;
      text-transform: uppercase;
      letter-spacing: 0.06em;
      color: #64748b;
      margin: 0 0 1.25rem;
      padding-bottom: 0.875rem;
      border-bottom: 1px solid #f1f5f9;
    }

    /* ── Estado vacío de sección ─────────────────────────────── */
    .empty-state {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 0.5rem;
      padding: 3rem 1.5rem;
      text-align: center;
    }
    .empty-icon { font-size: 2.25rem; color: #cbd5e1; }
    .empty-msg {
      font-size: 0.9375rem;
      font-weight: 500;
      color: #475569;
      margin: 0;
    }
    .empty-hint {
      font-size: 0.8125rem;
      color: #94a3b8;
      margin: 0;
    }

    /* ── Tabla ───────────────────────────────────────────────── */
    .col-right { text-align: right !important; }
    .monto-cell {
      text-align: right;
      font-weight: 600;
      color: #0f172a;
      font-variant-numeric: tabular-nums;
    }
    .empty-table-row {
      text-align: center;
      color: #94a3b8;
      font-style: italic;
      padding: 2rem;
    }

    .categoria-badge {
      display: inline-block;
      padding: 0.1875rem 0.625rem;
      background: #f1f5f9;
      border: 1px solid #e2e8f0;
      border-radius: 9999px;
      font-size: 0.75rem;
      font-weight: 600;
      color: #475569;
      text-transform: uppercase;
      letter-spacing: 0.04em;
      white-space: nowrap;
    }

    /* ── Balance saldado ─────────────────────────────────────── */
    .balance-settled {
      display: flex;
      align-items: flex-start;
      gap: 0.875rem;
      padding: 1.25rem 1.5rem;
      background: #f0fdf4;
      border: 1px solid #bbf7d0;
      border-radius: 8px;
    }
    .settled-icon { font-size: 1.375rem; color: #16a34a; flex-shrink: 0; margin-top: 0.125rem; }
    .settled-msg {
      font-size: 0.9375rem;
      color: #166534;
      font-weight: 500;
      line-height: 1.6;
      margin: 0;
    }

    /* ── Transferencias pendientes ───────────────────────────── */
    .transferencias-list {
      list-style: none;
      padding: 0;
      margin: 0;
      display: flex;
      flex-direction: column;
      gap: 0.75rem;
    }

    .transferencia-item {
      padding: 1rem 1.25rem;
      background: #fffbeb;
      border: 1px solid #fde68a;
      border-radius: 8px;
      display: flex;
      flex-direction: column;
      gap: 0.375rem;
    }

    .transferencia-principal {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      flex-wrap: wrap;
      font-size: 0.9375rem;
      line-height: 1.4;
    }

    .participante-nombre {
      font-weight: 700;
      color: #0f172a;
    }

    .transferencia-verbo {
      font-size: 0.875rem;
      color: #64748b;
    }

    .transferencia-monto {
      font-size: 1rem;
      font-weight: 800;
      color: #b45309;
      font-variant-numeric: tabular-nums;
    }

    .transferencia-emails {
      display: flex;
      align-items: center;
      gap: 0.375rem;
      flex-wrap: wrap;
      font-size: 0.78125rem;
      color: #94a3b8;
    }
    .email-etiqueta { font-weight: 600; color: #64748b; }
    .email-separador { color: #cbd5e1; }

    .transferencia-acciones {
      margin-top: 0.5rem;
      display: flex;
      justify-content: flex-end;
    }

    /* ── Formulario del diálogo ──────────────────────────────── */
    .form-field {
      display: flex;
      flex-direction: column;
      gap: 0.375rem;
      margin-bottom: 1.125rem;
    }
    .form-field:last-child { margin-bottom: 0; }

    .form-label {
      font-size: 0.875rem;
      font-weight: 600;
      color: #374151;
    }

    .required { color: #dc2626; }
    .form-input { width: 100%; }

    .form-error {
      font-size: 0.8rem;
      color: #dc2626;
    }
  `],
})
export class GastosComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly gastoService = inject(GastoService);
  private readonly authService = inject(AuthService);
  private readonly messageService = inject(MessageService);
  private readonly fb = inject(FormBuilder);

  readonly viajeId = signal(0);
  readonly historial = signal<GastoResponseDTO[]>([]);
  readonly balance = signal<TransferenciaSimplificadaDTO[]>([]);
  readonly participantes = signal<ParticipanteGastoDTO[]>([]);
  readonly loading = signal(true);
  readonly error = signal(false);
  readonly dialogVisible = signal(false);
  readonly submitting = signal(false);
  readonly pagandoEmail = signal<string | null>(null);

  readonly usuarioActualEmail = computed(() => this.authService.currentUser()?.email ?? '');

  readonly categoriaLabels = CATEGORIA_LABELS;
  readonly categoriaOptions = CATEGORIA_OPTIONS;

  readonly participanteOptions = computed(() =>
    this.participantes().map((p) => ({ label: p.nombre, value: p.id })),
  );

  readonly gastoForm = this.fb.group({
    descripcion: ['', [Validators.required]],
    monto: [null as number | null, [Validators.required, Validators.min(0.01)]],
    categoria: [null as CategoriaGasto | null, [Validators.required]],
    pagadorId: [null as number | null, [Validators.required]],
  });

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id') ?? '0');
    this.viajeId.set(id);
    this.cargarDatos(id);
  }

  abrirDialogo(): void {
    this.gastoForm.reset();
    this.dialogVisible.set(true);
  }

  cerrarDialogo(): void {
    this.dialogVisible.set(false);
    this.gastoForm.reset();
  }

  registrarGasto(): void {
    this.gastoForm.markAllAsTouched();
    if (this.gastoForm.invalid || this.submitting()) return;

    this.submitting.set(true);

    const raw = this.gastoForm.getRawValue();
    const dto: GastoRequestDTO = {
      descripcion: raw.descripcion!,
      monto: raw.monto!,
      categoria: raw.categoria!,
      pagadorId: raw.pagadorId,
    };

    this.gastoService.registrarGasto(this.viajeId(), dto).subscribe({
      next: () => {
        this.submitting.set(false);
        this.cerrarDialogo();
        this.messageService.add({
          severity: 'success',
          summary: 'Gasto registrado',
          detail: 'El gasto fue añadido correctamente al historial de la caravana.',
          life: 4000,
        });
        this.recargarHistorialYBalance();
      },
      error: () => {
        this.submitting.set(false);
        this.messageService.add({
          severity: 'error',
          summary: 'Error al registrar',
          detail: 'No se pudo registrar el gasto. Verificá los datos e intentá de nuevo.',
          life: 5000,
        });
      },
    });
  }

  pagarConMercadoPago(transferencia: TransferenciaSimplificadaDTO): void {
    if (this.pagandoEmail() !== null) return;

    this.pagandoEmail.set(transferencia.acreedorEmail);

    this.gastoService
      .crearPreferenciaPago(this.viajeId(), {
        monto: transferencia.monto,
        acreedorEmail: transferencia.acreedorEmail,
      })
      .subscribe({
        next: ({ initPoint }) => {
          window.location.href = initPoint;
        },
        error: () => {
          this.pagandoEmail.set(null);
          this.messageService.add({
            severity: 'error',
            summary: 'Error al iniciar el pago',
            detail: 'No se pudo generar el enlace de pago de Mercado Pago. Intentá de nuevo.',
            life: 5000,
          });
        },
      });
  }

  private cargarDatos(viajeId: number): void {
    forkJoin([
      this.gastoService.obtenerHistorialGastos(viajeId),
      this.gastoService.obtenerBalanceSimplificado(viajeId),
      this.gastoService.obtenerParticipantes(viajeId),
    ]).subscribe({
      next: ([historial, balance, participantes]) => {
        this.historial.set(historial);
        this.balance.set(balance);
        this.participantes.set(participantes);
        this.loading.set(false);
      },
      error: () => {
        this.error.set(true);
        this.loading.set(false);
      },
    });
  }

  private recargarHistorialYBalance(): void {
    const id = this.viajeId();
    forkJoin([
      this.gastoService.obtenerHistorialGastos(id),
      this.gastoService.obtenerBalanceSimplificado(id),
    ]).subscribe({
      next: ([historial, balance]) => {
        this.historial.set(historial);
        this.balance.set(balance);
      },
      error: () => {
        this.messageService.add({
          severity: 'warn',
          summary: 'Advertencia',
          detail:
            'El gasto fue registrado pero no se pudo actualizar la vista. Recargá la página.',
          life: 6000,
        });
      },
    });
  }
}
