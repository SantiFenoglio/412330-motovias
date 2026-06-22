import {
  ChangeDetectionStrategy,
  Component,
  inject,
  signal,
} from '@angular/core';
import { Router } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { TextareaModule } from 'primeng/textarea';
import { Toast } from 'primeng/toast';
import { Tab, TabList, TabPanel, TabPanels, Tabs } from 'primeng/tabs';
import { ViajeService } from '../../../core/services/viaje.service';

@Component({
  selector: 'app-viaje-dashboard',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [MessageService],
  imports: [
    ReactiveFormsModule,
    ButtonModule,
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
    </div>

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
  `],
})
export class ViajeDashboardComponent {
  private readonly viajeService = inject(ViajeService);
  private readonly messageService = inject(MessageService);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);

  readonly creando = signal(false);
  readonly uniendose = signal(false);
  readonly codigoUnirse = signal('');

  readonly crearForm = this.fb.group({
    titulo: ['', [Validators.required, Validators.maxLength(100)]],
    descripcion: [''],
  });

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
