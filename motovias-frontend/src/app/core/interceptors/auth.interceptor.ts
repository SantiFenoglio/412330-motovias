import { HttpContextToken, HttpInterceptorFn } from '@angular/common/http';

export const SKIP_AUTH = new HttpContextToken<boolean>(() => false);

const TOKEN_KEY = 'auth_token';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
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
  );
};
