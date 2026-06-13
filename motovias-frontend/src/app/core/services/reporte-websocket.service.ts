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

  private readonly upsertSubject = new Subject<PuntoInteres>();
  private readonly deleteSubject = new Subject<number>();

  readonly reporteUpserted$: Observable<PuntoInteres> = this.upsertSubject.asObservable();
  readonly reporteEliminado$: Observable<number> = this.deleteSubject.asObservable();

  constructor() {
    this.client = new Client({
      webSocketFactory: () => new SockJS(WS_URL),
      reconnectDelay: 5000,
      onConnect: () => {
        this.client.subscribe('/topic/reportes', (message) => {
          try {
            const reporte = JSON.parse(message.body) as PuntoInteres;
            this.zone.run(() => this.upsertSubject.next(reporte));
          } catch {
            // ignore malformed messages
          }
        });

        this.client.subscribe('/topic/reportes/eliminar', (message) => {
          const id = parseInt(message.body, 10);
          if (!isNaN(id)) {
            this.zone.run(() => this.deleteSubject.next(id));
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
