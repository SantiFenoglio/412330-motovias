import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { PuntoInteresService } from './punto-interes.service';
import { PuntoInteres, TODAS_LAS_CATEGORIAS } from '../models/punto-interes.model';

const BASE_URL = 'http://localhost:8080/api/puntos-interes';

const MOCK_PUNTOS: PuntoInteres[] = [
  { id: 1, titulo: 'Accidente Ruta 9', descripcion: '', latitud: -31.4, longitud: -64.1, categoria: 'ACCIDENTE' },
  { id: 2, titulo: 'Obra Vial Norte',  descripcion: '', latitud: -31.5, longitud: -64.2, categoria: 'OBRA' },
  { id: 3, titulo: 'Control Km 15',   descripcion: '', latitud: -31.6, longitud: -64.3, categoria: 'TRAMPA_POLICIAL' },
];

describe('PuntoInteresService – filtrado reactivo', () => {
  let service: PuntoInteresService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        PuntoInteresService,
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });

    service = TestBed.inject(PuntoInteresService);
    httpMock = TestBed.inject(HttpTestingController);

    // Resuelve la petición inicial que dispara toSignal
    httpMock.expectOne(BASE_URL).flush(MOCK_PUNTOS);
    TestBed.flushEffects();
  });

  afterEach(() => httpMock.verify());

  it('inicia con todas las categorías activas', () => {
    expect(service.categoriasActivas()).toEqual(TODAS_LAS_CATEGORIAS);
  });

  it('expone todos los puntos cuando todas las categorías están activas', () => {
    expect(service.puntosFiltrados().length).toBe(3);
  });

  it('filtra puntos al desactivar una categoría', () => {
    service.toggleCategoria('ACCIDENTE');

    const filtrados = service.puntosFiltrados();
    expect(filtrados.length).toBe(2);
    expect(filtrados.every((p) => p.categoria !== 'ACCIDENTE')).toBe(true);
  });

  it('reactiva una categoría al hacer toggle dos veces', () => {
    service.toggleCategoria('ACCIDENTE');
    service.toggleCategoria('ACCIDENTE');

    expect(service.puntosFiltrados().length).toBe(3);
  });

  it('esCategoriaActiva refleja el estado del signal correctamente', () => {
    expect(service.esCategoriaActiva('OBRA')).toBe(true);
    service.toggleCategoria('OBRA');
    expect(service.esCategoriaActiva('OBRA')).toBe(false);
  });

  it('retorna lista vacía cuando todas las categorías están desactivadas', () => {
    TODAS_LAS_CATEGORIAS.forEach((cat) => service.toggleCategoria(cat));
    expect(service.puntosFiltrados().length).toBe(0);
  });

  it('filtra correctamente con múltiples categorías desactivadas', () => {
    service.toggleCategoria('ACCIDENTE');
    service.toggleCategoria('TRAMPA_POLICIAL');

    const filtrados = service.puntosFiltrados();
    expect(filtrados.length).toBe(1);
    expect(filtrados[0].categoria).toBe('OBRA');
  });

  it('activa una categoría que fue desactivada previamente', () => {
    service.toggleCategoria('SEMAFORO_ROTO');
    expect(service.esCategoriaActiva('SEMAFORO_ROTO')).toBe(false);

    service.toggleCategoria('SEMAFORO_ROTO');
    expect(service.esCategoriaActiva('SEMAFORO_ROTO')).toBe(true);
    expect(service.categoriasActivas().length).toBe(TODAS_LAS_CATEGORIAS.length);
  });
});
