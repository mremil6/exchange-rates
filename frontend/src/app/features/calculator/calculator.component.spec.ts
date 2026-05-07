import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';

import { CalculatorComponent } from './calculator.component';
import { ApiService } from '../../core/services/api.service';
import { ExchangeResponse } from '../../core/models/exchange.models';

const mockExchange: ExchangeResponse = {
  from: 'EUR', to: 'PLN', exchange: 4.5, date: '2024-01-01',
  fromQueryCount: 1, toQueryCount: 2
};

describe('CalculatorComponent', () => {
  let component: CalculatorComponent;
  let mockApi: jasmine.SpyObj<ApiService>;

  beforeEach(async () => {
    mockApi = jasmine.createSpyObj<ApiService>('ApiService', ['exchange']);
    await TestBed.configureTestingModule({
      imports: [CalculatorComponent],
      providers: [{ provide: ApiService, useValue: mockApi }]
    }).compileComponents();
    component = TestBed.createComponent(CalculatorComponent).componentInstance;
  });

  it('starts in idle state', () => {
    expect(component.view().kind).toBe('idle');
  });

  it('initialises form with EUR/PLN defaults', () => {
    expect(component.form.getRawValue()).toEqual({ from: 'EUR', to: 'PLN', date: '' });
  });

  it('does not call API when form is invalid', () => {
    component.form.controls.from.setValue('');
    component.submit();
    expect(mockApi.exchange).not.toHaveBeenCalled();
    expect(component.view().kind).toBe('idle');
  });

  it('sets error state when from equals to', () => {
    component.form.setValue({ from: 'EUR', to: 'EUR', date: '' });
    component.submit();
    const v = component.view();
    expect(v.kind).toBe('error');
    if (v.kind === 'error') expect(v.problem.status).toBe(400);
  });

  it('sets data state on successful API response', () => {
    mockApi.exchange.and.returnValue(of(mockExchange));
    component.form.setValue({ from: 'EUR', to: 'PLN', date: '' });
    component.submit();
    const v = component.view();
    expect(v.kind).toBe('data');
    if (v.kind === 'data') expect(v.value.exchange).toBe(4.5);
  });

  it('passes date param when provided', () => {
    mockApi.exchange.and.returnValue(of(mockExchange));
    component.form.setValue({ from: 'EUR', to: 'PLN', date: '2024-01-01' });
    component.submit();
    expect(mockApi.exchange).toHaveBeenCalledWith('EUR', 'PLN', '2024-01-01');
  });

  it('omits date param when field is empty', () => {
    mockApi.exchange.and.returnValue(of(mockExchange));
    component.form.setValue({ from: 'EUR', to: 'PLN', date: '' });
    component.submit();
    expect(mockApi.exchange).toHaveBeenCalledWith('EUR', 'PLN', undefined);
  });

  it('sets error state on API failure', () => {
    const problem = { status: 404, title: 'Not Found', detail: 'No rate for date' };
    mockApi.exchange.and.returnValue(throwError(() => problem));
    component.form.setValue({ from: 'EUR', to: 'PLN', date: '' });
    component.submit();
    expect(component.view().kind).toBe('error');
  });
});
