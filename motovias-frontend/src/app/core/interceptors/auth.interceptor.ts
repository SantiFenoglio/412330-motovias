import { HttpContextToken, HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { MessageService } from 'primeng/api';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';

export const SKIP_AUTH = new HttpContextToken<boolean>(() => false);

const TOKEN_KEY = 'auth_token';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const messageService = inject(MessageService);
  const token = localStorage.getItem(TOKEN_KEY);

  // No añadir el token a los endpoints de auth (login/register):
  // evita que JwtAuthenticationFilter llame loadUserByUsername innecesariamente
  // y previene conflictos con sesiones anteriores guardadas en localStorage.
  // SKIP_AUTH se usa para peticiones a APIs externas (ej: Nominatim).
  if (!token || req.url.includes('/api/auth/') || req.context.get(SKIP_AUTH)) {
    return next(req);
  }

  return next(
    req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }),
  ).pipe(
    catchError((error) => {
      if (error instanceof HttpErrorResponse && error.status === 401) {
        authService.logout();
        messageService.add({
          key: 'auth',
          severity: 'warn',
          summary: 'Sesión expirada',
          detail: 'Tu sesión ha expirado. Por favor, ingresá nuevamente.',
          life: 5000,
        });
        router.navigate(['/login']);
      }
      return throwError(() => error);
    }),
  );
};
