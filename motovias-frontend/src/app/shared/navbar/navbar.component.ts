import {
  ChangeDetectionStrategy,
  Component,
  computed,
  DestroyRef,
  inject,
  signal,
  viewChild,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { MessageService, PrimeTemplate } from 'primeng/api';
import { Badge } from 'primeng/badge';
import { ButtonModule } from 'primeng/button';
import { Dialog } from 'primeng/dialog';
import { Popover } from 'primeng/popover';
import { Toast } from 'primeng/toast';
import { AuthService } from '../../core/services/auth.service';
import { NotificacionService } from '../../core/services/notificacion.service';
import { PuntoInteresService } from '../../core/services/punto-interes.service';
import { ReporteService, ReporteUpdatePayload } from '../../core/services/reporte.service';
import { NotificacionResponseDTO } from '../../core/models/notificacion.model';

interface NavItem {
  label: string;
  icon: string;
  route: string;
  ariaLabel: string;
}

@Component({
  selector: 'app-navbar',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [MessageService],
  imports: [RouterLink, RouterLinkActive, Badge, ButtonModule, Dialog, Popover, PrimeTemplate, Toast],
  templateUrl: './navbar.component.html',
  styleUrl: './navbar.component.css',
})
export class NavbarComponent {
  private readonly authService = inject(AuthService);
  private readonly notificacionService = inject(NotificacionService);
  private readonly puntoInteresService = inject(PuntoInteresService);
  private readonly reporteService = inject(ReporteService);
  private readonly messageService = inject(MessageService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  private readonly notifPanel = viewChild<Popover>('notifPanel');

  readonly currentUser = this.authService.currentUser;
  readonly notificaciones = this.notificacionService.notificaciones;
  readonly noLeidas = computed(() => this.notificaciones().length);

  readonly dialogNotif = signal<NotificacionResponseDTO | null>(null);
  readonly resolviendo = signal(false);

  readonly navItems: NavItem[] = [
    { label: 'Mapa',         icon: 'pi pi-map',  route: '/map',          ariaLabel: 'Ir al mapa' },
    { label: 'Mis Publicaciones', icon: 'pi pi-list', route: '/mis-reportes', ariaLabel: 'Ver mis publicaciones' },
    { label: 'Perfil',       icon: 'pi pi-user', route: '/perfil',       ariaLabel: 'Ver mi perfil' },
  ];

  constructor() {
    this.notificacionService.wsNotificacion$
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(notif => {
        if (notif.tipo === 'RECORDATORIO_CIERRE' && !this.dialogNotif()) {
          this.dialogNotif.set(notif);
        }
      });
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/auth/login']);
  }

  toggleNotificaciones(event: Event): void {
    this.notifPanel()?.toggle(event);
  }

  marcarLeida(id: number): void {
    this.notificacionService.marcarComoLeida(id).subscribe({
      next: () => this.notificacionService.marcarLeidaLocal(id),
    });
  }

  resolverReporte(notif: NotificacionResponseDTO): void {
    if (!notif.reporteId || this.resolviendo()) return;
    this.resolviendo.set(true);

    this.puntoInteresService.getById(notif.reporteId).subscribe({
      next: (reporte) => {
        const payload: ReporteUpdatePayload = {
          descripcion: reporte.descripcion,
          estado: 'RESUELTO',
        };
        this.reporteService.updateReporte(notif.reporteId!, payload).subscribe({
          next: () => {
            this.notificacionService.marcarComoLeida(notif.id).subscribe({
              next: () => {
                this.notificacionService.marcarLeidaLocal(notif.id);
                this.dialogNotif.set(null);
                this.resolviendo.set(false);
                this.messageService.add({
                  severity: 'success',
                  summary: 'Publicación cerrada',
                  detail: 'El mapa de la comunidad se actualizó.',
                  life: 4000,
                });
              },
            });
          },
          error: () => {
            this.resolviendo.set(false);
            this.messageService.add({
              severity: 'error',
              summary: 'Error',
              detail: 'No se pudo actualizar la publicación. Intentá de nuevo.',
              life: 4000,
            });
          },
        });
      },
      error: () => {
        this.resolviendo.set(false);
        this.messageService.add({
          severity: 'error',
          summary: 'Error',
          detail: 'No se pudo cargar la publicación. Intentá de nuevo.',
          life: 4000,
        });
      },
    });
  }

  descartarDialog(notif: NotificacionResponseDTO): void {
    this.notificacionService.marcarComoLeida(notif.id).subscribe({
      next: () => {
        this.notificacionService.marcarLeidaLocal(notif.id);
        this.dialogNotif.set(null);
      },
    });
  }
}
