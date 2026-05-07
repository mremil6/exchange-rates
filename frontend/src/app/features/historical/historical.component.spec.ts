import { NO_ERRORS_SCHEMA } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule } from '@angular/forms';
import { of, throwError } from 'rxjs';

import { HistoricalComponent } from './historical.component';
import { ApiService } from '../../core/services/api.service';
import { HistoricalRatesResponse, InsightResponse } from '../../core/models/exchange.models';

const mockHistorical: HistoricalRatesResponse = {
  from: 'EUR', to: 'USD',
  fromDate: '2024-01-01', toDate: '2024-01-03',
  points: [
    { date: '2024-01-01', rate: 1.1 },
    { date: '2024-01-02', rate: 1.2 }
  ]
};

const mockInsight: InsightResponse = {
  from: 'EUR', to: 'USD',
  fromDate: '2024-01-01', toDate: '2024-01-03',
  insight: 'EUR/USD rose 8.3% between Jan 1 and Jan 2.'
};

describe('HistoricalComponent', () => {
  let component: HistoricalComponent;
  let mockApi: jasmine.SpyObj<ApiService>;

  beforeEach(async () => {
    mockApi = jasmine.createSpyObj<ApiService>('ApiService', ['historical', 'insight']);
    await TestBed.configureTestingModule({
      imports: [HistoricalComponent],
      providers: [{ provide: ApiService, useValue: mockApi }],
      schemas: [NO_ERRORS_SCHEMA]
    })
    .overrideComponent(HistoricalComponent, {
      set: { imports: [CommonModule, ReactiveFormsModule], schemas: [NO_ERRORS_SCHEMA] }
    })
    .compileComponents();
    component = TestBed.createComponent(HistoricalComponent).componentInstance;
  });

  it('starts with both views in idle state', () => {
    expect(component.historicalView().kind).toBe('idle');
    expect(component.insightView().kind).toBe('idle');
  });

  it('chartData returns empty datasets when state is idle', () => {
    expect(component.chartData().datasets).toEqual([]);
    expect(component.chartData().labels).toEqual([]);
  });

  it('sets error state when from equals to', () => {
    component.form.patchValue({ from: 'EUR', to: 'EUR' });
    component.submit();
    const v = component.historicalView();
    expect(v.kind).toBe('error');
    if (v.kind === 'error') expect(v.problem.status).toBe(400);
  });

  it('sets error state when toDate is before fromDate', () => {
    component.form.patchValue({ fromDate: '2024-01-10', toDate: '2024-01-01' });
    component.submit();
    expect(component.historicalView().kind).toBe('error');
  });

  it('calls both historical() and insight() on valid submit', () => {
    mockApi.historical.and.returnValue(of(mockHistorical));
    mockApi.insight.and.returnValue(of(mockInsight));
    component.form.patchValue({ from: 'EUR', to: 'USD', fromDate: '2024-01-01', toDate: '2024-01-03' });
    component.submit();
    expect(mockApi.historical).toHaveBeenCalledWith('EUR', 'USD', '2024-01-01', '2024-01-03');
    expect(mockApi.insight).toHaveBeenCalledWith('EUR', 'USD', '2024-01-01', '2024-01-03');
  });

  it('sets both views to data on success', () => {
    mockApi.historical.and.returnValue(of(mockHistorical));
    mockApi.insight.and.returnValue(of(mockInsight));
    component.form.patchValue({ from: 'EUR', to: 'USD', fromDate: '2024-01-01', toDate: '2024-01-03' });
    component.submit();
    expect(component.historicalView().kind).toBe('data');
    expect(component.insightView().kind).toBe('data');
  });

  it('chartData maps points to labels and dataset', () => {
    mockApi.historical.and.returnValue(of(mockHistorical));
    mockApi.insight.and.returnValue(of(mockInsight));
    component.form.patchValue({ from: 'EUR', to: 'USD', fromDate: '2024-01-01', toDate: '2024-01-03' });
    component.submit();
    const cd = component.chartData();
    expect(cd.labels).toEqual(['2024-01-01', '2024-01-02']);
    expect(cd.datasets[0]!.data).toEqual([1.1, 1.2]);
    expect(cd.datasets[0]!.label).toBe('EUR/USD');
  });

  it('insightView error does not affect historicalView', () => {
    mockApi.historical.and.returnValue(of(mockHistorical));
    mockApi.insight.and.returnValue(throwError(() => ({ status: 503, title: 'Unavailable' })));
    component.form.patchValue({ from: 'EUR', to: 'USD', fromDate: '2024-01-01', toDate: '2024-01-03' });
    component.submit();
    expect(component.historicalView().kind).toBe('data');
    expect(component.insightView().kind).toBe('error');
  });

  it('historicalView error does not affect insightView', () => {
    mockApi.historical.and.returnValue(throwError(() => ({ status: 500, title: 'Server Error' })));
    mockApi.insight.and.returnValue(of(mockInsight));
    component.form.patchValue({ from: 'EUR', to: 'USD', fromDate: '2024-01-01', toDate: '2024-01-03' });
    component.submit();
    expect(component.historicalView().kind).toBe('error');
    expect(component.insightView().kind).toBe('data');
  });
});
