import { HttpInterceptorFn } from '@angular/common/http';

const TOKEN_KEY = 'auth_token';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const token = localStorage.getItem(TOKEN_KEY);

  // No añadir el token a los endpoints de auth (login/register):
  // evita que JwtAuthenticationFilter llame loadUserByUsername innecesariamente
  // y previene conflictos con sesiones anteriores guardadas en localStorage.
  if (!token || req.url.includes('/api/auth/')) {
    return next(req);
  }

  return next(
    req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }),
  );
};
