import { MoneyPipe } from './money.pipe';

describe('MoneyPipe', () => {
  it('shows the currency code, separated from the amount', () => {
    expect(new MoneyPipe().transform(4200, 'SGD')).toMatch(/^SGD\s4,200\.00$/);
  });

  it('shows nothing for a missing amount', () => {
    expect(new MoneyPipe().transform(null, 'AUD')).toBe('');
  });
});
