import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';

import { AdminComponent } from './admin.component';
import { ApiService } from '../../core/services/api.service';

describe('AdminComponent', () => {
  let component: AdminComponent;
  let mockApi: jasmine.SpyObj<ApiService>;

  beforeEach(async () => {
    mockApi = jasmine.createSpyObj<ApiService>('ApiService', ['refresh']);
    await TestBed.configureTestingModule({
      imports: [AdminComponent],
      providers: [{ provide: ApiService, useValue: mockApi }]
    }).compileComponents();
    component = TestBed.createComponent(AdminComponent).componentInstance;
  });

  it('starts in idle state', () => {
    expect(component.view().kind).toBe('idle');
  });

  it('sets data state with rowsWritten on success', () => {
    mockApi.refresh.and.returnValue(of({ rowsWritten: 42 }));
    component.triggerRefresh();
    const v = component.view();
    expect(v.kind).toBe('data');
    if (v.kind === 'data') expect(v.value.rowsWritten).toBe(42);
  });

  it('sets error state on API failure', () => {
    mockApi.refresh.and.returnValue(throwError(() => ({ status: 500, title: 'Server Error' })));
    component.triggerRefresh();
    expect(component.view().kind).toBe('error');
  });
});
