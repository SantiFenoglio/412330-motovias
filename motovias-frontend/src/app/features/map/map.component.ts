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
import { Router } from '@angular/router';
import * as L from 'leaflet';
import { AuthService } from '../../core/services/auth.service';
import { PuntoInteresService } from '../../core/services/punto-interes.service';
import { Categoria, PuntoInteres } from '../../core/models/punto-interes.model';

interface CategoriaConfig {
  emoji: string;
  label: string;
  color: string;
}

const CATEGORY_CONFIG: Record<Categoria, CategoriaConfig> = {
  ACCIDENTE:       { emoji: '🚨', label: 'Accidente',        color: '#ef4444' },
  OBRA:            { emoji: '🚧', label: 'Obra',              color: '#f59e0b' },
  TRAMPA_POLICIAL: { emoji: '👮', label: 'Control policial',  color: '#3b82f6' },
  SEMAFORO_ROTO:   { emoji: '🚦', label: 'Semáforo roto',    color: '#8b5cf6' },
  PIQUETE:         { emoji: '✊', label: 'Piquete',            color: '#ec4899' },
  PELIGRO:         { emoji: '⚠️', label: 'Peligro en vía',   color: '#f97316' },
};

@Component({
  selector: 'app-map',
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './map.component.html',
  styleUrl: './map.component.css',
})
export class MapComponent implements OnDestroy {
  readonly mapRef = viewChild.required<ElementRef>('mapContainer');

  private readonly zone = inject(NgZone);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly puntoInteresService = inject(PuntoInteresService);

  private map: L.Map | undefined;
  private readonly leafletMarkers = new Map<number, L.Marker>();

  readonly puntos = signal<PuntoInteres[]>([]);
  readonly categoriasActivas = signal<Categoria[]>(
    Object.keys(CATEGORY_CONFIG) as Categoria[],
  );

  readonly todasLasCategorias: [Categoria, CategoriaConfig][] = Object.entries(
    CATEGORY_CONFIG,
  ) as [Categoria, CategoriaConfig][];

  constructor() {
    // Reacciona automáticamente a cambios en puntos o filtros activos
    effect(() => {
      const puntos = this.puntos();
      const activas = this.categoriasActivas();
      if (this.map) {
        this.zone.runOutsideAngular(() => this.sincronizarMarcadores(puntos, activas));
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

  esCategoriaActiva(cat: Categoria): boolean {
    return this.categoriasActivas().includes(cat);
  }

  toggleCategoria(cat: Categoria): void {
    this.categoriasActivas.update((current) =>
      current.includes(cat) ? current.filter((c) => c !== cat) : [...current, cat],
    );
  }

  getCategoriaColor(cat: Categoria): string {
    return CATEGORY_CONFIG[cat].color;
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

    this.cargarPuntos();
  }

  private cargarPuntos(): void {
    // Re-entramos a la zona Angular para que HttpClient funcione correctamente
    this.zone.run(() => {
      this.puntoInteresService.listarTodos().subscribe({
        next: (puntos) => this.puntos.set(puntos),
        // Si el backend no está disponible el mapa carga igualmente sin marcadores
        error: () => {},
      });
    });
  }

  private sincronizarMarcadores(puntos: PuntoInteres[], activas: Categoria[]): void {
    // Eliminar marcadores de categorías desactivadas o puntos removidos
    this.leafletMarkers.forEach((marker, id) => {
      const punto = puntos.find((p) => p.id === id);
      if (!punto || !activas.includes(punto.categoria)) {
        marker.remove();
        this.leafletMarkers.delete(id);
      }
    });

    // Agregar marcadores nuevos o reactivados
    for (const punto of puntos) {
      if (activas.includes(punto.categoria) && !this.leafletMarkers.has(punto.id)) {
        const marker = this.crearMarcador(punto);
        marker.addTo(this.map!);
        this.leafletMarkers.set(punto.id, marker);
      }
    }
  }

  private crearMarcador(punto: PuntoInteres): L.Marker {
    const cfg = CATEGORY_CONFIG[punto.categoria];
    const icon = L.divIcon({
      className: '',
      html: `<div class="moto-pin" style="background:${cfg.color}" role="img" aria-label="${cfg.label}">
               <span aria-hidden="true">${cfg.emoji}</span>
             </div>`,
      iconSize: [36, 36],
      iconAnchor: [18, 36],
      popupAnchor: [0, -38],
    });

    const marker = L.marker([punto.latitud, punto.longitud], { icon });
    const desc = punto.descripcion ? `<br><span class="popup-desc">${punto.descripcion}</span>` : '';
    marker.bindPopup(
      `<strong class="popup-titulo">${punto.titulo}</strong>
       <br><span class="popup-cat" style="color:${cfg.color}">${cfg.emoji} ${cfg.label}</span>
       ${desc}`,
    );
    return marker;
  }
}
