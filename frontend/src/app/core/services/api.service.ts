import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, throwError } from 'rxjs';

import { environment } from '../../../environments/environment';
import {
  AnalyticsResponse,
  ExchangeResponse,
  HistoricalRatesResponse,
  InsightResponse,
  ProblemDetail,
  RefreshResponse
} from '../models/exchange.models';

/**
 * Single source of truth for backend access. Components depend on this service,
 * not on HttpClient directly, so the API surface is typed and centralised.
 *
 * <p>Errors are normalised to a {@link ProblemDetail} so view components can
 * display a single, consistent shape regardless of HTTP failure mode.
 */
@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = environment.apiBaseUrl;

  exchange(from: string, to: string, date?: string): Observable<ExchangeResponse> {
    let params = new HttpParams().set('from', from).set('to', to);
    if (date) params = params.set('date', date);
    return this.http
      .get<ExchangeResponse>(`${this.baseUrl}/exchange`, { params })
      .pipe(catchError(this.normaliseError));
  }

  historical(from: string, to: string, fromDate: string, toDate: string): Observable<HistoricalRatesResponse> {
    const params = new HttpParams()
      .set('from', from)
      .set('to', to)
      .set('fromDate', fromDate)
      .set('toDate', toDate);
    return this.http
      .get<HistoricalRatesResponse>(`${this.baseUrl}/historical`, { params })
      .pipe(catchError(this.normaliseError));
  }

  insight(from: string, to: string, fromDate: string, toDate: string): Observable<InsightResponse> {
    const params = new HttpParams()
      .set('from', from)
      .set('to', to)
      .set('fromDate', fromDate)
      .set('toDate', toDate);
    return this.http
      .get<InsightResponse>(`${this.baseUrl}/exchange/insight`, { params })
      .pipe(catchError(this.normaliseError));
  }

  refresh(): Observable<RefreshResponse> {
    return this.http
      .post<RefreshResponse>(`${this.baseUrl}/admin/refresh`, null)
      .pipe(catchError(this.normaliseError));
  }

  analytics(): Observable<AnalyticsResponse> {
    return this.http
      .get<AnalyticsResponse>(`${this.baseUrl}/analytics`)
      .pipe(catchError(this.normaliseError));
  }

  private normaliseError(err: HttpErrorResponse): Observable<never> {
    const problem: ProblemDetail = err.error && typeof err.error === 'object'
      ? { status: err.status, ...err.error }
      : { status: err.status, title: err.statusText || 'Network error', detail: err.message };
    return throwError(() => problem);
  }
}
