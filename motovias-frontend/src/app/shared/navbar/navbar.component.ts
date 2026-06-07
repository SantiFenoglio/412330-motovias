import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

interface NavItem {
  label: string;
  icon: string;
  route: string;
  ariaLabel: string;
}

@Component({
  selector: 'app-navbar',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './navbar.component.html',
  styleUrl: './navbar.component.css',
})
export class NavbarComponent {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  readonly currentUser = this.authService.currentUser;

  readonly navItems: NavItem[] = [
    { label: 'Mapa',        icon: 'pi pi-map',  route: '/map',          ariaLabel: 'Ir al mapa' },
    { label: 'Mis Reportes',icon: 'pi pi-list', route: '/mis-reportes', ariaLabel: 'Ver mis reportes' },
    { label: 'Perfil',      icon: 'pi pi-user', route: '/perfil',       ariaLabel: 'Ver mi perfil' },
  ];

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/auth/login']);
  }
}
