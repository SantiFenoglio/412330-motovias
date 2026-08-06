import { inject, Injectable, NgZone, OnDestroy } from '@angular/core';
import { Observable, Subject } from 'rxjs';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { ComercioVerificado } from '../models/comercio-verificado.model';

const WS_URL = 'http://localhost:8080/ws';

@Injectable({ providedIn: 'root' })
export class ComercioWebSocketService implements OnDestroy {
  private readonly zone = inject(NgZone);
  private readonly client: Client;

  private readonly upsertSubject = new Subject<ComercioVerificado>();

  readonly comercioUpserted$: Observable<ComercioVerificado> = this.upsertSubject.asObservable();

  constructor() {
    this.client = new Client({
      webSocketFactory: () => new SockJS(WS_URL),
      reconnectDelay: 5000,
      onConnect: () => {
        this.client.subscribe('/topic/comercios', (message) => {
          try {
            const comercio = JSON.parse(message.body) as ComercioVerificado;
            this.zone.run(() => this.upsertSubject.next(comercio));
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
