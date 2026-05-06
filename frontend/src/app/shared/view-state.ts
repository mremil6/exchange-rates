import { ProblemDetail } from '../core/models/exchange.models';

/**
 * Standard view-model wrapper. Every feature component renders these three
 * states explicitly: idle/loading skeleton, error banner, or content.
 */
export type ViewState<T> =
  | { kind: 'idle' }
  | { kind: 'loading' }
  | { kind: 'data'; value: T }
  | { kind: 'error'; problem: ProblemDetail };

export const idle = (): ViewState<never> => ({ kind: 'idle' });
export const loading = (): ViewState<never> => ({ kind: 'loading' });
export const data = <T>(value: T): ViewState<T> => ({ kind: 'data', value });
export const error = (problem: ProblemDetail): ViewState<never> => ({ kind: 'error', problem });
