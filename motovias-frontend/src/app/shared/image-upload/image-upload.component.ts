import {
  ChangeDetectionStrategy,
  Component,
  computed,
  input,
  output,
  signal,
} from '@angular/core';

const MAX_TAMANIO_BYTES = 5 * 1024 * 1024;
const TIPOS_PERMITIDOS = ['image/jpeg', 'image/png'];

interface FotoPreview {
  file: File;
  previewUrl: string;
}

@Component({
  selector: 'app-image-upload',
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './image-upload.component.html',
  styleUrl: './image-upload.component.css',
})
export class ImageUploadComponent {
  readonly maxFotos = input(3);
  readonly filesChanged = output<File[]>();

  protected readonly previews = signal<FotoPreview[]>([]);
  protected readonly error = signal<string | null>(null);

  protected readonly puedeAgregarMas = computed(() => this.previews().length < this.maxFotos());

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const archivos = input.files;
    if (!archivos || archivos.length === 0) return;

    this.error.set(null);

    const espacioDisponible = this.maxFotos() - this.previews().length;
    const candidatos = Array.from(archivos);

    if (candidatos.length > espacioDisponible) {
      this.error.set(`Solo podés agregar hasta ${this.maxFotos()} fotos en total.`);
    }

    const seleccionados = candidatos.slice(0, espacioDisponible);

    for (const archivo of seleccionados) {
      if (!TIPOS_PERMITIDOS.includes(archivo.type)) {
        this.error.set('Solo se permiten imágenes JPEG o PNG.');
        continue;
      }
      if (archivo.size > MAX_TAMANIO_BYTES) {
        this.error.set('Cada foto debe pesar como máximo 5MB.');
        continue;
      }

      const lector = new FileReader();
      lector.onload = () => {
        this.previews.update((lista) => [
          ...lista,
          { file: archivo, previewUrl: lector.result as string },
        ]);
        this.emitirCambio();
      };
      lector.readAsDataURL(archivo);
    }

    input.value = '';
  }

  eliminar(index: number): void {
    this.previews.update((lista) => lista.filter((_, i) => i !== index));
    this.emitirCambio();
  }

  private emitirCambio(): void {
    this.filesChanged.emit(this.previews().map((p) => p.file));
  }
}
