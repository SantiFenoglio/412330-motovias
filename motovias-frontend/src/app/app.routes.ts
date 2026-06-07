import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  {
    path: '',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./shared/layout/layout.component').then((m) => m.LayoutComponent),
    children: [
      {
        path: 'map',
        loadComponent: () =>
          import('./features/map/map.component').then((m) => m.MapComponent),
      },
      {
        path: 'mis-reportes',
        loadComponent: () =>
          import('./features/mis-reportes/mis-reportes.component').then(
            (m) => m.MisReportesComponent,
          ),
      },
      {
        path: 'perfil',
        loadComponent: () =>
          import('./features/perfil/perfil.component').then(
            (m) => m.PerfilComponent,
          ),
      },
      {
        path: 'reporte/:id',
        loadComponent: () =>
          import('./features/reporte/reporte-detail/reporte-detail.component').then(
            (m) => m.ReporteDetailComponent,
          ),
      },
      { path: '', redirectTo: 'map', pathMatch: 'full' },
    ],
  },
  {
    path: 'auth',
    children: [
      {
        path: 'login',
        loadComponent: () =>
          import('./features/auth/login/login.component').then(
            (m) => m.LoginComponent,
          ),
      },
      {
        path: 'register',
        loadComponent: () =>
          import('./features/auth/register/register.component').then(
            (m) => m.RegisterComponent,
          ),
      },
      { path: '', redirectTo: 'login', pathMatch: 'full' },
    ],
  },
  { path: '**', redirectTo: '/auth/login' },
];
