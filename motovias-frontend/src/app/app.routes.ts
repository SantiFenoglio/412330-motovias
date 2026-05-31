import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  {
    path: 'map',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/map/map.component').then((m) => m.MapComponent),
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
  { path: '', redirectTo: '/map', pathMatch: 'full' },
  { path: '**', redirectTo: '/auth/login' },
];
