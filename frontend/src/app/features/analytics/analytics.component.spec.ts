import { NO_ERRORS_SCHEMA } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { CommonModule } from '@angular/common';
import { of, throwError } from 'rxjs';

import { AnalyticsComponent } from './analytics.component';
import { ApiService } from '../../core/services/api.service';
import { AnalyticsResponse } from '../../core/models/exchange.models';

const mockAnalytics: AnalyticsResponse = {
  topCurrencies: [
    { currency: 'EUR', totalCount: 100, lastQueriedAt: '2024-01-01T00:00:00Z' },
    { currency: 'USD', totalCount: 80,  lastQueriedAt: '2024-01-01T00:00:00Z' },
    { currency: 'GBP', totalCount: 60,  lastQueriedAt: null }
  ]
};

describe('AnalyticsComponent', () => {
  let mockApi: jasmine.SpyObj<ApiService>;

  function createComponent(): AnalyticsComponent {
    return TestBed.createComponent(AnalyticsComponent).componentInstance;
  }

  beforeEach(async () => {
    mockApi = jasmine.createSpyObj<ApiService>('ApiService', ['analytics']);
    mockApi.analytics.and.returnValue(of(mockAnalytics));
    await TestBed.configureTestingModule({
      imports: [AnalyticsComponent],
      providers: [{ provide: ApiService, useValue: mockApi }],
      schemas: [NO_ERRORS_SCHEMA]
    })
    .overrideComponent(AnalyticsComponent, {
      set: { imports: [CommonModule], schemas: [NO_ERRORS_SCHEMA] }
    })
    .compileComponents();
  });

  it('starts in loading state', () => {
    const component = createComponent();
    expect(component.view().kind).toBe('loading');
  });

  it('topBarData returns empty datasets before data loads', () => {
    const component = createComponent();
    expect(component.topBarData().labels).toEqual([]);
    expect(component.topBarData().datasets).toEqual([]);
  });

  it('loads analytics on ngOnInit and transitions to data', () => {
    const fixture = TestBed.createComponent(AnalyticsComponent);
    fixture.detectChanges(); // triggers ngOnInit → refresh()
    expect(fixture.componentInstance.view().kind).toBe('data');
  });

  it('topBarData maps currencies and counts from response', () => {
    const fixture = TestBed.createComponent(AnalyticsComponent);
    fixture.detectChanges();
    const bd = fixture.componentInstance.topBarData();
    expect(bd.labels).toEqual(['EUR', 'USD', 'GBP']);
    expect(bd.datasets[0]!.data).toEqual([100, 80, 60]);
  });

  it('topBarData slices to top 10 even when more are returned', () => {
    const many: AnalyticsResponse = {
      topCurrencies: Array.from({ length: 15 }, (_, i) => ({
        currency: `C${i}`, totalCount: 100 - i, lastQueriedAt: null
      }))
    };
    mockApi.analytics.and.returnValue(of(many));
    const fixture = TestBed.createComponent(AnalyticsComponent);
    fixture.detectChanges();
    expect(fixture.componentInstance.topBarData().labels!.length).toBe(10);
  });

  it('sets error state when API call fails', () => {
    mockApi.analytics.and.returnValue(throwError(() => ({ status: 500, title: 'Server Error' })));
    const fixture = TestBed.createComponent(AnalyticsComponent);
    fixture.detectChanges();
    expect(fixture.componentInstance.view().kind).toBe('error');
  });

  it('refresh() sets loading then updates to data', () => {
    const fixture = TestBed.createComponent(AnalyticsComponent);
    fixture.detectChanges(); // ngOnInit
    const component = fixture.componentInstance;
    mockApi.analytics.and.returnValue(of(mockAnalytics));
    component.refresh();
    expect(component.view().kind).toBe('data');
    expect(mockApi.analytics).toHaveBeenCalledTimes(2);
  });
});
