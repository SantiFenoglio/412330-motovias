import { inject, Injectable, NgZone, OnDestroy } from '@angular/core';
import { Observable, Subject } from 'rxjs';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { PuntoInteres } from '../models/punto-interes.model';

const WS_URL = 'http://localhost:8080/ws';

@Injectable({ providedIn: 'root' })
export class ReporteWebSocketService implements OnDestroy {
  private readonly zone = inject(NgZone);
  private readonly client: Client;
  private readonly subject = new Subject<PuntoInteres>();

  readonly nuevosReportes$: Observable<PuntoInteres> = this.subject.asObservable();

  constructor() {
    this.client = new Client({
      webSocketFactory: () => new SockJS(WS_URL),
      reconnectDelay: 5000,
      onConnect: () => {
        this.client.subscribe('/topic/reportes', (message) => {
          try {
            const reporte = JSON.parse(message.body) as PuntoInteres;
            this.zone.run(() => this.subject.next(reporte));
          } catch {
            // ignore malformed messages
          }
        });
      },
    });

    this.client.activate();
  }

  ngOnDestroy(): void {
    this.client.deactivate();
  }
}
