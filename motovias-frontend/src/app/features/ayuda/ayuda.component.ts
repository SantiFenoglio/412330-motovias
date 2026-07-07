import { ChangeDetectionStrategy, Component } from '@angular/core';
import { Accordion, AccordionContent, AccordionHeader, AccordionPanel } from 'primeng/accordion';
import { AsistenteVirtualComponent } from './asistente-virtual/asistente-virtual.component';

interface FaqItem {
  pregunta: string;
  respuesta: string;
}

interface FaqCategoria {
  nombre: string;
  icono: string;
  preguntas: FaqItem[];
}

@Component({
  selector: 'app-ayuda',
  imports: [Accordion, AccordionPanel, AccordionHeader, AccordionContent, AsistenteVirtualComponent],
  templateUrl: './ayuda.component.html',
  styleUrl: './ayuda.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AyudaComponent {
  readonly categorias: FaqCategoria[] = [
    {
      nombre: 'Caravanas',
      icono: 'pi pi-users',
      preguntas: [
        {
          pregunta: '¿Cómo me uno a una caravana?',
          respuesta:
            'Necesitás el código de invitación de 6 caracteres que te comparte el organizador. Ingresá a "Caravanas" y usá la opción "Unirme con código" para sumarte al grupo.',
        },
        {
          pregunta: '¿Cómo creo una caravana nueva?',
          respuesta:
            'Desde "Caravanas" tocá "Crear caravana", indicá un título y una descripción. El sistema genera automáticamente un código único que podés compartir con el resto de los motoviajeros.',
        },
        {
          pregunta: '¿Qué pasa si abandono o elimino una caravana?',
          respuesta:
            'Si abandonás, perdés el acceso a los gastos y a la ubicación compartida del grupo, pero la caravana sigue activa para el resto. Si sos el creador y la eliminás, se borran permanentemente todos los gastos, participantes y el historial asociado.',
        },
      ],
    },
    {
      nombre: 'Mapa y Reportes',
      icono: 'pi pi-map',
      preguntas: [
        {
          pregunta: '¿Cómo publico un reporte en el mapa?',
          respuesta:
            'Usá el botón de reportar en el mapa, seleccioná la ubicación y elegí una categoría (accidente, obra, control policial, semáforo roto, piquete o peligro). Tu reporte queda visible para toda la comunidad en tiempo real.',
        },
        {
          pregunta: '¿Puedo filtrar los reportes por categoría?',
          respuesta:
            'Sí. El panel de filtros del mapa permite mostrar u ocultar marcadores según su categoría, para que veas solo lo que te interesa.',
        },
        {
          pregunta: '¿Cómo marco un reporte como resuelto?',
          respuesta:
            'Si sos el autor del reporte, podés editarlo desde "Mis Publicaciones" y cambiar su estado a "Resuelto". También recibís un recordatorio automático si el reporte lleva más de dos horas activo.',
        },
      ],
    },
    {
      nombre: 'Gastos y Liquidaciones',
      icono: 'pi pi-wallet',
      preguntas: [
        {
          pregunta: '¿Cómo se reparten los gastos de una caravana?',
          respuesta:
            'Dentro del detalle de la caravana, en la sección "Gastos", cada participante puede cargar lo que pagó indicando el monto y la categoría. El sistema calcula automáticamente cuánto le debe cada integrante al resto del grupo.',
        },
        {
          pregunta: '¿Cómo pago lo que debo con Mercado Pago?',
          respuesta:
            'En el balance de gastos de tu caravana vas a ver el monto pendiente con cada participante. Tocá "Pagar con Mercado Pago" para ser redirigido a la pasarela de pago segura; una vez confirmado el pago, tu saldo se actualiza automáticamente.',
        },
        {
          pregunta: '¿Los pagos por Mercado Pago son seguros?',
          respuesta:
            'Sí, la integración utiliza el checkout oficial de Mercado Pago. Motovías no almacena datos de tarjetas ni medios de pago; solo recibe la confirmación del estado de la transacción.',
        },
      ],
    },
  ];
}
