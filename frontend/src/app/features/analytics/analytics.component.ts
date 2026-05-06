import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { BaseChartDirective } from 'ng2-charts';
import { ChartConfiguration, ChartData } from 'chart.js';

import { ApiService } from '../../core/services/api.service';
import { AnalyticsResponse } from '../../core/models/exchange.models';
import { ViewState, data, error, loading } from '../../shared/view-state';

@Component({
  selector: 'app-analytics',
  standalone: true,
  imports: [CommonModule, BaseChartDirective],
  templateUrl: './analytics.component.html',
  styleUrl: './analytics.component.scss'
})
export class AnalyticsComponent implements OnInit {
  private readonly api = inject(ApiService);

  readonly view = signal<ViewState<AnalyticsResponse>>(loading());

  readonly topBarData = computed<ChartData<'bar', number[], string>>(() => {
    const v = this.view();
    if (v.kind !== 'data') return { labels: [], datasets: [] };
    const top = v.value.topCurrencies.slice(0, 10);
    return {
      labels: top.map((c) => c.currency),
      datasets: [{
        label: 'Total queries',
        data: top.map((c) => c.totalCount),
        backgroundColor: '#2c5cdc'
      }]
    };
  });

  readonly topBarOptions: ChartConfiguration<'bar'>['options'] = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: { legend: { display: false } },
    scales: { y: { beginAtZero: true, ticks: { precision: 0 } } }
  };

  ngOnInit(): void {
    this.refresh();
  }

  refresh(): void {
    this.view.set(loading());
    this.api.analytics().subscribe({
      next: (res) => this.view.set(data(res)),
      error: (problem) => this.view.set(error(problem))
    });
  }
}
