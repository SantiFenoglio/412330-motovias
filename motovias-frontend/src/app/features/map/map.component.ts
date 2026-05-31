import {
  afterNextRender,
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  inject,
  NgZone,
  OnDestroy,
  viewChild,
} from '@angular/core';
import { Router } from '@angular/router';
import * as L from 'leaflet';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-map',
  templateUrl: './map.component.html',
  styleUrl: './map.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MapComponent implements OnDestroy {
  private readonly mapRef = viewChild.required<ElementRef<HTMLDivElement>>('mapContainer');
  private readonly zone = inject(NgZone);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  private map?: L.Map;

  constructor() {
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
}
