import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  ElementRef,
  afterRenderEffect,
  inject,
  signal,
  viewChild,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { delay, of } from 'rxjs';

type AutorMensaje = 'usuario' | 'asistente';

interface MensajeChat {
  id: number;
  autor: AutorMensaje;
  texto: string;
  fecha: Date;
}

const RESPUESTA_LATENCIA_MS = 1000;

const MENSAJE_BIENVENIDA =
  'Hola, soy el asistente virtual de Motovías. Estoy disponible las 24 horas para ayudarte con caravanas, el mapa de reportes o la gestión de gastos y pagos. ¿En qué puedo ayudarte?';

@Component({
  selector: 'app-asistente-virtual',
  imports: [ReactiveFormsModule, ButtonModule, InputTextModule],
  templateUrl: './asistente-virtual.component.html',
  styleUrl: './asistente-virtual.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AsistenteVirtualComponent {
  private readonly destroyRef = inject(DestroyRef);
  private readonly historial = viewChild<ElementRef<HTMLDivElement>>('historial');

  private nextId = 1;

  readonly abierto = signal(false);
  readonly enviando = signal(false);
  readonly mensajes = signal<MensajeChat[]>([
    { id: this.nextId++, autor: 'asistente', texto: MENSAJE_BIENVENIDA, fecha: new Date() },
  ]);

  readonly mensajeControl = new FormControl('', {
    nonNullable: true,
    validators: [Validators.required],
  });

  constructor() {
    afterRenderEffect(() => {
      this.mensajes();
      this.enviando();
      const el = this.historial()?.nativeElement;
      if (el) {
        el.scrollTop = el.scrollHeight;
      }
    });
  }

  toggle(): void {
    this.abierto.update((v) => !v);
  }

  onSubmit(event: Event): void {
    event.preventDefault();
    this.enviarMensaje();
  }

  private enviarMensaje(): void {
    const texto = this.mensajeControl.value.trim();
    if (!texto || this.enviando()) return;

    this.mensajes.update((msgs) => [
      ...msgs,
      { id: this.nextId++, autor: 'usuario', texto, fecha: new Date() },
    ]);
    this.mensajeControl.setValue('');
    this.enviando.set(true);

    of(null)
      .pipe(delay(RESPUESTA_LATENCIA_MS), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        const respuesta = this.generarRespuesta(texto);
        this.mensajes.update((msgs) => [
          ...msgs,
          { id: this.nextId++, autor: 'asistente', texto: respuesta, fecha: new Date() },
        ]);
        this.enviando.set(false);
      });
  }

  private generarRespuesta(mensaje: string): string {
    const texto = mensaje.toLowerCase();

    if (texto.includes('gasto') || texto.includes('mercado pago') || texto.includes('pago')) {
      return (
        'Para gestionar los gastos de tu caravana, ingresá a "Caravanas" > seleccioná el viaje > "Gastos". ' +
        'Ahí podés cargar un gasto indicando el pagador y el monto; el sistema calcula automáticamente el ' +
        'balance entre los participantes. Para saldar una deuda, usá el botón "Pagar con Mercado Pago" en el ' +
        'detalle del balance: te redirige a la pasarela de pago segura y, al confirmarse, el saldo se actualiza automáticamente.'
      );
    }

    if (texto.includes('caravana')) {
      return (
        'Para unirte a una caravana necesitás el código de invitación de 6 caracteres que te comparte el ' +
        'organizador. Ingresá a "Caravanas" y usá la opción "Unirme con código". Si sos vos quien organiza, ' +
        'podés crear una nueva caravana desde el mismo panel y compartir el código generado con el resto del grupo.'
      );
    }

    if (texto.includes('mapa') || texto.includes('reporte') || texto.includes('incidente')) {
      return (
        'El mapa muestra en tiempo real los reportes de la comunidad (accidentes, controles, obras, ' +
        'semáforos rotos, piquetes y peligros). Podés filtrar por categoría desde el panel de filtros y tocar ' +
        'un marcador para ver el detalle. Para publicar un nuevo reporte, usá el botón de reportar y seleccioná tu ubicación en el mapa.'
      );
    }

    if (
      texto.includes('cuenta') ||
      texto.includes('perfil') ||
      texto.includes('contraseña') ||
      texto.includes('eliminar')
    ) {
      return (
        'Desde "Perfil" podés editar tus datos personales y cambiar tu contraseña. En la sección "Zona ' +
        'crítica" también podés eliminar tu cuenta y todos tus datos personales de forma permanente, en ' +
        'ejercicio de tu derecho al olvido.'
      );
    }

    return (
      'Gracias por tu consulta. Soy un asistente automatizado disponible las 24 horas. Podés preguntarme ' +
      'sobre caravanas, el mapa de reportes o la gestión de gastos y pagos. Si tu consulta requiere atención ' +
      'personalizada, un miembro del equipo la revisará a la brevedad.'
    );
  }
}
