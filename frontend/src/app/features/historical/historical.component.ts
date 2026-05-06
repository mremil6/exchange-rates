import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { BaseChartDirective } from 'ng2-charts';
import { ChartConfiguration, ChartData } from 'chart.js';

import { ApiService } from '../../core/services/api.service';
import { CURRENCIES } from '../../core/currencies';
import { HistoricalRatesResponse, InsightResponse } from '../../core/models/exchange.models';
import { ViewState, data, error, idle, loading } from '../../shared/view-state';

/**
 * Historical view: line chart + table + AI insight panel.
 *
 * <p>The historical and insight requests are independent — the chart shouldn't
 * wait on the LLM. Insight has its own {@link ViewState} so its loading state
 * is reflected separately in the UI.
 */
@Component({
  selector: 'app-historical',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, BaseChartDirective],
  templateUrl: './historical.component.html',
  styleUrl: './historical.component.scss'
})
export class HistoricalComponent {
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(ApiService);

  readonly currencies = CURRENCIES;

  readonly historicalView = signal<ViewState<HistoricalRatesResponse>>(idle());
  readonly insightView = signal<ViewState<InsightResponse>>(idle());

  readonly form = this.fb.nonNullable.group({
    from: ['EUR', [Validators.required]],
    to:   ['USD', [Validators.required]],
    fromDate: [this.daysAgo(14), [Validators.required]],
    toDate:   [this.today(),    [Validators.required]]
  });

  readonly chartData = computed<ChartData<'line', number[], string>>(() => {
    const v = this.historicalView();
    if (v.kind !== 'data' || v.value.points.length === 0) {
      return { labels: [], datasets: [] };
    }
    return {
      labels: v.value.points.map((p) => p.date),
      datasets: [{
        label: `${v.value.from}/${v.value.to}`,
        data: v.value.points.map((p) => p.rate),
        borderColor: '#2c5cdc',
        backgroundColor: 'rgba(44, 92, 220, 0.10)',
        fill: true,
        tension: 0.25,
        pointRadius: 2
      }]
    };
  });

  readonly chartOptions: ChartConfiguration<'line'>['options'] = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: { legend: { display: true } },
    scales: {
      y: { beginAtZero: false, ticks: { precision: 4 } }
    }
  };

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { from, to, fromDate, toDate } = this.form.getRawValue();
    if (from === to) {
      this.historicalView.set(error({ status: 400, title: 'Invalid pair', detail: 'Pick two different currencies.' }));
      return;
    }
    if (toDate < fromDate) {
      this.historicalView.set(error({ status: 400, title: 'Invalid range', detail: 'End date must be on or after start date.' }));
      return;
    }

    this.historicalView.set(loading());
    this.api.historical(from, to, fromDate, toDate).subscribe({
      next: (res) => this.historicalView.set(data(res)),
      error: (problem) => this.historicalView.set(error(problem))
    });

    this.insightView.set(loading());
    this.api.insight(from, to, fromDate, toDate).subscribe({
      next: (res) => this.insightView.set(data(res)),
      error: (problem) => this.insightView.set(error(problem))
    });
  }

  private today(): string {
    return new Date().toISOString().slice(0, 10);
  }

  private daysAgo(n: number): string {
    const d = new Date();
    d.setDate(d.getDate() - n);
    return d.toISOString().slice(0, 10);
  }
}
