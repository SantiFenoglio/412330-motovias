import {
  ChangeDetectionStrategy,
  Component,
  inject,
  NgZone,
  input,
  output,
  signal,
} from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Dialog } from 'primeng/dialog';
import { InputText } from 'primeng/inputtext';
import { Select } from 'primeng/select';
import { Textarea } from 'primeng/textarea';
import { FloatLabel } from 'primeng/floatlabel';
import { Button } from 'primeng/button';
import { Message } from 'primeng/message';
import { GeolocationService } from '../../../core/services/geolocation.service';
import { ReporteService } from '../../../core/services/reporte.service';
import {
  Categoria,
  CATEGORY_CONFIG,
  PuntoInteresRequest,
  TODAS_LAS_CATEGORIAS,
} from '../../../core/models/punto-interes.model';

interface CategoriaOption {
  label: string;
  value: Categoria;
}

type GpsState = 'idle' | 'loading' | 'success' | 'error';

@Component({
  selector: 'app-reporte-form',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule,
    Dialog,
    InputText,
    Select,
    Textarea,
    FloatLabel,
    Button,
    Message,
  ],
  templateUrl: './reporte-form.component.html',
  styleUrl: './reporte-form.component.css',
})
export class ReporteFormComponent {
  readonly visible = input.required<boolean>();
  readonly closed = output<void>();

  private readonly fb = inject(FormBuilder);
  private readonly zone = inject(NgZone);
  private readonly geoService = inject(GeolocationService);
  private readonly reporteService = inject(ReporteService);

  readonly form = this.fb.group({
    titulo: ['', [Validators.required, Validators.minLength(5)]],
    categoria: [null as Categoria | null, Validators.required],
    descripcion: ['', Validators.required],
  });

  readonly categorias: CategoriaOption[] = TODAS_LAS_CATEGORIAS.map((cat) => ({
    label: CATEGORY_CONFIG[cat].label,
    value: cat,
  }));

  private coordsLat: number | null = null;
  private coordsLng: number | null = null;

  readonly gpsState = signal<GpsState>('idle');
  readonly submitting = signal(false);
  readonly submitError = signal<string | null>(null);

  onDialogShow(): void {
    this.gpsState.set('loading');
    this.geoService
      .getUserPosition()
      .then((coords) => {
        this.zone.run(() => {
          this.coordsLat = coords.lat;
          this.coordsLng = coords.lng;
          this.gpsState.set('success');
        });
      })
      .catch(() => {
        this.zone.run(() => this.gpsState.set('error'));
      });
  }

  onDialogHide(): void {
    this.form.reset();
    this.coordsLat = null;
    this.coordsLng = null;
    this.gpsState.set('idle');
    this.submitError.set(null);
    this.submitting.set(false);
  }

  onCancel(): void {
    this.closed.emit();
  }

  onSubmit(): void {
    if (this.form.invalid || this.coordsLat === null || this.submitting()) return;

    const { titulo, categoria, descripcion } = this.form.getRawValue();

    const request: PuntoInteresRequest = {
      titulo: titulo!,
      categoria: categoria!,
      descripcion: descripcion!,
      latitud: this.coordsLat,
      longitud: this.coordsLng!,
    };

    this.submitting.set(true);
    this.submitError.set(null);

    this.reporteService.crear(request).subscribe({
      next: () => {
        this.submitting.set(false);
        this.closed.emit();
      },
      error: () => {
        this.submitting.set(false);
        this.submitError.set('No se pudo enviar el reporte. Intentá de nuevo.');
      },
    });
  }

  get tituloInvalid(): boolean {
    const c = this.form.get('titulo');
    return !!(c?.invalid && c.touched);
  }

  get categoriaInvalid(): boolean {
    const c = this.form.get('categoria');
    return !!(c?.invalid && c.touched);
  }

  get descripcionInvalid(): boolean {
    const c = this.form.get('descripcion');
    return !!(c?.invalid && c.touched);
  }
}
