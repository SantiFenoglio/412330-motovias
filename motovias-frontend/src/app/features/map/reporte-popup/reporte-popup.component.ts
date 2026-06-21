import {
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  ElementRef,
  inject,
  input,
  output,
  viewChild,
} from '@angular/core';
import { DOCUMENT, DatePipe } from '@angular/common';
import { Tooltip } from 'primeng/tooltip';
import { Router } from '@angular/router';
import {
  CATEGORY_CONFIG,
  ESTADO_CONFIG,
  PuntoInteres,
} from '../../../core/models/punto-interes.model';

@Component({
  selector: 'app-reporte-popup',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DatePipe, Tooltip],
  templateUrl: './reporte-popup.component.html',
  styleUrl: './reporte-popup.component.css',
})
export class ReportePopupComponent {
  readonly punto = input<PuntoInteres | null>(null);
  readonly esOwner = input(false);
  readonly esAdmin = input(false);
  readonly puedeVotar = input(false);
  readonly closed = output<void>();
  readonly editClicked = output<void>();
  readonly deleteClicked = output<void>();
  readonly votoClick = output<'CONFIRMA' | 'REFUTA'>();

  private readonly document = inject(DOCUMENT);
  private readonly router = inject(Router);
  private readonly closeBtnRef = viewChild<ElementRef<HTMLButtonElement>>('closeBtn');

  protected readonly catConfig = computed(() => {
    const p = this.punto();
    return p ? CATEGORY_CONFIG[p.categoria] : null;
  });

  protected readonly estadoConfig = computed(() => {
    const p = this.punto();
    return p?.estado ? ESTADO_CONFIG[p.estado] : null;
  });

  protected readonly puedeEditar = computed(() => this.esOwner() || this.esAdmin());

  constructor() {
    effect((onCleanup) => {
      const punto = this.punto();
      if (!punto) return;

      Promise.resolve().then(() => this.closeBtnRef()?.nativeElement.focus());

      const handler = (e: KeyboardEvent) => {
        if (e.key === 'Escape') this.closed.emit();
      };
      this.document.addEventListener('keydown', handler);
      onCleanup(() => this.document.removeEventListener('keydown', handler));
    });
  }

  close(): void {
    this.closed.emit();
  }

  onBackdropClick(event: MouseEvent): void {
    if ((event.target as HTMLElement).classList.contains('popup-backdrop')) {
      this.close();
    }
  }

  verDetalle(): void {
    const p = this.punto();
    if (p) this.router.navigate(['/reporte', p.id]);
  }

  onEdit(): void {
    this.editClicked.emit();
  }

  onDelete(): void {
    this.deleteClicked.emit();
  }

  onVotar(tipo: 'CONFIRMA' | 'REFUTA'): void {
    this.votoClick.emit(tipo);
  }
}
