import { Injectable } from '@angular/core';

export interface UserCoords {
  lat: number;
  lng: number;
}

const GEO_OPTIONS: PositionOptions = {
  timeout: 10_000,
  maximumAge: 0,
  enableHighAccuracy: false,
};

@Injectable({ providedIn: 'root' })
export class GeolocationService {
  getUserPosition(): Promise<UserCoords> {
    if (!('geolocation' in navigator)) {
      return Promise.reject(new Error('Geolocation no disponible en este navegador'));
    }
    return new Promise<UserCoords>((resolve, reject) => {
      navigator.geolocation.getCurrentPosition(
        ({ coords }) => resolve({ lat: coords.latitude, lng: coords.longitude }),
        reject,
        GEO_OPTIONS,
      );
    });
  }
}
