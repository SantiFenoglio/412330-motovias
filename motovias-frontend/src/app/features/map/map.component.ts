import {
  afterNextRender,
  ChangeDetectionStrategy,
  Component,
  computed,
  DestroyRef,
  effect,
  ElementRef,
  inject,
  NgZone,
  OnDestroy,
  signal,
  viewChild,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import * as L from 'leaflet';
import { ConfirmationService, MessageService, PrimeTemplate } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ConfirmDialog } from 'primeng/confirmdialog';
import { Dialog } from 'primeng/dialog';
import { Select } from 'primeng/select';
import { TextareaModule } from 'primeng/textarea';
import { Toast } from 'primeng/toast';
import { AuthService } from '../../core/services/auth.service';
import { GeolocationService, UserCoords } from '../../core/services/geolocation.service';
import { PuntoInteresService } from '../../core/services/punto-interes.service';
import { ReporteService } from '../../core/services/reporte.service';
import { ReporteWebSocketService } from '../../core/services/reporte-websocket.service';
import { Categoria, EstadoPunto, PuntoInteres } from '../../core/models/punto-interes.model';
import { FilterPanelComponent } from './filter-panel/filter-panel.component';
import { ReportePopupComponent } from './reporte-popup/reporte-popup.component';
import { ReporteFormComponent } from './reporte-form/reporte-form.component';

const CATEGORY_SVG: Record<Categoria, string> = {
  TALLER: `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none"
    stroke="#fff" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" width="20" height="20">
    <path d="M14.7 6.3a1 1 0 000 1.4l1.6 1.6a1 1 0 001.4 0l3.77-3.77a6 6 0 01-7.94 7.94l-6.91 6.91a2.12 2.12 0 01-3-3l6.91-6.91a6 6 0 017.94-7.94l-3.76 3.76z"/>
  </svg>`,
  GOMERIA: `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none"
    stroke="#fff" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" width="20" height="20">
    <circle cx="12" cy="12" r="10"/>
    <circle cx="12" cy="12" r="4"/>
    <line x1="12" y1="2"  x2="12" y2="8"/>
    <line x1="12" y1="16" x2="12" y2="22"/>
    <line x1="2"  y1="12" x2="8"  y2="12"/>
    <line x1="16" y1="12" x2="22" y2="12"/>
  </svg>`,
  ALERTA_SOS: `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none"
    stroke="#fff" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" width="20" height="20">
    <path d="M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z"/>
    <line x1="12" y1="9" x2="12" y2="13"/>
    <circle cx="12" cy="17" r="0.5" fill="#fff" stroke="#fff"/>
  </svg>`,
  PUNTO_INTERES: `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none"
    stroke="#fff" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" width="20" height="20">
    <path d="M4 15s1-1 4-1 5 2 8 2 4-1 4-1V3s-1 1-4 1-5-2-8-2-4 1-4 1z"/>
    <line x1="4" y1="22" x2="4" y2="15"/>
  </svg>`,
};

const CATEGORY_COLOR: Record<Categoria, string> = {
  TALLER:        '#2563eb',
  GOMERIA:       '#16a34a',
  ALERTA_SOS:    '#dc2626',
  PUNTO_INTERES: '#ea580c',
};

const CATEGORY_LABEL: Record<Categoria, string> = {
  TALLER:        'Taller mecánico',
  GOMERIA:       'Gomería',
  ALERTA_SOS:    'Emergencia S.O.S.',
  PUNTO_INTERES: 'Punto de interés / Parador',
};

const DUDOSO_SVG = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none"
  stroke="#fff" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" width="20" height="20">
  <path d="M9.09 9a3 3 0 0 1 5.83 1c0 2-3 3-3 3"/>
  <circle cx="12" cy="17" r="0.5" fill="#fff" stroke="#fff"/>
</svg>`;

const ARGENTINA_CENTER: [number, number] = [-34.6, -64.1];

interface EstadoOption {
  label: string;
  value: EstadoPunto;
}

@Component({
  selector: 'app-map',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [ConfirmationService, MessageService],
  imports: [
    ReactiveFormsModule,
    ButtonModule,
    FilterPanelComponent,
    ReportePopupComponent,
    ReporteFormComponent,
    Dialog,
    ConfirmDialog,
    Toast,
    Select,
    TextareaModule,
    PrimeTemplate,
  ],
  templateUrl: './map.component.html',
  styleUrl: './map.component.css',
})
export class MapComponent implements OnDestroy {
  readonly mapRef = viewChild.required<ElementRef>('mapContainer');

  private readonly zone = inject(NgZone);
  private readonly destroyRef = inject(DestroyRef);
  private readonly geolocationService = inject(GeolocationService);
  readonly puntoInteresService = inject(PuntoInteresService);
  private readonly wsService = inject(ReporteWebSocketService);
  private readonly authService = inject(AuthService);
  private readonly reporteService = inject(ReporteService);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly messageService = inject(MessageService);
  private readonly fb = inject(FormBuilder);

  private map: L.Map | undefined;
  private readonly leafletMarkers = new Map<number, L.Marker>();
  private userLocationMarker: L.Marker | undefined;

  readonly selectedPunto = signal<PuntoInteres | null>(null);
  readonly showReporteForm = signal(false);
  readonly editandoPunto = signal<PuntoInteres | null>(null);
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

  readonly esOwner = computed(() => {
    const user = this.authService.currentUser();
    const punto = this.selectedPunto();
    return !!user && !!punto?.emailUsuario && punto.emailUsuario === user.email;
  });

  readonly esAdmin = computed(() =>
    this.authService.currentUser()?.roles.includes('ROLE_ADMIN') ?? false,
  );

  readonly puedeVotar = computed(() => {
    const user = this.authService.currentUser();
    const punto = this.selectedPunto();
    return !!user && !!punto && punto.emailUsuario !== user.email;
  });

  constructor() {
    effect(() => {
      const filtrados = this.puntoInteresService.puntosFiltrados();
      if (this.map) {
        this.zone.runOutsideAngular(() => this.sincronizarMarcadores(filtrados));
      }
    });

    afterNextRender(() => {
      this.zone.runOutsideAngular(() => {
        this.initMap();
        this.sincronizarMarcadores(this.puntoInteresService.puntosFiltrados());
      });
    });

    // Canal /topic/reportes: tanto nuevos como editados
    this.wsService.reporteUpserted$
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((reporte) => {
        this.puntoInteresService.upsertReporte(reporte);
        if (this.leafletMarkers.has(reporte.id)) {
          this.zone.runOutsideAngular(() => this.actualizarMarcadorEnMapa(reporte));
        }
        if (this.selectedPunto()?.id === reporte.id) {
          this.selectedPunto.set(reporte);
        }
      });

    // Canal /topic/reportes/eliminar
    this.wsService.reporteEliminado$
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((id) => {
        this.zone.runOutsideAngular(() => {
          const marker = this.leafletMarkers.get(id);
          if (marker) {
            marker.remove();
            this.leafletMarkers.delete(id);
          }
        });
        this.puntoInteresService.eliminarReporte(id);
        if (this.selectedPunto()?.id === id) {
          this.selectedPunto.set(null);
        }
        if (this.editandoPunto()?.id === id) {
          this.editandoPunto.set(null);
        }
      });
  }

  ngOnDestroy(): void {
    this.map?.remove();
    this.map = undefined;
  }

  recenter(): void {
    if (!this.map) return;
    this.centerOnUser();
  }

  onEditClicked(): void {
    const p = this.selectedPunto();
    if (!p) return;
    this.selectedPunto.set(null);
    this.editForm.reset({ descripcion: p.descripcion ?? '', estado: p.estado ?? 'ACTIVO' });
    this.saveError.set(null);
    this.editandoPunto.set(p);
  }

  onDeleteClicked(): void {
    const p = this.selectedPunto();
    if (!p) return;
    this.selectedPunto.set(null);

    this.confirmationService.confirm({
      message: `¿Querés eliminar "<strong>${p.titulo}</strong>"? Esta acción no se puede deshacer.`,
      header: 'Confirmar eliminación',
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: 'Sí, eliminar',
      rejectLabel: 'Cancelar',
      accept: () => {
        this.reporteService.deleteReporte(p.id)
          .pipe(takeUntilDestroyed(this.destroyRef))
          .subscribe({
            error: () => {
              this.messageService.add({
                severity: 'error',
                summary: 'Error',
                detail: 'No se pudo eliminar la publicación. Intentá de nuevo.',
                life: 4000,
              });
            },
          });
      },
    });
  }

  guardarEdicion(): void {
    const p = this.editandoPunto();
    if (!p || this.editForm.invalid || this.saving()) return;

    const { descripcion, estado } = this.editForm.getRawValue();
    this.saving.set(true);
    this.saveError.set(null);

    this.reporteService.updateReporte(p.id, { descripcion: descripcion ?? '', estado: estado! })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.saving.set(false);
          this.editandoPunto.set(null);
        },
        error: () => {
          this.saving.set(false);
          this.saveError.set('No se pudo guardar. Intentá de nuevo.');
        },
      });
  }

  cancelarEdicion(): void {
    if (!this.editandoPunto()) return;
    this.editandoPunto.set(null);
    this.saveError.set(null);
  }

  onVotoClicked(tipo: 'CONFIRMA' | 'REFUTA'): void {
    const p = this.selectedPunto();
    if (!p) return;
    this.reporteService.votarReporte(p.id, tipo)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.messageService.add({
            severity: 'success',
            summary: 'Voto registrado',
            detail: tipo === 'CONFIRMA' ? 'Confirmaste este aviso.' : 'Refutaste este aviso.',
            life: 2500,
          });
        },
        error: () => {
          this.messageService.add({
            severity: 'error',
            summary: 'Error',
            detail: 'No se pudo registrar tu voto. Puede que ya hayas votado.',
            life: 4000,
          });
        },
      });
  }

  private initMap(): void {
    this.map = L.map(this.mapRef().nativeElement, {
      center: ARGENTINA_CENTER,
      zoom: 5,
      zoomControl: true,
    });

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution:
        '© <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors',
      maxZoom: 19,
    }).addTo(this.map);

    this.centerOnUser();
  }

  private centerOnUser(): void {
    this.geolocationService
      .getUserPosition()
      .then((coords) => {
        this.zone.runOutsideAngular(() => {
          this.map!.setView([coords.lat, coords.lng], 13);
          this.userLocationMarker?.remove();
          this.userLocationMarker = this.crearMarcadorUsuario(coords);
          this.userLocationMarker.addTo(this.map!);
        });
      })
      .catch(() => {
        this.zone.runOutsideAngular(() => {
          this.map!.setView(ARGENTINA_CENTER, 5);
        });
      });
  }

  private crearMarcadorUsuario(coords: UserCoords): L.Marker {
    const icon = L.divIcon({
      className: '',
      html: `<div class="user-location-pin" role="img" aria-label="Mi ubicación">
               <div class="user-location-pulse"></div>
               <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="20" height="20">
                 <circle cx="12" cy="12" r="9" fill="#1d4ed8" stroke="#ffffff" stroke-width="3"/>
                 <circle cx="12" cy="12" r="3.5" fill="#ffffff"/>
               </svg>
             </div>`,
      iconSize: [40, 40],
      iconAnchor: [20, 20],
    });
    return L.marker([coords.lat, coords.lng], { icon, zIndexOffset: 1000 });
  }

  private crearIcono(punto: PuntoInteres): L.DivIcon {
    const isDudoso = punto.estado === 'DUDOSO';
    const color = isDudoso ? '#94a3b8' : CATEGORY_COLOR[punto.categoria];
    const label = isDudoso ? 'Aviso dudoso — información en disputa' : CATEGORY_LABEL[punto.categoria];
    const svg = isDudoso ? DUDOSO_SVG : CATEGORY_SVG[punto.categoria];
    const extraStyle = isDudoso
      ? 'opacity:0.55;'
      : punto.estado === 'RESUELTO'
        ? ''
        : '';
    const resoltoClass = punto.estado === 'RESUELTO' ? ' moto-pin--resuelto' : '';
    return L.divIcon({
      className: '',
      html: `<div class="moto-pin${resoltoClass}" style="background:${color};${extraStyle}" role="img" aria-label="${label}">
               ${svg}
             </div>`,
      iconSize: [40, 40],
      iconAnchor: [20, 40],
    });
  }

  private crearMarcador(punto: PuntoInteres): L.Marker {
    const marker = L.marker([punto.latitud, punto.longitud], { icon: this.crearIcono(punto) });
    marker.on('click', () => {
      this.zone.run(() => this.selectedPunto.set(punto));
    });
    return marker;
  }

  private actualizarMarcadorEnMapa(punto: PuntoInteres): void {
    const marker = this.leafletMarkers.get(punto.id);
    if (!marker) return;
    marker.setIcon(this.crearIcono(punto));
    marker.off('click');
    marker.on('click', () => {
      this.zone.run(() => this.selectedPunto.set(punto));
    });
  }

  private sincronizarMarcadores(filtrados: PuntoInteres[]): void {
    const filtradosIds = new Set(filtrados.map((p) => p.id));

    this.leafletMarkers.forEach((marker, id) => {
      if (!filtradosIds.has(id)) {
        marker.remove();
        this.leafletMarkers.delete(id);
      }
    });

    for (const punto of filtrados) {
      if (!this.leafletMarkers.has(punto.id)) {
        const marker = this.crearMarcador(punto);
        marker.addTo(this.map!);
        this.leafletMarkers.set(punto.id, marker);
      }
    }
  }
}
