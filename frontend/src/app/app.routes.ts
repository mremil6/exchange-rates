import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', redirectTo: 'calculator', pathMatch: 'full' },
  {
    path: 'calculator',
    title: 'Calculator · Marcura',
    loadComponent: () =>
      import('./features/calculator/calculator.component').then((m) => m.CalculatorComponent)
  },
  {
    path: 'historical',
    title: 'Historical · Marcura',
    loadComponent: () =>
      import('./features/historical/historical.component').then((m) => m.HistoricalComponent)
  },
  {
    path: 'analytics',
    title: 'Analytics · Marcura',
    loadComponent: () =>
      import('./features/analytics/analytics.component').then((m) => m.AnalyticsComponent)
  },
  {
    path: 'admin',
    title: 'Admin · Marcura',
    loadComponent: () =>
      import('./features/admin/admin.component').then((m) => m.AdminComponent)
  },
  { path: '**', redirectTo: 'calculator' }
];
