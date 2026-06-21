import { inject, Injectable, NgZone, OnDestroy, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, Subject } from 'rxjs';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { AuthService } from './auth.service';
import { NotificacionResponseDTO } from '../models/notificacion.model';

const BASE_URL = 'http://localhost:8080';
const WS_URL = `${BASE_URL}/ws`;

@Injectable({ providedIn: 'root' })
export class NotificacionService implements OnDestroy {
  private readonly http = inject(HttpClient);
  private readonly authService = inject(AuthService);
  private readonly zone = inject(NgZone);

  private readonly client: Client;
  private readonly _wsSubject = new Subject<NotificacionResponseDTO>();

  /** Notificaciones no leídas en tiempo real. */
  readonly notificaciones = signal<NotificacionResponseDTO[]>([]);

  /** Emite sólo las notificaciones que llegan vía WebSocket (no el carga inicial HTTP). */
  readonly wsNotificacion$ = this._wsSubject.asObservable();

  constructor() {
    this.cargarInicial();

    this.client = new Client({
      webSocketFactory: () => new SockJS(WS_URL),
      reconnectDelay: 5000,
      onConnect: () => {
        const email = this.authService.currentUser()?.email;
        if (!email) return;

        this.client.subscribe(
          `/topic/usuarios/${email}/notificaciones`,
          (message) => {
            try {
              const notif = JSON.parse(message.body) as NotificacionResponseDTO;
              this.zone.run(() => {
                this.notificaciones.update(lista => {
                  if (lista.some(n => n.id === notif.id)) return lista;
                  return [notif, ...lista];
                });
                this._wsSubject.next(notif);
              });
            } catch {
              // ignore malformed messages
            }
          },
        );
      },
    });

    this.client.activate();
  }

  getNotificaciones(): Observable<NotificacionResponseDTO[]> {
    return this.http.get<NotificacionResponseDTO[]>(`${BASE_URL}/api/notificaciones`);
  }

  marcarComoLeida(id: number): Observable<NotificacionResponseDTO> {
    return this.http.post<NotificacionResponseDTO>(
      `${BASE_URL}/api/notificaciones/${id}/leer`,
      {},
    );
  }

  marcarLeidaLocal(id: number): void {
    this.notificaciones.update(lista => lista.filter(n => n.id !== id));
  }

  private cargarInicial(): void {
    this.getNotificaciones().subscribe({
      next: lista => this.notificaciones.set(lista),
      error: () => { /* silent: can fail if token not yet present */ },
    });
  }

  ngOnDestroy(): void {
    this.client.deactivate();
    this._wsSubject.complete();
  }
}
