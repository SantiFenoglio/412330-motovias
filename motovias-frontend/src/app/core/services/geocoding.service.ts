import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpContext, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { SKIP_AUTH } from '../interceptors/auth.interceptor';

export interface NominatimResult {
  lat: string;
  lon: string;
  display_name: string;
}

const NOMINATIM_HEADERS = new HttpHeaders({
  'User-Agent': 'MotoviasUTNTesis/1.0 (santifeno2004@gmail.com)',
  'Accept-Language': 'es',
});

@Injectable({ providedIn: 'root' })
export class GeocodingService {
  private readonly http = inject(HttpClient);

  search(query: string): Observable<NominatimResult[]> {
    const url = `https://nominatim.openstreetmap.org/search?q=${encodeURIComponent(query)}&format=json&limit=5&countrycodes=ar`;
    return this.http.get<NominatimResult[]>(url, {
      headers: NOMINATIM_HEADERS,
      context: new HttpContext().set(SKIP_AUTH, true),
    });
  }
}
