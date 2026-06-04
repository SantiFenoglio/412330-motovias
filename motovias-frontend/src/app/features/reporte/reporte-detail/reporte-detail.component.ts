import {
  ChangeDetectionStrategy,
  Component,
  inject,
  signal,
} from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { DatePipe } from '@angular/common';
import { Card } from 'primeng/card';
import { PuntoInteresService } from '../../../core/services/punto-interes.service';
import {
  CATEGORY_CONFIG,
  ESTADO_CONFIG,
  PuntoInteres,
} from '../../../core/models/punto-interes.model';

@Component({
  selector: 'app-reporte-detail',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DatePipe, RouterLink, Card],
  templateUrl: './reporte-detail.component.html',
  styleUrl: './reporte-detail.component.css',
})
export class ReporteDetailComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly puntoService = inject(PuntoInteresService);

  protected readonly punto = signal<PuntoInteres | null>(null);
  protected readonly cargando = signal(true);
  protected readonly error = signal(false);

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
  }
}
