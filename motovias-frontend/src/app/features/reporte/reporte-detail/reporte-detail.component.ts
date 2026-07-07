import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  signal,
} from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { DatePipe } from '@angular/common';
import { PrimeTemplate } from 'primeng/api';
import { Card } from 'primeng/card';
import { Timeline } from 'primeng/timeline';
import { PuntoInteresService } from '../../../core/services/punto-interes.service';
import { ReporteService } from '../../../core/services/reporte.service';
import {
  CATEGORY_CONFIG,
  ESTADO_CONFIG,
  PuntoInteres,
} from '../../../core/models/punto-interes.model';
import {
  ReporteEvento,
  TIPO_EVENTO_CONFIG,
} from '../../../core/models/reporte-evento.model';

interface EventoHistorialVista {
  id: number;
  tipoLabel: string;
  tipoIcon: string;
  descripcion: string;
  nombreUsuario: string;
  timestamp: string;
}

@Component({
  selector: 'app-reporte-detail',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DatePipe, RouterLink, Card, Timeline, PrimeTemplate],
  templateUrl: './reporte-detail.component.html',
  styleUrl: './reporte-detail.component.css',
})
export class ReporteDetailComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly puntoService = inject(PuntoInteresService);
  private readonly reporteService = inject(ReporteService);

  protected readonly punto = signal<PuntoInteres | null>(null);
  protected readonly cargando = signal(true);
  protected readonly error = signal(false);

  protected readonly historial = signal<ReporteEvento[]>([]);
  protected readonly cargandoHistorial = signal(true);

  protected readonly historialVista = computed<EventoHistorialVista[]>(() =>
    this.historial().map((evento) => ({
      id: evento.id,
      tipoLabel: TIPO_EVENTO_CONFIG[evento.tipoEvento].label,
      tipoIcon: TIPO_EVENTO_CONFIG[evento.tipoEvento].icon,
      descripcion: evento.descripcion,
      nombreUsuario: evento.nombreUsuario ?? 'Sistema',
      timestamp: evento.timestamp,
    })),
  );

  protected readonly CATEGORY_CONFIG = CATEGORY_CONFIG;
  protected readonly ESTADO_CONFIG = ESTADO_CONFIG;

  constructor() {
    const id = Number(this.route.snapshot.params['id']);

    this.puntoService.getById(id).subscribe({
      next: (p) => {
        this.punto.set(p);
        this.cargando.set(false);
      },
      error: () => {
        this.error.set(true);
        this.cargando.set(false);
      },
    });

    this.reporteService.obtenerHistorialReporte(id).subscribe({
      next: (eventos) => {
        this.historial.set(eventos);
        this.cargandoHistorial.set(false);
      },
      error: () => {
        this.cargandoHistorial.set(false);
      },
    });
  }
}
