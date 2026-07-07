import { ChangeDetectionStrategy, Component, model } from '@angular/core';
import { Dialog } from 'primeng/dialog';
import { ButtonModule } from 'primeng/button';
import { PrimeTemplate } from 'primeng/api';

interface SeccionLegal {
  titulo: string;
  parrafos: string[];
}

@Component({
  selector: 'app-terminos-dialog',
  imports: [Dialog, ButtonModule, PrimeTemplate],
  templateUrl: './terminos-dialog.component.html',
  styleUrl: './terminos-dialog.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TerminosDialogComponent {
  readonly visible = model<boolean>(false);

  readonly secciones: SeccionLegal[] = [
    {
      titulo: '1. Consentimiento informado',
      parrafos: [
        'Al registrarte en Motovías declarás haber leído y comprendido el presente contrato, y aceptás voluntariamente sus términos como condición necesaria para acceder a la plataforma.',
        'La aceptación se registra junto con tu cuenta y rige durante todo el tiempo en que la utilices. Si en algún momento no estás de acuerdo con estos términos, debés discontinuar el uso del servicio.',
      ],
    },
    {
      titulo: '2. Uso responsable',
      parrafos: [
        'La plataforma está destinada a la publicación y consulta de información vial georreferenciada (incidentes, puntos de interés, alertas) de forma colaborativa entre motoviajeros.',
        'Te comprometés a no publicar información falsa, ofensiva o que pueda inducir a error o poner en riesgo a otros usuarios de la vía pública. Motovías puede moderar o eliminar contenido que incumpla esta condición.',
      ],
    },
    {
      titulo: '3. Seguridad de la cuenta',
      parrafos: [
        'Sos responsable de mantener la confidencialidad de tus credenciales de acceso y de toda actividad realizada bajo tu cuenta.',
        'Las contraseñas se almacenan de forma cifrada y la autenticación se realiza mediante tokens de sesión con expiración. Ante cualquier sospecha de acceso no autorizado, debés notificarlo y actualizar tu contraseña de inmediato.',
      ],
    },
    {
      titulo: '4. Propiedad intelectual',
      parrafos: [
        'El software, el diseño de la interfaz y la marca Motovías son propiedad de sus desarrolladores. El contenido generado por los usuarios (reportes, ubicaciones, descripciones) permanece asociado a su autor, quien otorga a Motovías una licencia no exclusiva para mostrarlo dentro de la plataforma.',
        'Queda prohibida la reproducción o explotación comercial del software sin autorización expresa.',
      ],
    },
  ];

  cerrar(): void {
    this.visible.set(false);
  }
}
