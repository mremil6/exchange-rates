import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

import { ApiService } from './api.service';
import { environment } from '../../../environments/environment';

describe('ApiService', () => {
  let service: ApiService;
  let http: HttpTestingController;
  const base = environment.apiBaseUrl;

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [HttpClientTestingModule] });
    service = TestBed.inject(ApiService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('exchange() sends from/to/date params', () => {
    const mock = { from: 'EUR', to: 'PLN', exchange: 4.5, date: '2024-01-01', fromQueryCount: 1, toQueryCount: 2 };
    service.exchange('EUR', 'PLN', '2024-01-01').subscribe(res => expect(res).toEqual(mock));
    const req = http.expectOne(r => r.url === `${base}/exchange`);
    expect(req.request.params.get('from')).toBe('EUR');
    expect(req.request.params.get('to')).toBe('PLN');
    expect(req.request.params.get('date')).toBe('2024-01-01');
    req.flush(mock);
  });

  it('exchange() omits date param when not provided', () => {
    service.exchange('EUR', 'USD').subscribe();
    const req = http.expectOne(r => r.url === `${base}/exchange`);
    expect(req.request.params.has('date')).toBeFalse();
    req.flush({});
  });

  it('historical() sends from/to/fromDate/toDate params', () => {
    service.historical('EUR', 'USD', '2024-01-01', '2024-01-14').subscribe();
    const req = http.expectOne(r => r.url === `${base}/historical`);
    expect(req.request.params.get('from')).toBe('EUR');
    expect(req.request.params.get('to')).toBe('USD');
    expect(req.request.params.get('fromDate')).toBe('2024-01-01');
    expect(req.request.params.get('toDate')).toBe('2024-01-14');
    req.flush({ from: 'EUR', to: 'USD', fromDate: '2024-01-01', toDate: '2024-01-14', points: [] });
  });

  it('insight() calls GET /exchange/insight', () => {
    service.insight('EUR', 'USD', '2024-01-01', '2024-01-14').subscribe();
    const req = http.expectOne(r => r.url === `${base}/exchange/insight`);
    expect(req.request.method).toBe('GET');
    req.flush({ from: 'EUR', to: 'USD', fromDate: '2024-01-01', toDate: '2024-01-14', insight: 'test' });
  });

  it('analytics() calls GET /analytics', () => {
    service.analytics().subscribe();
    const req = http.expectOne(`${base}/analytics`);
    expect(req.request.method).toBe('GET');
    req.flush({ topCurrencies: [] });
  });

  it('refresh() calls POST /admin/refresh', () => {
    service.refresh().subscribe();
    const req = http.expectOne(`${base}/admin/refresh`);
    expect(req.request.method).toBe('POST');
    req.flush({ rowsWritten: 5 });
  });

  it('normalises structured backend error to ProblemDetail shape', () => {
    let captured: unknown;
    service.exchange('EUR', 'PLN').subscribe({ error: e => (captured = e) });
    http.expectOne(r => r.url.includes('/exchange')).flush(
      { title: 'Not Found', detail: 'No rate for date', status: 404 },
      { status: 404, statusText: 'Not Found' }
    );
    expect((captured as { status: number }).status).toBe(404);
    expect((captured as { title: string }).title).toBe('Not Found');
  });

  it('normalises network error to ProblemDetail with status 0', () => {
    let captured: unknown;
    service.exchange('EUR', 'PLN').subscribe({ error: e => (captured = e) });
    http.expectOne(r => r.url.includes('/exchange')).error(new ProgressEvent('network'));
    expect((captured as { status: number }).status).toBe(0);
  });
});
