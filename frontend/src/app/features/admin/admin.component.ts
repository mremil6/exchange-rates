import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';

import { ApiService } from '../../core/services/api.service';
import { RefreshResponse } from '../../core/models/exchange.models';
import { ViewState, data, error, idle, loading } from '../../shared/view-state';

@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './admin.component.html',
  styleUrl: './admin.component.scss'
})
export class AdminComponent {
  private readonly api = inject(ApiService);

  readonly view = signal<ViewState<RefreshResponse>>(idle());

  triggerRefresh(): void {
    this.view.set(loading());
    this.api.refresh().subscribe({
      next: (res) => this.view.set(data(res)),
      error: (problem) => this.view.set(error(problem))
    });
  }
}
