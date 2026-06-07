import {
  afterNextRender,
  ChangeDetectionStrategy,
  Component,
  effect,
  ElementRef,
  inject,
  NgZone,
  OnDestroy,
  signal,
  viewChild,
} from '@angular/core';
import * as L from 'leaflet';
import { PuntoInteresService } from '../../core/services/punto-interes.service';
import { Categoria, PuntoInteres } from '../../core/models/punto-interes.model';
import { GeolocationService, UserCoords } from '../../core/services/geolocation.service';
import { Button } from 'primeng/button';
import { FilterPanelComponent } from './filter-panel/filter-panel.component';
import { ReportePopupComponent } from './reporte-popup/reporte-popup.component';

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

const ARGENTINA_CENTER: [number, number] = [-34.6, -64.1];

@Component({
  selector: 'app-map',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Button, FilterPanelComponent, ReportePopupComponent],
  templateUrl: './map.component.html',
  styleUrl: './map.component.css',
})
export class MapComponent implements OnDestroy {
  readonly mapRef = viewChild.required<ElementRef>('mapContainer');

  private readonly zone = inject(NgZone);
  private readonly geolocationService = inject(GeolocationService);
  readonly puntoInteresService = inject(PuntoInteresService);

  private map: L.Map | undefined;
  private readonly leafletMarkers = new Map<number, L.Marker>();
  private userLocationMarker: L.Marker | undefined;

  readonly selectedPunto = signal<PuntoInteres | null>(null);

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
  }

  ngOnDestroy(): void {
    this.map?.remove();
    this.map = undefined;
  }

  recenter(): void {
    if (!this.map) return;
    this.centerOnUser();
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

  private crearMarcador(punto: PuntoInteres): L.Marker {
    const color = CATEGORY_COLOR[punto.categoria];
    const label = CATEGORY_LABEL[punto.categoria];
    const svg = CATEGORY_SVG[punto.categoria];

    const icon = L.divIcon({
      className: '',
      html: `<div class="moto-pin" style="background:${color}" role="img" aria-label="${label}">
               ${svg}
             </div>`,
      iconSize: [40, 40],
      iconAnchor: [20, 40],
    });

    const marker = L.marker([punto.latitud, punto.longitud], { icon });
    marker.on('click', () => {
      this.zone.run(() => this.selectedPunto.set(punto));
    });
    return marker;
  }
}
