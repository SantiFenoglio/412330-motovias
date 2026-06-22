import { inject, Injectable, NgZone, OnDestroy, signal } from '@angular/core';
import { Observable, Subject } from 'rxjs';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { AuthService } from './auth.service';
import { ParticipanteUbicacionDTO } from '../models/participante-ubicacion.model';

const WS_URL = 'http://localhost:8080/ws';

@Injectable({ providedIn: 'root' })
export class LocationSharingService implements OnDestroy {
  private readonly zone = inject(NgZone);
  private readonly authService = inject(AuthService);

  private client: Client | null = null;
  private watchId: number | null = null;
  private stompSubscription: { unsubscribe(): void } | null = null;

  readonly modoViajeActivo = signal(false);
  readonly viajeIdActivo = signal<number | null>(null);

  private readonly participanteSubject = new Subject<ParticipanteUbicacionDTO>();
  readonly participanteUbicacion$: Observable<ParticipanteUbicacionDTO> =
    this.participanteSubject.asObservable();

  iniciarCompartir(viajeId: number): void {
    if (this.modoViajeActivo()) {
      this.detenerCompartir(this.viajeIdActivo()!);
    }

    this.viajeIdActivo.set(viajeId);
    this.modoViajeActivo.set(true);

    this.client = new Client({
      webSocketFactory: () => new SockJS(WS_URL),
      reconnectDelay: 5000,
      onConnect: () => {
        this.stompSubscription = this.client!.subscribe(
          `/topic/viajes/${viajeId}/ubicaciones`,
          (message) => {
            try {
              const dto = JSON.parse(message.body) as ParticipanteUbicacionDTO;
              this.zone.run(() => this.participanteSubject.next(dto));
            } catch {
              // ignore malformed messages
            }
          },
        );

        this.watchId = navigator.geolocation.watchPosition(
          (pos) =>
            this.publicarUbicacion(viajeId, pos.coords.latitude, pos.coords.longitude),
          () => {},
          { enableHighAccuracy: true, maximumAge: 15000, timeout: 10000 },
        );
      },
    });

    this.client.activate();
  }

  detenerCompartir(viajeId: number): void {
    const user = this.authService.currentUser();
    if (user && this.client?.connected) {
      this.client.publish({
        destination: `/app/viajes/${viajeId}/compartir`,
        body: JSON.stringify({
          email: user.email,
          nombre: user.nombre,
          latitud: null,
          longitud: null,
          conectado: false,
        } as ParticipanteUbicacionDTO),
      });
    }

    if (this.watchId !== null) {
      navigator.geolocation.clearWatch(this.watchId);
      this.watchId = null;
    }

    this.stompSubscription?.unsubscribe();
    this.stompSubscription = null;

    this.client?.deactivate();
    this.client = null;

    this.modoViajeActivo.set(false);
    this.viajeIdActivo.set(null);
  }

  private publicarUbicacion(viajeId: number, latitud: number, longitud: number): void {
    if (!this.client?.connected) return;
    const user = this.authService.currentUser();
    if (!user) return;

    this.client.publish({
      destination: `/app/viajes/${viajeId}/compartir`,
      body: JSON.stringify({
        email: user.email,
        nombre: user.nombre,
        latitud,
        longitud,
        conectado: true,
      } as ParticipanteUbicacionDTO),
    });
  }

  ngOnDestroy(): void {
    const viajeId = this.viajeIdActivo();
    if (viajeId !== null) {
      this.detenerCompartir(viajeId);
    }
  }
}
