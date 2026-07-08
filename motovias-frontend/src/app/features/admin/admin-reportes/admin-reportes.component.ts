import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DatePipe } from '@angular/common';
import { MessageService, PrimeTemplate } from 'primeng/api';
import { TableModule, TableLazyLoadEvent } from 'primeng/table';
import { Select } from 'primeng/select';
import { DatePicker } from 'primeng/datepicker';
import { Toast } from 'primeng/toast';
import { AdminService, AdminReporteItem } from '../../../core/services/admin.service';
import {
  Categoria,
  CATEGORY_CONFIG,
  EstadoPunto,
  ESTADO_CONFIG,
  TODAS_LAS_CATEGORIAS,
  TODOS_LOS_ESTADOS,
} from '../../../core/models/punto-interes.model';

interface OpcionFiltro<T> {
  label: string;
  value: T;
}

@Component({
  selector: 'app-admin-reportes',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [MessageService],
  imports: [FormsModule, DatePipe, TableModule, Select, DatePicker, Toast, PrimeTemplate],
  template: `
    <div class="page-shell">
      <header class="page-header">
        <h1 class="page-title">
          <i class="pi pi-shield" aria-hidden="true"></i>
          Panel de Administración y Moderación
        </h1>
        <p class="page-subtitle">
          Gestioná el estado de los reportes publicados por la comunidad.
        </p>
      </header>

      <section class="filtros" aria-label="Filtros de búsqueda de reportes">
        <div class="filtro-campo">
          <label for="filtro-categoria" class="filtro-label">Categoría</label>
          <p-select
            inputId="filtro-categoria"
            [options]="categoriaOptions"
            optionLabel="label"
            optionValue="value"
            [showClear]="true"
            placeholder="Todas las categorías"
            [style]="{ width: '100%' }"
            appendTo="body"
            [ngModel]="filtroCategoria()"
            (ngModelChange)="onFiltroCategoriaChange($event)"
          />
        </div>

        <div class="filtro-campo">
          <label for="filtro-estado" class="filtro-label">Estado</label>
          <p-select
            inputId="filtro-estado"
            [options]="estadoOptions"
            optionLabel="label"
            optionValue="value"
            [showClear]="true"
            placeholder="Todos los estados"
            [style]="{ width: '100%' }"
            appendTo="body"
            [ngModel]="filtroEstado()"
            (ngModelChange)="onFiltroEstadoChange($event)"
          />
        </div>

        <div class="filtro-campo">
          <label for="filtro-fechas" class="filtro-label">Rango de fechas</label>
          <p-datepicker
            inputId="filtro-fechas"
            selectionMode="range"
            [readonlyInput]="true"
            [showIcon]="true"
            placeholder="Seleccioná un rango"
            [style]="{ width: '100%' }"
            appendTo="body"
            [ngModel]="rangoFechas()"
            (ngModelChange)="onRangoFechasChange($event)"
          />
        </div>
      </section>

      <section class="tabla-wrapper" aria-label="Listado de reportes de la comunidad">
        <p-table
          [value]="reportes()"
          [lazy]="true"
          (onLazyLoad)="onLazyLoad($event)"
          [paginator]="true"
          [rows]="rows()"
          [totalRecords]="totalRecords()"
          [loading]="loading()"
          [rowsPerPageOptions]="[10, 25, 50]"
          styleClass="p-datatable-striped"
          [tableStyle]="{ 'min-width': '860px' }"
          aria-label="Tabla de moderación de reportes"
        >
          <ng-template pTemplate="header">
            <tr>
              <th scope="col">ID</th>
              <th scope="col">Título</th>
              <th scope="col">Categoría</th>
              <th scope="col">Usuario creador</th>
              <th scope="col">Fecha</th>
              <th scope="col" class="col-right">Votos</th>
              <th scope="col">Estado</th>
            </tr>
          </ng-template>
          <ng-template pTemplate="body" let-reporte>
            <tr>
              <td>{{ reporte.id }}</td>
              <td>{{ reporte.titulo }}</td>
              <td>
                <span class="categoria-badge">
                  {{ $any(categoriaLabels)[reporte.categoria] ?? reporte.categoria }}
                </span>
              </td>
              <td>{{ reporte.nombreUsuarioCreador ?? reporte.emailUsuarioCreador ?? '—' }}</td>
              <td>{{ reporte.fechaCreacion | date: 'dd/MM/yyyy HH:mm':'-0300' }}</td>
              <td class="col-right">{{ reporte.votos }}</td>
              <td>
                <p-select
                  [options]="estadoOptions"
                  optionLabel="label"
                  optionValue="value"
                  [style]="{ width: '11rem' }"
                  appendTo="body"
                  [disabled]="cambiandoEstadoId() === reporte.id"
                  [ngModel]="reporte.estado"
                  (ngModelChange)="cambiarEstado(reporte, $event)"
                  [attr.aria-label]="'Cambiar estado del reporte ' + reporte.id"
                />
              </td>
            </tr>
          </ng-template>
          <ng-template pTemplate="emptymessage">
            <tr>
              <td colspan="7" class="empty-table-row">
                No hay reportes que coincidan con los filtros seleccionados.
              </td>
            </tr>
          </ng-template>
        </p-table>
      </section>
    </div>

    <p-toast position="bottom-center" />
  `,
  styles: [`
    .page-shell {
      max-width: 72rem;
      margin: 0 auto;
      padding: 2rem 1.25rem 3rem;
    }

    .page-header { margin-bottom: 1.5rem; }

    .page-title {
      display: flex;
      align-items: center;
      gap: 0.625rem;
      font-size: 1.625rem;
      font-weight: 800;
      color: #0f172a;
      margin: 0 0 0.375rem;
    }
    .page-title .pi { color: #3b82f6; font-size: 1.5rem; }

    .page-subtitle {
      font-size: 0.9375rem;
      color: #64748b;
      margin: 0;
    }

    .filtros {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(14rem, 1fr));
      gap: 1rem;
      background: #ffffff;
      border: 1px solid #e2e8f0;
      border-radius: 12px;
      padding: 1.25rem;
      margin-bottom: 1.5rem;
    }

    .filtro-campo {
      display: flex;
      flex-direction: column;
      gap: 0.375rem;
    }

    .filtro-label {
      font-size: 0.8125rem;
      font-weight: 600;
      color: #374151;
    }

    .tabla-wrapper {
      background: #ffffff;
      border: 1px solid #e2e8f0;
      border-radius: 12px;
      padding: 1.5rem;
      overflow-x: auto;
    }

    .col-right { text-align: right !important; }

    .categoria-badge {
      display: inline-block;
      padding: 0.1875rem 0.625rem;
      background: #f1f5f9;
      border: 1px solid #e2e8f0;
      border-radius: 9999px;
      font-size: 0.75rem;
      font-weight: 600;
      color: #475569;
      text-transform: uppercase;
      letter-spacing: 0.04em;
      white-space: nowrap;
    }

    .empty-table-row {
      text-align: center;
      color: #94a3b8;
      font-style: italic;
      padding: 2rem;
    }
  `],
})
export class AdminReportesComponent implements OnInit {
  private readonly adminService = inject(AdminService);
  private readonly messageService = inject(MessageService);

  readonly reportes = signal<AdminReporteItem[]>([]);
  readonly totalRecords = signal(0);
  readonly loading = signal(false);
  readonly rows = signal(10);
  readonly first = signal(0);
  readonly cambiandoEstadoId = signal<number | null>(null);

  readonly filtroCategoria = signal<Categoria | null>(null);
  readonly filtroEstado = signal<EstadoPunto | null>(null);
  readonly rangoFechas = signal<Date[] | null>(null);

  readonly categoriaLabels: Record<Categoria, string> = Object.fromEntries(
    TODAS_LAS_CATEGORIAS.map((c) => [c, CATEGORY_CONFIG[c].label]),
  ) as Record<Categoria, string>;

  readonly categoriaOptions: OpcionFiltro<Categoria>[] = TODAS_LAS_CATEGORIAS.map((value) => ({
    value,
    label: CATEGORY_CONFIG[value].label,
  }));

  readonly estadoOptions: OpcionFiltro<EstadoPunto>[] = TODOS_LOS_ESTADOS.map((value) => ({
    value,
    label: ESTADO_CONFIG[value].label,
  }));

  ngOnInit(): void {
    this.cargar();
  }

  onLazyLoad(event: TableLazyLoadEvent): void {
    this.first.set(event.first ?? 0);
    this.rows.set(event.rows ?? this.rows());
    this.cargar();
  }

  onFiltroCategoriaChange(categoria: Categoria | null): void {
    this.filtroCategoria.set(categoria);
    this.first.set(0);
    this.cargar();
  }

  onFiltroEstadoChange(estado: EstadoPunto | null): void {
    this.filtroEstado.set(estado);
    this.first.set(0);
    this.cargar();
  }

  onRangoFechasChange(rango: Date[] | null): void {
    this.rangoFechas.set(rango);
    if (!rango || (rango[0] && rango[1])) {
      this.first.set(0);
      this.cargar();
    }
  }

  cambiarEstado(reporte: AdminReporteItem, nuevoEstado: EstadoPunto): void {
    if (nuevoEstado === reporte.estado || this.cambiandoEstadoId() !== null) return;

    this.cambiandoEstadoId.set(reporte.id);

    this.adminService.cambiarEstadoReporte(reporte.id, nuevoEstado).subscribe({
      next: (actualizado) => {
        this.reportes.update((items) =>
          items.map((r) => (r.id === actualizado.id ? actualizado : r)),
        );
        this.cambiandoEstadoId.set(null);
        this.messageService.add({
          severity: 'success',
          summary: 'Estado actualizado',
          detail: `El reporte #${actualizado.id} pasó a estado ${ESTADO_CONFIG[actualizado.estado].label}.`,
          life: 4000,
        });
      },
      error: () => {
        this.cambiandoEstadoId.set(null);
        this.messageService.add({
          severity: 'error',
          summary: 'Error al actualizar',
          detail: 'No se pudo cambiar el estado del reporte. Intentá de nuevo.',
          life: 5000,
        });
      },
    });
  }

  private cargar(): void {
    this.loading.set(true);
    const rango = this.rangoFechas();

    this.adminService
      .listarReportes({
        categoria: this.filtroCategoria() ?? undefined,
        estado: this.filtroEstado() ?? undefined,
        fechaInicio: rango?.[0] ? this.toIsoDate(rango[0]) : undefined,
        fechaFin: rango?.[1] ? this.toIsoDate(rango[1]) : undefined,
        page: this.rows() > 0 ? Math.floor(this.first() / this.rows()) : 0,
        size: this.rows(),
      })
      .subscribe({
        next: (pagina) => {
          this.reportes.set(pagina.content);
          this.totalRecords.set(pagina.totalElements);
          this.loading.set(false);
        },
        error: () => {
          this.loading.set(false);
          this.messageService.add({
            severity: 'error',
            summary: 'Error al cargar',
            detail: 'No se pudieron cargar los reportes. Verificá tu conexión.',
            life: 5000,
          });
        },
      });
  }

  private toIsoDate(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }
}
