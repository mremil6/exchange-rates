import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { ApiService } from '../../core/services/api.service';
import { CURRENCIES } from '../../core/currencies';
import { ExchangeResponse } from '../../core/models/exchange.models';
import { ViewState, data, error, idle, loading } from '../../shared/view-state';

/**
 * Calculator: select two currencies, optionally pick a date, hit the backend.
 * Result panel renders the spread-adjusted rate plus the live usage counters.
 */
@Component({
  selector: 'app-calculator',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './calculator.component.html',
  styleUrl: './calculator.component.scss'
})
export class CalculatorComponent {
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(ApiService);

  readonly currencies = CURRENCIES;
  readonly view = signal<ViewState<ExchangeResponse>>(idle());

  readonly form = this.fb.nonNullable.group({
    from: ['EUR', [Validators.required]],
    to:   ['PLN', [Validators.required]],
    date: ['']      // optional ISO date
  });

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { from, to, date } = this.form.getRawValue();
    if (from === to) {
      this.view.set(error({ status: 400, title: 'Invalid pair', detail: 'Pick two different currencies.' }));
      return;
    }
    this.view.set(loading());
    this.api.exchange(from, to, date || undefined).subscribe({
      next: (res) => this.view.set(data(res)),
      error: (problem) => this.view.set(error(problem))
    });
  }
}
