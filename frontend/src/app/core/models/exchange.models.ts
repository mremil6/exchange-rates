/* Wire types — kept in sync with backend DTOs. */

export interface ExchangeResponse {
  from: string;
  to: string;
  exchange: number;
  date: string;            // ISO yyyy-MM-dd
  fromQueryCount: number;
  toQueryCount: number;
}

export interface HistoricalPoint {
  date: string;            // ISO yyyy-MM-dd
  rate: number;
}

export interface HistoricalRatesResponse {
  from: string;
  to: string;
  fromDate: string;
  toDate: string;
  points: HistoricalPoint[];
}

export interface InsightResponse {
  from: string;
  to: string;
  fromDate: string;
  toDate: string;
  insight: string;
}

export interface TopCurrency {
  currency: string;
  totalCount: number;
  lastQueriedAt: string | null;
}

export interface AnalyticsResponse {
  topCurrencies: TopCurrency[];
}

export interface RefreshResponse {
  rowsWritten: number;
}

/** RFC 7807 Problem Details — what the backend returns on error. */
export interface ProblemDetail {
  type?: string;
  title?: string;
  status: number;
  detail?: string;
}
