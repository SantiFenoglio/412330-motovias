import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Checkbox } from 'primeng/checkbox';
import { PuntoInteresService } from '../../../core/services/punto-interes.service';
import {
  Categoria,
  CATEGORY_CONFIG,
  TODAS_LAS_CATEGORIAS,
} from '../../../core/models/punto-interes.model';

@Component({
  selector: 'app-filter-panel',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Checkbox, FormsModule],
  templateUrl: './filter-panel.component.html',
  styleUrl: './filter-panel.component.css',
})
export class FilterPanelComponent {
  private readonly service = inject(PuntoInteresService);

  readonly isPanelOpen = signal(false);
  readonly todasLasCategorias: [Categoria, (typeof CATEGORY_CONFIG)[Categoria]][] =
    Object.entries(CATEGORY_CONFIG) as [Categoria, (typeof CATEGORY_CONFIG)[Categoria]][];

  togglePanel(): void {
    this.isPanelOpen.update((v) => !v);
  }

  esCategoriaActiva(cat: Categoria): boolean {
    return this.service.esCategoriaActiva(cat);
  }

  toggleCategoria(cat: Categoria): void {
    this.service.toggleCategoria(cat);
  }

  get activasCount(): number {
    return this.service.categoriasActivas().length;
  }

  get totalCount(): number {
    return TODAS_LAS_CATEGORIAS.length;
  }
}
