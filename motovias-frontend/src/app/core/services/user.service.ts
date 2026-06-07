import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export type TipoMotocicleta = 'CUSTOM' | 'ADVENTURE' | 'SPORT' | 'NAKED' | 'TOURING' | 'ENDURO';

export interface UserProfile {
  nombre: string;
  apellido: string | null;
  email: string;
  tipoMotocicleta: TipoMotocicleta | null;
  activo: boolean;
  role: string;
  tipoSangre: string | null;
  contactoEmergenciaNombre: string | null;
  contactoEmergenciaTelefono: string | null;
  direccion: string | null;
}

export interface UserProfileUpdate {
  nombre: string;
  apellido?: string | null;
  tipoMotocicleta: TipoMotocicleta | null;
  tipoSangre?: string | null;
  contactoEmergenciaNombre?: string | null;
  contactoEmergenciaTelefono?: string | null;
  direccion?: string | null;
  newPassword?: string | null;
}

const BASE_URL = 'http://localhost:8080';

@Injectable({ providedIn: 'root' })
export class UserService {
  private readonly http = inject(HttpClient);

  getProfile(): Observable<UserProfile> {
    return this.http.get<UserProfile>(`${BASE_URL}/api/users/me`);
  }

  updateProfile(data: UserProfileUpdate): Observable<UserProfile> {
    return this.http.put<UserProfile>(`${BASE_URL}/api/users/me`, data);
  }
}
