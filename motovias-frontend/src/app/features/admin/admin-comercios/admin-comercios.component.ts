import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  ElementRef,
  NgZone,
  OnInit,
  computed,
  effect,
  inject,
  signal,
  viewChild,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { Subject, of } from 'rxjs';
import { catchError, debounceTime, distinctUntilChanged, switchMap } from 'rxjs';
import * as L from 'leaflet';
import { MessageService, PrimeTemplate } from 'primeng/api';
import { TableModule } from 'primeng/table';
import { Select } from 'primeng/select';
import { InputText } from 'primeng/inputtext';
import { FloatLabel } from 'primeng/floatlabel';
import { Button } from 'primeng/button';
import { Dialog } from 'primeng/dialog';
import { Toast } from 'primeng/toast';
import { AdminComercioService } from '../../../core/services/admin-comercio.service';
import { GeocodingService, NominatimResult } from '../../../core/services/geocoding.service';
import {
  CATEGORIA_COMERCIO_CONFIG,
  CategoriaComercio,
  ComercioVerificado,
  ComercioVerificadoRequest,
  TODAS_LAS_CATEGORIAS_COMERCIO,
} from '../../../core/models/comercio-verificado.model';

interface CategoriaOption {
  label: string;
  value: CategoriaComercio;
}

interface EstadoFiltroOption {
  label: string;
  value: boolean | null;
}

@Component({
  selector: 'app-admin-comercios',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [MessageService],
  imports: [
    ReactiveFormsModule,
    FormsModule,
    TableModule,
    Select,
    InputText,
    FloatLabel,
    Button,
    Dialog,
    Toast,
    PrimeTemplate,
  ],
  templateUrl: './admin-comercios.component.html',
  styleUrl: './admin-comercios.component.css',
})
export class AdminComerciosComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly zone = inject(NgZone);
  private readonly destroyRef = inject(DestroyRef);
  private readonly adminComercioService = inject(AdminComercioService);
  private readonly geocodingService = inject(GeocodingService);
  private readonly messageService = inject(MessageService);

  readonly comercios = signal<ComercioVerificado[]>([]);
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly dialogVisible = signal(false);
  readonly editando = signal<ComercioVerificado | null>(null);
  readonly cambiandoEstadoId = signal<number | null>(null);

  readonly filtroCategoria = signal<CategoriaComercio | null>(null);
  readonly filtroEstado = signal<boolean | null>(null);

  readonly categoriaOptions: CategoriaOption[] = TODAS_LAS_CATEGORIAS_COMERCIO.map((value) => ({
    value,
    label: CATEGORIA_COMERCIO_CONFIG[value].label,
  }));

  readonly estadoOptions: EstadoFiltroOption[] = [
    { label: 'Activos', value: true },
    { label: 'Inactivos', value: false },
  ];

  readonly categoriaLabels: Record<CategoriaComercio, string> = Object.fromEntries(
    TODAS_LAS_CATEGORIAS_COMERCIO.map((c) => [c, CATEGORIA_COMERCIO_CONFIG[c].label]),
  ) as Record<CategoriaComercio, string>;

  readonly comerciosFiltrados = computed(() => {
    const categoria = this.filtroCategoria();
    const estado = this.filtroEstado();
    return this.comercios().filter((c) => {
      if (categoria && c.categoria !== categoria) return false;
      if (estado !== null && c.activo !== estado) return false;
      return true;
    });
  });

  readonly form = this.fb.group({
    nombre: ['', [Validators.required, Validators.minLength(3)]],
    direccion: ['', Validators.required],
    telefono: [''],
    categoria: [null as CategoriaComercio | null, Validators.required],
    latitud: [null as number | null, [Validators.required, Validators.min(-90), Validators.max(90)]],
    longitud: [null as number | null, [Validators.required, Validators.min(-180), Validators.max(180)]],
  });

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

    effect(() => {
      const el = this.miniMapRef()?.nativeElement;
      if (el && !this.miniMap) {
        this.zone.runOutsideAngular(() => this.initMiniMap(el));
      }
    });
  }

  ngOnInit(): void {
    this.cargar();
  }

  onFiltroCategoriaChange(categoria: CategoriaComercio | null): void {
    this.filtroCategoria.set(categoria);
  }

  onFiltroEstadoChange(estado: boolean | null): void {
    this.filtroEstado.set(estado);
  }

  abrirAlta(): void {
    this.editando.set(null);
    this.form.reset({ nombre: '', direccion: '', telefono: '', categoria: null, latitud: null, longitud: null });
    this.dialogVisible.set(true);
  }

  abrirEdicion(comercio: ComercioVerificado): void {
    this.editando.set(comercio);
    this.form.reset({
      nombre: comercio.nombre,
      direccion: comercio.direccion,
      telefono: comercio.telefono ?? '',
      categoria: comercio.categoria,
      latitud: comercio.latitud,
      longitud: comercio.longitud,
    });
    this.dialogVisible.set(true);
  }

  onDialogShow(): void {
    this.zone.runOutsideAngular(() => {
      setTimeout(() => this.miniMap?.invalidateSize(), 0);
      const { latitud, longitud } = this.form.getRawValue();
      if (latitud !== null && longitud !== null) {
        const latlng = L.latLng(latitud, longitud);
        this.miniMap?.setView(latlng, 14);
        this.colocarMarcador(latlng);
      }
    });
  }

  onDialogHide(): void {
    this.editando.set(null);
    this.form.reset();
    this.geocodingResults.set([]);
    this.geocodingLoading.set(false);
    this.zone.runOutsideAngular(() => {
      this.miniMarker?.remove();
      this.miniMarker = undefined;
    });
  }

  onDireccionInput(event: Event): void {
    const value = (event.target as HTMLInputElement).value;
    if (!value.trim()) {
      this.geocodingResults.set([]);
    }
    this.searchQuery$.next(value);
  }

  onResultSelect(result: NominatimResult): void {
    const lat = parseFloat(result.lat);
    const lng = parseFloat(result.lon);
    this.geocodingResults.set([]);
    this.form.patchValue({
      direccion: result.display_name,
      latitud: lat,
      longitud: lng,
    });

    this.zone.runOutsideAngular(() => {
      if (this.miniMap) {
        const latlng = L.latLng(lat, lng);
        this.miniMap.setView(latlng, 15);
        this.colocarMarcador(latlng);
      }
    });
  }

  onCoordenadasManualesChange(): void {
    const { latitud, longitud } = this.form.getRawValue();
    if (latitud === null || longitud === null || !this.miniMap) return;
    if (this.form.get('latitud')?.invalid || this.form.get('longitud')?.invalid) return;

    this.zone.runOutsideAngular(() => {
      const latlng = L.latLng(latitud, longitud);
      this.miniMap!.setView(latlng, this.miniMap!.getZoom());
      this.colocarMarcador(latlng);
    });
  }

  guardar(): void {
    if (this.form.invalid || this.saving()) return;

    const { nombre, direccion, telefono, categoria, latitud, longitud } = this.form.getRawValue();
    const request: ComercioVerificadoRequest = {
      nombre: nombre!,
      direccion: direccion!,
      telefono: telefono || undefined,
      categoria: categoria!,
      latitud: latitud!,
      longitud: longitud!,
    };

    this.saving.set(true);
    const comercioEditando = this.editando();
    const peticion = comercioEditando
      ? this.adminComercioService.editar(comercioEditando.id, request)
      : this.adminComercioService.crear(request);

    peticion.subscribe({
      next: (resultado) => {
        this.saving.set(false);
        this.dialogVisible.set(false);
        this.comercios.update((items) => {
          const existe = items.some((c) => c.id === resultado.id);
          return existe
            ? items.map((c) => (c.id === resultado.id ? resultado : c))
            : [resultado, ...items];
        });
        this.messageService.add({
          severity: 'success',
          summary: comercioEditando ? 'Comercio actualizado' : 'Comercio dado de alta',
          detail: `"${resultado.nombre}" se guardó correctamente.`,
          life: 4000,
        });
      },
      error: () => {
        this.saving.set(false);
        this.messageService.add({
          severity: 'error',
          summary: 'Error al guardar',
          detail: 'No se pudo guardar el comercio. Verificá los datos e intentá de nuevo.',
          life: 5000,
        });
      },
    });
  }

  toggleEstado(comercio: ComercioVerificado): void {
    if (this.cambiandoEstadoId() !== null) return;
    this.cambiandoEstadoId.set(comercio.id);

    this.adminComercioService.cambiarEstado(comercio.id, !comercio.activo).subscribe({
      next: (actualizado) => {
        this.comercios.update((items) => items.map((c) => (c.id === actualizado.id ? actualizado : c)));
        this.cambiandoEstadoId.set(null);
        this.messageService.add({
          severity: 'success',
          summary: actualizado.activo ? 'Comercio activado' : 'Comercio desactivado',
          detail: `"${actualizado.nombre}" ahora está ${actualizado.activo ? 'visible' : 'oculto'} en el mapa público.`,
          life: 4000,
        });
      },
      error: () => {
        this.cambiandoEstadoId.set(null);
        this.messageService.add({
          severity: 'error',
          summary: 'Error',
          detail: 'No se pudo cambiar el estado del comercio. Intentá de nuevo.',
          life: 5000,
        });
      },
    });
  }

  get nombreInvalid(): boolean {
    const c = this.form.get('nombre');
    return !!(c?.invalid && c.touched);
  }

  get direccionInvalid(): boolean {
    const c = this.form.get('direccion');
    return !!(c?.invalid && c.touched);
  }

  get categoriaInvalid(): boolean {
    const c = this.form.get('categoria');
    return !!(c?.invalid && c.touched);
  }

  get coordenadasInvalidas(): boolean {
    const lat = this.form.get('latitud');
    const lon = this.form.get('longitud');
    return !!((lat?.invalid && lat.touched) || (lon?.invalid && lon.touched));
  }

  private cargar(): void {
    this.loading.set(true);
    this.adminComercioService.listar().subscribe({
      next: (comercios) => {
        this.comercios.set(comercios);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.messageService.add({
          severity: 'error',
          summary: 'Error al cargar',
          detail: 'No se pudieron cargar los comercios verificados.',
          life: 5000,
        });
      },
    });
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

    this.miniMap.on('click', (e: L.LeafletMouseEvent) => {
      this.colocarMarcador(e.latlng);
      this.zone.run(() => {
        this.form.patchValue({ latitud: e.latlng.lat, longitud: e.latlng.lng });
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
          this.form.patchValue({ latitud: pos.lat, longitud: pos.lng });
        });
      });
    }
  }
}
