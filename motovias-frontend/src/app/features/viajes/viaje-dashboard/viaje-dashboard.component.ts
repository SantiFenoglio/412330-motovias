import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  OnInit,
  signal,
} from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { ConfirmationService, MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ConfirmDialog } from 'primeng/confirmdialog';
import { InputTextModule } from 'primeng/inputtext';
import { TextareaModule } from 'primeng/textarea';
import { Toast } from 'primeng/toast';
import { Tab, TabList, TabPanel, TabPanels, Tabs } from 'primeng/tabs';
import { AuthService } from '../../../core/services/auth.service';
import { ViajeService } from '../../../core/services/viaje.service';
import { ParticipanteResponse, ViajeResponse } from '../../../core/models/viaje.model';

@Component({
  selector: 'app-viaje-dashboard',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [MessageService, ConfirmationService],
  imports: [
    ReactiveFormsModule,
    RouterLink,
    ButtonModule,
    ConfirmDialog,
    InputTextModule,
    TextareaModule,
    Toast,
    Tabs,
    TabList,
    Tab,
    TabPanels,
    TabPanel,
  ],
  template: `
    <div class="page-shell">
      <header class="page-header">
        <h1 class="page-title">
          <i class="pi pi-users" aria-hidden="true"></i>
          Caravanas
        </h1>
        <p class="page-subtitle">Organizá o unite a un viaje grupal de motoviajeros</p>
      </header>

      @if (checkingActivo()) {
        <div class="loading-state" role="status" aria-live="polite">
          <i class="pi pi-spin pi-spinner loading-icon" aria-hidden="true"></i>
          <p>Buscando tu caravana activa…</p>
        </div>
      } @else if (viajeActivo(); as v) {

        <!-- ── Mi Caravana Activa ─────────────────────────────── -->
        <section class="active-panel" aria-label="Mi caravana activa">
          <div class="active-panel__header">
            <span class="active-badge">
              <i class="pi pi-circle-fill active-badge__dot" aria-hidden="true"></i>
              Caravana activa
            </span>
            <h2 class="active-title">{{ v.titulo }}</h2>
            @if (v.descripcion) {
              <p class="active-desc">{{ v.descripcion }}</p>
            }
          </div>

          <section class="code-section" aria-label="Código de la caravana">
            <p class="code-section__label">Código de la caravana</p>
            <p class="code-section__hint">Compartí este código con quienes quieran unirse</p>

            <div class="code-row">
              <div class="code-box" role="text" [attr.aria-label]="'Código: ' + v.codigo">
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

          <section class="participantes-section" aria-label="Compañeros de la caravana">
            <p class="participantes-label">Compañeros de ruta ({{ participantes().length }})</p>

            @if (cargandoParticipantes()) {
              <p class="participantes-loading">
                <i class="pi pi-spin pi-spinner" aria-hidden="true"></i>
                Cargando participantes…
              </p>
            } @else {
              <ul class="participantes-list">
                @for (p of participantes(); track p.id) {
                  <li class="participante-item">
                    <span class="participante-avatar" aria-hidden="true">{{ inicial(p.nombre) }}</span>
                    <span class="participante-info">
                      <span class="participante-nombre">{{ p.nombre }}</span>
                      <span class="participante-email">{{ p.email }}</span>
                    </span>
                  </li>
                }
              </ul>
            }
          </section>

          <a
            [routerLink]="['/caravanas', v.codigo]"
            class="detail-link"
            aria-label="Ver el detalle completo de esta caravana"
          >
            <i class="pi pi-arrow-right-arrow-left" aria-hidden="true"></i>
            Ver caravana completa
            <i class="pi pi-arrow-right detail-arrow" aria-hidden="true"></i>
          </a>

          @if (esOrganizador()) {
            <p-button
              label="Eliminar caravana"
              icon="pi pi-trash"
              severity="danger"
              [outlined]="true"
              [loading]="eliminando()"
              (onClick)="eliminarCaravana()"
              aria-label="Eliminar permanentemente la caravana activa"
              styleClass="leave-button"
            />
          } @else {
            <p-button
              label="Abandonar caravana"
              icon="pi pi-sign-out"
              severity="danger"
              [outlined]="true"
              [loading]="abandonando()"
              (onClick)="abandonarCaravana()"
              aria-label="Abandonar la caravana activa"
              styleClass="leave-button"
            />
          }
        </section>

      } @else {

        <p-tabs value="crear">
          <p-tablist>
            <p-tab value="crear">
              <i class="pi pi-plus-circle tab-icon" aria-hidden="true"></i>
              Crear caravana
            </p-tab>
            <p-tab value="unirse">
              <i class="pi pi-sign-in tab-icon" aria-hidden="true"></i>
              Unirse a caravana
            </p-tab>
          </p-tablist>

          <p-tabpanels>

            <!-- ── Crear ─────────────────────────────────────────── -->
            <p-tabpanel value="crear">
              <form
                [formGroup]="crearForm"
                (ngSubmit)="crearCaravana()"
                class="form"
                aria-label="Formulario para crear una caravana"
                novalidate
              >
                <div class="field">
                  <label for="titulo" class="field-label">
                    Título <span class="required" aria-hidden="true">*</span>
                  </label>
                  <input
                    pInputText
                    id="titulo"
                    formControlName="titulo"
                    class="field-input"
                    placeholder="Ej: Vuelta a los cerros"
                    maxlength="100"
                    aria-required="true"
                    [attr.aria-invalid]="crearForm.get('titulo')?.invalid && crearForm.get('titulo')?.touched"
                  />
                  @if (crearForm.get('titulo')?.invalid && crearForm.get('titulo')?.touched) {
                    <small class="field-error" role="alert">El título es obligatorio</small>
                  }
                </div>

                <div class="field">
                  <label for="descripcion" class="field-label">Descripción</label>
                  <textarea
                    pInputTextarea
                    id="descripcion"
                    formControlName="descripcion"
                    class="field-input"
                    placeholder="Detallá la ruta, punto de encuentro, duración estimada…"
                    [rows]="4"
                    [autoResize]="true"
                    style="width:100%; resize:none; overflow:hidden"
                    aria-label="Descripción de la caravana (opcional)"
                  ></textarea>
                </div>

                <p-button
                  type="submit"
                  label="Crear caravana"
                  icon="pi pi-check"
                  [loading]="creando()"
                  [disabled]="crearForm.invalid || creando()"
                  aria-label="Crear caravana"
                />
              </form>
            </p-tabpanel>

            <!-- ── Unirse ─────────────────────────────────────────── -->
            <p-tabpanel value="unirse">
              <div class="join-section" aria-labelledby="join-section-title">
                <p id="join-section-title" class="join-hint">
                  <i class="pi pi-info-circle" aria-hidden="true"></i>
                  Pedile el código de 6 caracteres al organizador de la caravana.
                </p>

                <div class="join-input-row">
                  <input
                    pInputText
                    id="codigoUnirse"
                    class="join-code-input"
                    [value]="codigoUnirse()"
                    (input)="onCodigoInput($event)"
                    maxlength="6"
                    placeholder="ABC123"
                    autocomplete="off"
                    autocorrect="off"
                    spellcheck="false"
                    aria-label="Código de la caravana (6 caracteres)"
                    aria-required="true"
                  />
                </div>

                <p-button
                  label="Unirse a la caravana"
                  icon="pi pi-users"
                  [loading]="uniendose()"
                  [disabled]="codigoUnirse().length !== 6 || uniendose()"
                  (onClick)="unirseACaravana()"
                  aria-label="Unirse a la caravana con el código ingresado"
                />
              </div>
            </p-tabpanel>

          </p-tabpanels>
        </p-tabs>

      }
    </div>

    <p-confirmDialog [style]="{ maxWidth: '420px' }" acceptButtonStyleClass="p-button-danger" />
    <p-toast position="bottom-center" />
  `,
  styles: [`
    .page-shell {
      max-width: 38rem;
      margin: 0 auto;
      padding: 2rem 1.25rem;
    }

    .page-header { margin-bottom: 2rem; }

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

    .tab-icon { margin-right: 0.375rem; }

    /* ── Crear form ────────────────────────────────────────── */
    .form {
      display: flex;
      flex-direction: column;
      gap: 1.25rem;
      padding: 1.25rem 0;
    }

    .field {
      display: flex;
      flex-direction: column;
      gap: 0.4rem;
    }

    .field-label {
      font-size: 0.875rem;
      font-weight: 600;
      color: #374151;
    }

    .required { color: #dc2626; }

    .field-input { width: 100%; }

    .field-error {
      font-size: 0.8rem;
      color: #dc2626;
    }

    /* ── Unirse ─────────────────────────────────────────────── */
    .join-section {
      display: flex;
      flex-direction: column;
      gap: 1.25rem;
      padding: 1.25rem 0;
    }

    .join-hint {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      font-size: 0.875rem;
      color: #64748b;
      margin: 0;
      background: #f8fafc;
      border: 1px solid #e2e8f0;
      border-radius: 8px;
      padding: 0.75rem 1rem;
    }

    .join-input-row { display: flex; }

    .join-code-input {
      font-family: monospace !important;
      font-size: 1.75rem !important;
      font-weight: 700 !important;
      letter-spacing: 0.3em !important;
      text-align: center;
      text-transform: uppercase;
      width: 13rem;
      padding: 0.75rem 1rem !important;
      border: 2px solid #cbd5e1 !important;
      border-radius: 10px !important;
      background: #f8fafc !important;
      color: #0f172a !important;
      transition: border-color 0.2s;
    }
    .join-code-input:focus {
      border-color: #3b82f6 !important;
      background: #fff !important;
      box-shadow: 0 0 0 3px rgb(59 130 246 / .15) !important;
    }
    .join-code-input::placeholder {
      color: #94a3b8 !important;
      font-weight: 400 !important;
      letter-spacing: 0.05em !important;
    }

    /* ── Estado de carga inicial ───────────────────────────────── */
    .loading-state {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 1rem;
      padding: 4rem 1.5rem;
      color: #64748b;
      text-align: center;
    }
    .loading-icon { font-size: 2.5rem; }

    /* ── Mi Caravana Activa ────────────────────────────────────── */
    .active-panel {
      display: flex;
      flex-direction: column;
      gap: 1.5rem;
    }

    .active-panel__header { display: flex; flex-direction: column; gap: 0.5rem; }

    .active-badge {
      display: inline-flex;
      align-items: center;
      gap: 0.4rem;
      align-self: flex-start;
      font-size: 0.75rem;
      font-weight: 700;
      text-transform: uppercase;
      letter-spacing: 0.05em;
      color: #059669;
      background: #ecfdf5;
      border: 1px solid #a7f3d0;
      border-radius: 999px;
      padding: 0.3rem 0.75rem;
    }
    .active-badge__dot {
      font-size: 0.5rem;
      animation: pulse-dot 1.5s ease-in-out infinite;
    }
    @keyframes pulse-dot {
      0%, 100% { opacity: 1; }
      50% { opacity: 0.3; }
    }

    .active-title {
      font-size: 1.5rem;
      font-weight: 800;
      color: #0f172a;
      margin: 0;
      line-height: 1.2;
    }

    .active-desc {
      font-size: 0.9375rem;
      color: #475569;
      margin: 0;
      line-height: 1.6;
    }

    .code-section,
    .participantes-section {
      padding: 1.5rem 1.75rem;
      background: #fff;
      border: 1.5px solid #e2e8f0;
      border-radius: 16px;
      box-shadow: 0 2px 8px rgb(0 0 0 / .06);
    }

    .code-section__label,
    .participantes-label {
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

    .code-row { display: flex; align-items: center; gap: 0.5rem; }

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

    .participantes-label { margin-bottom: 1rem; }

    .participantes-loading {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      font-size: 0.875rem;
      color: #64748b;
      margin: 0;
    }

    .participantes-list {
      list-style: none;
      margin: 0;
      padding: 0;
      display: flex;
      flex-direction: column;
      gap: 0.75rem;
      max-height: 18rem;
      overflow-y: auto;
    }

    .participante-item {
      display: flex;
      align-items: center;
      gap: 0.75rem;
    }

    .participante-avatar {
      flex-shrink: 0;
      width: 2.25rem;
      height: 2.25rem;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      background: #eff6ff;
      color: #1d4ed8;
      font-weight: 700;
      font-size: 0.9375rem;
    }

    .participante-info {
      display: flex;
      flex-direction: column;
      min-width: 0;
    }

    .participante-nombre {
      font-size: 0.9375rem;
      font-weight: 600;
      color: #1e293b;
    }

    .participante-email {
      font-size: 0.8125rem;
      color: #94a3b8;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .detail-link {
      display: flex;
      align-items: center;
      gap: 0.625rem;
      padding: 0.875rem 1.25rem;
      background: #f8fafc;
      border: 1.5px solid #e2e8f0;
      border-radius: 10px;
      color: #0f172a;
      font-size: 0.9375rem;
      font-weight: 600;
      text-decoration: none;
      transition: border-color 0.15s, background 0.15s, color 0.15s;
    }
    .detail-link:hover {
      border-color: #3b82f6;
      background: #eff6ff;
      color: #1d4ed8;
    }
    .detail-link:focus-visible {
      outline: 2px solid #3b82f6;
      outline-offset: 2px;
    }
    .detail-arrow { margin-left: auto; font-size: 0.875rem; }
  `],
})
export class ViajeDashboardComponent implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly viajeService = inject(ViajeService);
  private readonly messageService = inject(MessageService);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);

  readonly creando = signal(false);
  readonly uniendose = signal(false);
  readonly codigoUnirse = signal('');

  readonly checkingActivo = signal(true);
  readonly viajeActivo = signal<ViajeResponse | null>(null);
  readonly participantes = signal<ParticipanteResponse[]>([]);
  readonly cargandoParticipantes = signal(false);
  readonly abandonando = signal(false);
  readonly eliminando = signal(false);

  readonly esOrganizador = computed(() => {
    const v = this.viajeActivo();
    const email = this.authService.currentUser()?.email;
    return !!v && !!email && v.organizadorEmail === email;
  });

  readonly crearForm = this.fb.group({
    titulo: ['', [Validators.required, Validators.maxLength(100)]],
    descripcion: [''],
  });

  ngOnInit(): void {
    this.viajeService.obtenerViajeActivo().subscribe({
      next: (viaje) => {
        this.viajeActivo.set(viaje);
        this.checkingActivo.set(false);
        if (viaje) {
          this.cargarParticipantes(viaje.id);
        }
      },
      error: () => {
        this.checkingActivo.set(false);
      },
    });
  }

  cargarParticipantes(viajeId: number): void {
    this.cargandoParticipantes.set(true);
    this.viajeService.listarParticipantes(viajeId).subscribe({
      next: (participantes) => {
        this.participantes.set(participantes);
        this.cargandoParticipantes.set(false);
      },
      error: () => {
        this.cargandoParticipantes.set(false);
      },
    });
  }

  inicial(nombre: string): string {
    return nombre.charAt(0).toUpperCase();
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

  abandonarCaravana(): void {
    const v = this.viajeActivo();
    if (!v || this.abandonando()) return;

    this.confirmationService.confirm({
      message: `¿Querés abandonar "<strong>${v.titulo}</strong>"? Perderás acceso a los gastos y a la ubicación compartida del grupo.`,
      header: 'Abandonar caravana',
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: 'Sí, abandonar',
      rejectLabel: 'Cancelar',
      accept: () => {
        this.abandonando.set(true);
        this.viajeService.salirDeViaje(v.codigo).subscribe({
          next: () => {
            this.abandonando.set(false);
            this.viajeActivo.set(null);
            this.participantes.set([]);
            this.messageService.add({
              severity: 'success',
              summary: 'Listo',
              detail: 'Abandonaste la caravana.',
              life: 4000,
            });
          },
          error: () => {
            this.abandonando.set(false);
            this.messageService.add({
              severity: 'error',
              summary: 'Error',
              detail: 'No se pudo abandonar la caravana. Intentá de nuevo.',
              life: 5000,
            });
          },
        });
      },
    });
  }

  eliminarCaravana(): void {
    const v = this.viajeActivo();
    if (!v || this.eliminando()) return;

    this.confirmationService.confirm({
      message: `¿Querés eliminar permanentemente "<strong>${v.titulo}</strong>"? Se borrarán todos los gastos, participantes y el historial de la caravana. Esta acción no se puede deshacer.`,
      header: 'Eliminar caravana',
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: 'Sí, eliminar',
      rejectLabel: 'Cancelar',
      accept: () => {
        this.eliminando.set(true);
        this.viajeService.eliminarViaje(v.codigo).subscribe({
          next: () => {
            this.eliminando.set(false);
            this.viajeActivo.set(null);
            this.participantes.set([]);
            this.messageService.add({
              severity: 'success',
              summary: 'Listo',
              detail: 'La caravana fue eliminada.',
              life: 4000,
            });
          },
          error: () => {
            this.eliminando.set(false);
            this.messageService.add({
              severity: 'error',
              summary: 'Error',
              detail: 'No se pudo eliminar la caravana. Intentá de nuevo.',
              life: 5000,
            });
          },
        });
      },
    });
  }

  onCodigoInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    const upper = input.value.toUpperCase();
    input.value = upper;
    this.codigoUnirse.set(upper);
  }

  crearCaravana(): void {
    if (this.crearForm.invalid || this.creando()) return;
    this.creando.set(true);

    const { titulo, descripcion } = this.crearForm.getRawValue();
    const dto = {
      titulo: titulo!,
      ...(descripcion ? { descripcion } : {}),
    };

    this.viajeService.crearViaje(dto).subscribe({
      next: (viaje) => {
        this.creando.set(false);
        this.router.navigate(['/caravanas', viaje.codigo]);
      },
      error: () => {
        this.creando.set(false);
        this.messageService.add({
          severity: 'error',
          summary: 'Error',
          detail: 'No se pudo crear la caravana. Intentá de nuevo.',
          life: 5000,
        });
      },
    });
  }

  unirseACaravana(): void {
    const codigo = this.codigoUnirse();
    if (codigo.length !== 6 || this.uniendose()) return;
    this.uniendose.set(true);

    this.viajeService.unirseAViaje(codigo).subscribe({
      next: (viaje) => {
        this.uniendose.set(false);
        this.router.navigate(['/caravanas', viaje.codigo]);
      },
      error: (err: HttpErrorResponse) => {
        this.uniendose.set(false);
        const backendMsg: string | undefined =
          err.error?.message ?? err.error?.detail;
        const fallback =
          err.status === 404
            ? 'No se encontró ninguna caravana con ese código.'
            : 'No se pudo unir a la caravana. Intentá de nuevo.';
        this.messageService.add({
          severity: err.status === 404 ? 'warn' : 'error',
          summary: err.status === 404 ? 'No encontrada' : 'Error al unirse',
          detail: backendMsg ?? fallback,
          life: 6000,
        });
      },
    });
  }
}
