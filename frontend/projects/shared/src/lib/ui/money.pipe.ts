import { Pipe, PipeTransform } from '@angular/core';

/**
 * Formats an amount with its ISO currency code ("SGD 4,200.00"). The code, not a symbol, is used because the
 * platform handles six markets and several of them use "$".
 */
@Pipe({ name: 'money' })
export class MoneyPipe implements PipeTransform {
  transform(amount: number | null | undefined, currency: string): string {
    if (amount == null) return '';
    return new Intl.NumberFormat('en', {
      style: 'currency',
      currency,
      currencyDisplay: 'code',
    }).format(amount);
  }
}
