import { inject, Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  nombre: string;
  apellido?: string;
  tipoMotocicleta?: string;
  email: string;
  password: string;
}

export interface AuthResponse {
  token: string;
}

export interface UserInfo {
  email: string;
  nombre: string;
  roles: string[];
}

const TOKEN_KEY = 'auth_token';
// esto va directo a docker asi hardcodeado, no es un secreto ni nada, es solo para que funcione en docker sin tener que configurar variables de entorno
const BASE_URL = 'http://localhost:8080';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);

  readonly currentUser = signal<UserInfo | null>(this.decodeUserFromStorage());

  login(credentials: LoginRequest): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${BASE_URL}/api/auth/login`, credentials)
      .pipe(tap(({ token }) => this.saveToken(token)));
  }

  register(data: RegisterRequest): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${BASE_URL}/api/auth/register`, data)
      .pipe(tap(({ token }) => this.saveToken(token)));
  }

  saveToken(token: string): void {
    localStorage.setItem(TOKEN_KEY, token);
    this.currentUser.set(this.decodeUser(token));
  }

  getToken(): string | null {
    return localStorage.getItem(TOKEN_KEY);
  }

  logout(): void {
    localStorage.removeItem(TOKEN_KEY);
    this.currentUser.set(null);
  }

  isLoggedIn(): boolean {
    return !!this.getToken();
  }

  private decodeUserFromStorage(): UserInfo | null {
    const token = localStorage.getItem(TOKEN_KEY);
    return token ? this.decodeUser(token) : null;
  }

  private decodeUser(token: string): UserInfo | null {
    try {
      const raw = token.split('.')[1];
      const padding = raw.length % 4;
      const padded = padding ? raw + '='.repeat(4 - padding) : raw;
      const payload = JSON.parse(atob(padded.replace(/-/g, '+').replace(/_/g, '/')));
      return {
        email: payload['sub'] ?? payload['email'] ?? '',
        nombre: payload['nombre'] ?? payload['name'] ?? payload['sub'] ?? '',
        roles: payload['roles'] ?? payload['authorities'] ?? [],
      };
    } catch {
      return null;
    }
  }
}