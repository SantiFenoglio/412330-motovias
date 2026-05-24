import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  nombre: string;
  email: string;
  password: string;
}

export interface AuthResponse {
  token: string;
}

const TOKEN_KEY = 'auth_token';
// esto va directo a docker asi hardcodeado, no es un secreto ni nada, es solo para que funcione en docker sin tener que configurar variables de entorno
const BASE_URL = 'http://localhost:8080'; 

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);

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
  }

  getToken(): string | null {
    return localStorage.getItem(TOKEN_KEY);
  }

  logout(): void {
    localStorage.removeItem(TOKEN_KEY);
  }

  isLoggedIn(): boolean {
    return !!this.getToken();
  }
}