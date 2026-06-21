import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  effect,
  ElementRef,
  inject,
  NgZone,
  input,
  output,
  signal,
  viewChild,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Subject, of } from 'rxjs';
import { catchError, debounceTime, distinctUntilChanged, switchMap } from 'rxjs';
import * as L from 'leaflet';
import { Badge } from 'primeng/badge';
import { Button } from 'primeng/button';
import { Dialog } from 'primeng/dialog';
import { FloatLabel } from 'primeng/floatlabel';
import { InputText } from 'primeng/inputtext';
import { Message } from 'primeng/message';
import { Select } from 'primeng/select';
import { Textarea } from 'primeng/textarea';
import { GeolocationService } from '../../../core/services/geolocation.service';
import { GeocodingService, NominatimResult } from '../../../core/services/geocoding.service';
import { ReporteService } from '../../../core/services/reporte.service';
import {
  Categoria,
  CATEGORY_CONFIG,
  FuenteUbicacion,
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
    Badge,
    Button,
    Dialog,
    FloatLabel,
    InputText,
    Message,
    Select,
    Textarea,
  ],
  templateUrl: './reporte-form.component.html',
  styleUrl: './reporte-form.component.css',
})
export class ReporteFormComponent {
  readonly visible = input.required<boolean>();
  readonly closed = output<void>();

  private readonly fb = inject(FormBuilder);
  private readonly zone = inject(NgZone);
  private readonly destroyRef = inject(DestroyRef);
  private readonly geoService = inject(GeolocationService);
  private readonly geocodingService = inject(GeocodingService);
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
  readonly fuenteUbicacion = signal<FuenteUbicacion>('GPS');
  readonly locationSelected = signal(false);
  readonly submitting = signal(false);
  readonly submitError = signal<string | null>(null);
  readonly searchValue = signal('');
  readonly geocodingResults = signal<NominatimResult[]>([]);
  readonly geocodingLoading = signal(false);

  private readonly searchQuery$ = new Subject<string>();
  readonly miniMapRef = viewChild<ElementRef>('miniMapContainer');

  private miniMap: L.Map | undefined;
  private miniMarker: L.Marker | undefined;

  constructor() {
    // Pipeline reactivo: debounce → cancelación con switchMap → geocodificación
    this.searchQuery$.pipe(
      debounceTime(400),
      distinctUntilChanged(),
      switchMap((query) => {
        if (query.trim().length < 3) {
          return of([] as NominatimResult[]);
        }
        this.zone.run(() => this.geocodingLoading.set(true));
        return this.geocodingService.search(query).pipe(
          catchError(() => of([] as NominatimResult[])),
        );
      }),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe((results) => {
      this.zone.run(() => {
        this.geocodingLoading.set(false);
        this.geocodingResults.set(results);
      });
    });

    // Inicializa el mini-mapa en cuanto el contenedor aparece en el DOM
    effect(() => {
      const el = this.miniMapRef()?.nativeElement;
      if (el && !this.miniMap) {
        this.zone.runOutsideAngular(() => this.initMiniMap(el));
      }
    });
  }

  onDialogShow(): void {
    this.gpsState.set('loading');
    this.geoService
      .getUserPosition()
      .then((coords) => {
        this.zone.run(() => {
          this.coordsLat = coords.lat;
          this.coordsLng = coords.lng;
          this.gpsState.set('success');
          this.fuenteUbicacion.set('GPS');
        });
      })
      .catch(() => {
        this.zone.run(() => {
          this.gpsState.set('error');
          this.fuenteUbicacion.set('MANUAL');
        });
      });
  }

  onDialogHide(): void {
    this.form.reset();
    this.coordsLat = null;
    this.coordsLng = null;
    this.gpsState.set('idle');
    this.fuenteUbicacion.set('GPS');
    this.locationSelected.set(false);
    this.submitError.set(null);
    this.submitting.set(false);
    this.searchValue.set('');
    this.geocodingResults.set([]);
    this.geocodingLoading.set(false);
    this.destroyMiniMap();
  }

  onCancel(): void {
    this.closed.emit();
  }

  onSearchInput(event: Event): void {
    const value = (event.target as HTMLInputElement).value;
    this.searchValue.set(value);
    if (!value.trim()) {
      this.geocodingResults.set([]);
    }
    this.searchQuery$.next(value);
  }

  onResultSelect(result: NominatimResult): void {
    const lat = parseFloat(result.lat);
    const lng = parseFloat(result.lon);
    this.coordsLat = lat;
    this.coordsLng = lng;
    this.locationSelected.set(true);
    this.geocodingResults.set([]);
    this.searchValue.set(result.display_name);

    this.zone.runOutsideAngular(() => {
      if (this.miniMap) {
        const latlng = L.latLng(lat, lng);
        this.miniMap.setView(latlng, 15);
        this.colocarMarcador(latlng);
      }
    });
  }

  onSubmit(): void {
    const locationReady = this.fuenteUbicacion() === 'GPS'
      ? this.gpsState() === 'success'
      : this.locationSelected();

    if (this.form.invalid || !locationReady || this.submitting()) return;
    if (this.coordsLat === null || this.coordsLng === null) return;

    const { titulo, categoria, descripcion } = this.form.getRawValue();

    const request: PuntoInteresRequest = {
      titulo: titulo!,
      categoria: categoria!,
      descripcion: descripcion!,
      latitud: this.coordsLat,
      longitud: this.coordsLng,
      fuenteUbicacion: this.fuenteUbicacion(),
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
        this.submitError.set('No se pudo publicar el aviso. Intentá de nuevo.');
      },
    });
  }

  get submitDisabled(): boolean {
    if (this.form.invalid || this.submitting()) return true;
    return this.fuenteUbicacion() === 'GPS'
      ? this.gpsState() !== 'success'
      : !this.locationSelected();
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

  private initMiniMap(el: HTMLElement): void {
    this.miniMap = L.map(el, {
      center: L.latLng(-34.6, -64.1),
      zoom: 5,
      zoomControl: true,
    });

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '© <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors',
      maxZoom: 19,
    }).addTo(this.miniMap);

    // Pequeño delay para que el contenedor tenga sus dimensiones reales en el DOM
    setTimeout(() => this.miniMap?.invalidateSize(), 0);

    this.miniMap.on('click', (e: L.LeafletMouseEvent) => {
      this.colocarMarcador(e.latlng);
      this.zone.run(() => {
        this.coordsLat = e.latlng.lat;
        this.coordsLng = e.latlng.lng;
        this.locationSelected.set(true);
      });
    });
  }

  private colocarMarcador(latlng: L.LatLng): void {
    if (!this.miniMap) return;

    if (this.miniMarker) {
      this.miniMarker.setLatLng(latlng);
    } else {
      this.miniMarker = L.marker(latlng, { draggable: true }).addTo(this.miniMap);
      this.miniMarker.on('dragend', () => {
        const pos = this.miniMarker!.getLatLng();
        this.zone.run(() => {
          this.coordsLat = pos.lat;
          this.coordsLng = pos.lng;
          this.locationSelected.set(true);
        });
      });
    }
  }

  private destroyMiniMap(): void {
    this.miniMarker?.remove();
    this.miniMarker = undefined;
    this.miniMap?.remove();
    this.miniMap = undefined;
  }
}
