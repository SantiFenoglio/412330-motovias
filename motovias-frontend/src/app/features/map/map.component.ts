import {
  afterNextRender,
  ChangeDetectionStrategy,
  Component,
  effect,
  ElementRef,
  inject,
  NgZone,
  OnDestroy,
  viewChild,
} from '@angular/core';
import { Router } from '@angular/router';
import * as L from 'leaflet';
import { AuthService } from '../../core/services/auth.service';
import { PuntoInteresService } from '../../core/services/punto-interes.service';
import { Categoria, PuntoInteres } from '../../core/models/punto-interes.model';
import { FilterPanelComponent } from './filter-panel/filter-panel.component';

// SVG inline strings for Leaflet divIcon – kept here because they are
// only needed for marker rendering, not for the filter UI.
const CATEGORY_SVG: Record<Categoria, string> = {
  // Wrench / llave inglesa
  TALLER: `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none"
    stroke="#fff" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" width="20" height="20">
    <path d="M14.7 6.3a1 1 0 000 1.4l1.6 1.6a1 1 0 001.4 0l3.77-3.77a6 6 0 01-7.94 7.94l-6.91 6.91a2.12 2.12 0 01-3-3l6.91-6.91a6 6 0 017.94-7.94l-3.76 3.76z"/>
  </svg>`,
  // Tire / neumático con radios
  GOMERIA: `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none"
    stroke="#fff" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" width="20" height="20">
    <circle cx="12" cy="12" r="10"/>
    <circle cx="12" cy="12" r="4"/>
    <line x1="12" y1="2"  x2="12" y2="8"/>
    <line x1="12" y1="16" x2="12" y2="22"/>
    <line x1="2"  y1="12" x2="8"  y2="12"/>
    <line x1="16" y1="12" x2="22" y2="12"/>
  </svg>`,
  // SOS triangle with bold exclamation
  ALERTA_SOS: `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none"
    stroke="#fff" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" width="20" height="20">
    <path d="M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z"/>
    <line x1="12" y1="9" x2="12" y2="13"/>
    <circle cx="12" cy="17" r="0.5" fill="#fff" stroke="#fff"/>
  </svg>`,
  // Campsite flag / bandera de parador
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

@Component({
  selector: 'app-map',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FilterPanelComponent],
  templateUrl: './map.component.html',
  styleUrl: './map.component.css',
})
export class MapComponent implements OnDestroy {
  readonly mapRef = viewChild.required<ElementRef>('mapContainer');

  private readonly zone = inject(NgZone);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  readonly puntoInteresService = inject(PuntoInteresService);

  private map: L.Map | undefined;
  private readonly leafletMarkers = new Map<number, L.Marker>();

  constructor() {
    effect(() => {
      const filtrados = this.puntoInteresService.puntosFiltrados();
      if (this.map) {
        this.zone.runOutsideAngular(() => this.sincronizarMarcadores(filtrados));
      }
    });

    afterNextRender(() => {
      this.zone.runOutsideAngular(() => this.initMap());
    });
  }

  ngOnDestroy(): void {
    this.map?.remove();
    this.map = undefined;
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/auth/login']);
  }

  private initMap(): void {
    this.map = L.map(this.mapRef().nativeElement, {
      center: [-31.4135, -64.1811],
      zoom: 13,
      zoomControl: true,
    });

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution:
        '© <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors',
      maxZoom: 19,
    }).addTo(this.map);
  }

  private sincronizarMarcadores(filtrados: PuntoInteres[]): void {
    const filtradosIds = new Set(filtrados.map((p) => p.id));

    // Remove markers no longer in the filtered list
    this.leafletMarkers.forEach((marker, id) => {
      if (!filtradosIds.has(id)) {
        marker.remove();
        this.leafletMarkers.delete(id);
      }
    });

    // Add new markers
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
      popupAnchor: [0, -42],
    });

    const marker = L.marker([punto.latitud, punto.longitud], { icon });
    const desc = punto.descripcion
      ? `<br><span class="popup-desc">${punto.descripcion}</span>`
      : '';
    marker.bindPopup(
      `<strong class="popup-titulo">${punto.titulo}</strong>
       <br><span class="popup-cat" style="color:${color}">${label}</span>
       ${desc}`,
    );
    return marker;
  }
}
