import { ChangeDetectionStrategy, Component, input, signal } from '@angular/core';

@Component({
  selector: 'app-image-gallery',
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './image-gallery.component.html',
  styleUrl: './image-gallery.component.css',
})
export class ImageGalleryComponent {
  readonly fotos = input<string[]>([]);

  protected readonly indiceActivo = signal<number | null>(null);

  abrir(index: number): void {
    this.indiceActivo.set(index);
  }

  cerrar(): void {
    this.indiceActivo.set(null);
  }

  anterior(): void {
    const total = this.fotos().length;
    this.indiceActivo.update((actual) => (actual === null ? null : (actual - 1 + total) % total));
  }

  siguiente(): void {
    const total = this.fotos().length;
    this.indiceActivo.update((actual) => (actual === null ? null : (actual + 1) % total));
  }

  onBackdropClick(event: MouseEvent): void {
    if ((event.target as HTMLElement).classList.contains('gallery-lightbox-backdrop')) {
      this.cerrar();
    }
  }
}
