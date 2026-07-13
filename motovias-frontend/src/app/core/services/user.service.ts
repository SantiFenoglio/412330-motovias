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

export interface AporteMensual {
  anio: number;
  mes: number;
  cantidad: number;
}

export interface DashboardResponse {
  reportesActivos: number;
  votosRecibidos: number;
  caravanasParticipando: number;
  aportesPorMes: AporteMensual[];
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

  deleteAccount(): Observable<void> {
    return this.http.delete<void>(`${BASE_URL}/api/usuarios/mi-cuenta`);
  }

  getMiDashboard(): Observable<DashboardResponse> {
    return this.http.get<DashboardResponse>(`${BASE_URL}/api/usuarios/mi-dashboard`);
  }
}
