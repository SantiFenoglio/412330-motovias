import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-perfil',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [],
  template: `
    <div class="page-shell">
      <header class="page-header">
        <h1 class="page-title">
          <i class="pi pi-user" aria-hidden="true"></i>
          Mi Perfil
        </h1>
      </header>

      @if (currentUser()) {
        <section class="profile-card" aria-label="Información de la cuenta">
          <div class="avatar" aria-hidden="true">
            {{ initials() }}
          </div>

          <div class="profile-info">
            <p class="profile-name">{{ currentUser()!.nombre }}</p>
            <p class="profile-email">{{ currentUser()!.email }}</p>
          </div>
        </section>
      }

      <div class="actions">
        <button
          class="btn-logout"
          type="button"
          (click)="logout()"
          aria-label="Cerrar sesión de la cuenta"
        >
          <i class="pi pi-sign-out" aria-hidden="true"></i>
          Cerrar sesión
        </button>
      </div>
    </div>
  `,
  styles: [`
    .page-shell {
      max-width: 28rem;
      margin: 0 auto;
      padding: 2rem 1.25rem;
    }

    .page-header { margin-bottom: 2rem; }

    .page-title {
      display: flex;
      align-items: center;
      gap: 0.625rem;
      font-size: 1.5rem;
      font-weight: 700;
      color: #0f172a;
      margin: 0;
    }

    .page-title .pi { color: #3b82f6; font-size: 1.375rem; }

    .profile-card {
      display: flex;
      align-items: center;
      gap: 1.25rem;
      padding: 1.5rem;
      background: #f8fafc;
      border: 1px solid #e2e8f0;
      border-radius: 12px;
      margin-bottom: 2rem;
    }

    .avatar {
      width: 3.5rem;
      height: 3.5rem;
      border-radius: 50%;
      background: #1e293b;
      color: #f8fafc;
      font-size: 1.25rem;
      font-weight: 700;
      display: flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;
      letter-spacing: 0.05em;
    }

    .profile-info { min-width: 0; }

    .profile-name {
      font-size: 1.0625rem;
      font-weight: 600;
      color: #0f172a;
      margin: 0 0 0.25rem;
      overflow: hidden;
      white-space: nowrap;
      text-overflow: ellipsis;
    }

    .profile-email {
      font-size: 0.875rem;
      color: #64748b;
      margin: 0;
      overflow: hidden;
      white-space: nowrap;
      text-overflow: ellipsis;
    }

    .actions { display: flex; }

    .btn-logout {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      padding: 0.625rem 1.25rem;
      min-height: 2.75rem;
      background: transparent;
      border: 1.5px solid #e2e8f0;
      border-radius: 8px;
      color: #dc2626;
      font-size: 0.9375rem;
      font-family: inherit;
      font-weight: 500;
      cursor: pointer;
      transition: background 0.15s, border-color 0.15s;
    }

    .btn-logout:hover {
      background: #fef2f2;
      border-color: #fca5a5;
    }

    .btn-logout:focus-visible {
      outline: 2px solid #dc2626;
      outline-offset: 2px;
    }
  `],
})
export class PerfilComponent {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  readonly currentUser = this.authService.currentUser;

  initials(): string {
    const user = this.currentUser();
    if (!user) return '?';
    const src = user.nombre || user.email;
    return src
      .split(/[\s@.]+/)
      .slice(0, 2)
      .map((w) => w[0]?.toUpperCase() ?? '')
      .join('');
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/auth/login']);
  }
}
