import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { UIChart } from 'primeng/chart';
import { TableModule } from 'primeng/table';
import {
  AdminMetricasResponse,
  AdminMetricasService,
} from '../../../core/services/admin-metricas.service';
import { CATEGORY_CONFIG } from '../../../core/models/punto-interes.model';

interface ChartDataset {
  label: string;
  data: number[];
  backgroundColor: string[];
  borderColor: string[];
  borderWidth: number;
  borderRadius: number;
}

interface ChartData {
  labels: string[];
  datasets: ChartDataset[];
}

@Component({
  selector: 'app-admin-dashboard',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [UIChart, TableModule],
  template: `
    <section class="dashboard" aria-label="Métricas de actividad de la plataforma">
      <div class="metric-cards">
        <article class="metric-card metric-card--activos">
          <i class="pi pi-map-marker metric-card__icon" aria-hidden="true"></i>
          <div class="metric-card__body">
            <span class="metric-card__value">
              {{ loading() ? '—' : (metricas()?.totalReportesActivos ?? '—') }}
            </span>
            <span class="metric-card__label">Total reportes activos</span>
          </div>
        </article>

        <article class="metric-card metric-card--semana">
          <i class="pi pi-calendar metric-card__icon" aria-hidden="true"></i>
          <div class="metric-card__body">
            <span class="metric-card__value">
              {{ loading() ? '—' : (metricas()?.reportesEstaSemana ?? '—') }}
            </span>
            <span class="metric-card__label">Reportes esta semana</span>
          </div>
        </article>

        <article class="metric-card metric-card--usuarios">
          <i class="pi pi-user-plus metric-card__icon" aria-hidden="true"></i>
          <div class="metric-card__body">
            <span class="metric-card__value">
              {{ loading() ? '—' : (metricas()?.usuariosNuevosEsteMes ?? '—') }}
            </span>
            <span class="metric-card__label">Nuevos usuarios este mes</span>
          </div>
        </article>
      </div>

      <div class="dashboard-panels">
        <div class="chart-panel">
          <h2 class="panel-title">Reportes por categoría</h2>
          @if (loading()) {
            <p class="panel-empty">Cargando métricas...</p>
          } @else if (error()) {
            <p class="panel-empty panel-empty--error">
              No se pudieron cargar las métricas. Intentá recargar la página.
            </p>
          } @else if (!chartData()) {
            <p class="panel-empty">No hay datos suficientes para graficar.</p>
          } @else {
            <p-chart
              type="bar"
              [data]="chartData()!"
              [options]="chartOptions"
              height="18rem"
            />
          }
        </div>

        <div class="zonas-panel">
          <h2 class="panel-title">Zonas con más actividad</h2>
          @if (loading()) {
            <p class="panel-empty">Cargando métricas...</p>
          } @else if (!metricas()?.zonasMasActivas?.length) {
            <p class="panel-empty">Sin datos geoespaciales agregados disponibles.</p>
          } @else {
            <p-table
              [value]="metricas()!.zonasMasActivas"
              styleClass="p-datatable-sm zonas-table"
              [tableStyle]="{ 'min-width': '18rem' }"
              aria-label="Tabla de zonas con mayor densidad de reportes"
            >
              <ng-template pTemplate="header">
                <tr>
                  <th scope="col" class="col-rank">#</th>
                  <th scope="col">Latitud</th>
                  <th scope="col">Longitud</th>
                  <th scope="col" class="col-right">Reportes</th>
                </tr>
              </ng-template>
              <ng-template pTemplate="body" let-zona let-rowIndex="rowIndex">
                <tr>
                  <td class="col-rank">{{ rowIndex + 1 }}</td>
                  <td>{{ zona.latitud !== null ? zona.latitud.toFixed(4) : '—' }}</td>
                  <td>{{ zona.longitud !== null ? zona.longitud.toFixed(4) : '—' }}</td>
                  <td class="col-right">{{ zona.cantidad }}</td>
                </tr>
              </ng-template>
            </p-table>
          }
        </div>
      </div>
    </section>
  `,
  styles: [`
    .dashboard {
      margin-bottom: 1.5rem;
    }

    .metric-cards {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(13rem, 1fr));
      gap: 1rem;
      margin-bottom: 1rem;
    }

    .metric-card {
      display: flex;
      align-items: center;
      gap: 0.875rem;
      background: #ffffff;
      border: 1px solid #e2e8f0;
      border-left-width: 4px;
      border-radius: 12px;
      padding: 1.125rem 1.25rem;
    }

    .metric-card--activos { border-left-color: #2563eb; }
    .metric-card--semana { border-left-color: #16a34a; }
    .metric-card--usuarios { border-left-color: #ea580c; }

    .metric-card__icon {
      font-size: 1.5rem;
      color: #3b82f6;
      background: #eff6ff;
      border-radius: 10px;
      padding: 0.625rem;
    }

    .metric-card--semana .metric-card__icon {
      color: #16a34a;
      background: #dcfce7;
    }

    .metric-card--usuarios .metric-card__icon {
      color: #ea580c;
      background: #ffedd5;
    }

    .metric-card__body {
      display: flex;
      flex-direction: column;
    }

    .metric-card__value {
      font-size: 1.5rem;
      font-weight: 800;
      color: #0f172a;
      line-height: 1.2;
    }

    .metric-card__label {
      font-size: 0.8125rem;
      color: #64748b;
    }

    .dashboard-panels {
      display: grid;
      grid-template-columns: 2fr 1fr;
      gap: 1rem;
    }

    .chart-panel,
    .zonas-panel {
      background: #ffffff;
      border: 1px solid #e2e8f0;
      border-radius: 12px;
      padding: 1.25rem;
    }

    .panel-title {
      font-size: 0.9375rem;
      font-weight: 700;
      color: #0f172a;
      margin: 0 0 1rem;
    }

    .panel-empty {
      font-size: 0.875rem;
      color: #94a3b8;
      font-style: italic;
      margin: 0;
    }

    .panel-empty--error {
      color: #b91c1c;
      font-style: normal;
    }

    .col-rank {
      width: 2.5rem;
      text-align: center;
      color: #64748b;
      font-weight: 600;
    }

    .col-right { text-align: right; }

    @media (max-width: 768px) {
      .dashboard-panels {
        grid-template-columns: 1fr;
      }
    }
  `],
})
export class AdminDashboardComponent implements OnInit {
  private readonly metricasService = inject(AdminMetricasService);

  readonly metricas = signal<AdminMetricasResponse | null>(null);
  readonly loading = signal(false);
  readonly error = signal(false);

  readonly chartOptions = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: { legend: { display: false } },
    scales: {
      y: { beginAtZero: true, ticks: { precision: 0 } },
      x: { grid: { display: false } },
    },
  };

  readonly chartData = computed<ChartData | null>(() => {
    const datos = this.metricas()?.reportesPorCategoria;
    if (!datos || datos.length === 0) return null;

    return {
      labels: datos.map((d) => CATEGORY_CONFIG[d.categoria]?.label ?? d.categoria),
      datasets: [
        {
          label: 'Reportes',
          data: datos.map((d) => d.cantidad),
          backgroundColor: datos.map((d) => CATEGORY_CONFIG[d.categoria]?.color ?? '#3b82f6'),
          borderColor: datos.map((d) => CATEGORY_CONFIG[d.categoria]?.color ?? '#3b82f6'),
          borderWidth: 1,
          borderRadius: 6,
        },
      ],
    };
  });

  ngOnInit(): void {
    this.cargar();
  }

  private cargar(): void {
    this.loading.set(true);
    this.error.set(false);
    this.metricasService.obtenerMetricas().subscribe({
      next: (metricas) => {
        this.metricas.set(metricas);
        this.loading.set(false);
      },
      error: () => {
        this.error.set(true);
        this.loading.set(false);
      },
    });
  }
}
