/**
 * Currencies offered in dropdowns. Matches the synthetic seed dataset and
 * Fixer.io's free-tier coverage so all selections always have a corresponding
 * rate row in the local DB.
 */
export const CURRENCIES: ReadonlyArray<string> = [
  'EUR', 'USD', 'GBP', 'PLN', 'CHF',
  'JPY', 'HKD', 'KRW', 'MYR', 'INR',
  'MXN', 'RUB', 'CNY', 'ZAR', 'AUD',
  'CAD', 'SEK', 'NOK'
] as const;
