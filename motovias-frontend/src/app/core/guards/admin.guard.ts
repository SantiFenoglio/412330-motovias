import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

/** Protege rutas exclusivas del rol ADMIN. Redirige al mapa principal si el rol no coincide. */
export const adminGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.currentUser()?.role === 'ADMIN') {
    return true;
  }

  return router.createUrlTree(['/map']);
};
