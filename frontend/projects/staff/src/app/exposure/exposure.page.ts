import { CurrencyPipe, DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { LINE_OF_BUSINESS_LABELS, PageStateComponent } from '@claims/shared';
import { ExposureStore } from './exposure.store';

@Component({
  selector: 'staff-exposure-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CurrencyPipe, DatePipe, PageStateComponent],
  providers: [ExposureStore],
  template: `
    <section class="cc-page">
      <header class="cc-page-header">
        <h1>Outstanding exposure</h1>
        @if (store.loadedAt(); as loadedAt) {
          <p class="cc-muted">Refreshed {{ loadedAt | date: 'mediumTime' }}</p>
        }
      </header>
      <p class="cc-muted intro">
        What the company may still have to pay on open claims: the reserve on every claim that is not yet rejected or
        settled. Amounts are shown per currency and never converted. Figures are updated from claim events and can
        lag a decision by a few seconds.
      </p>

      <cc-page-state
        [state]="store.state()"
        [empty]="store.isEmpty()"
        emptyMessage="There are no open claims, so there is no outstanding exposure."
        loadingMessage="Loading exposure…"
        (retry)="store.reload()"
      >
        <ul class="totals" aria-label="Totals by currency">
          @for (total of store.totals(); track total.currency) {
            <li class="cc-card total">
              <p class="total__label">{{ total.currency }}</p>
              <p class="total__amount">{{ total.outstandingAmount | currency: total.currency : 'code' }}</p>
              <p class="cc-muted">{{ total.openClaims }} open {{ total.openClaims === 1 ? 'claim' : 'claims' }}</p>
            </li>
          }
        </ul>

        <div class="cc-table-wrapper">
          <table class="cc-table">
            <caption>Breakdown by country and line of business</caption>
            <thead>
              <tr>
                <th scope="col">Country</th>
                <th scope="col">Line of business</th>
                <th scope="col" class="cc-numeric">Open claims</th>
                <th scope="col" class="cc-numeric">Outstanding</th>
                <th scope="col">Last change</th>
              </tr>
            </thead>
            <tbody>
              @for (line of store.breakdown(); track line.countryCode + line.lineOfBusiness + line.currency) {
                <tr>
                  <th scope="row">{{ line.countryCode }}</th>
                  <td>{{ lineLabels[line.lineOfBusiness] }}</td>
                  <td class="cc-numeric">{{ line.openClaims }}</td>
                  <td class="cc-numeric">{{ line.outstandingAmount | currency: line.currency : 'code' }}</td>
                  <td>{{ line.updatedAt | date: 'medium' }}</td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      </cc-page-state>
    </section>
  `,
  styles: `
    .intro { max-width: 48rem; margin-top: calc(-1 * var(--cc-space-4)); }
    .totals {
      list-style: none;
      margin: 0 0 var(--cc-space-6);
      padding: 0;
      display: grid;
      gap: var(--cc-space-4);
      grid-template-columns: repeat(auto-fill, minmax(14rem, 1fr));
    }
    .total { margin: 0; }
    .total p { margin: 0; }
    .total__label { font-weight: 600; color: var(--cc-color-text-muted); }
    .total__amount { font-size: 1.5rem; font-weight: 700; font-variant-numeric: tabular-nums; }
  `,
})
export class ExposurePage {
  protected readonly store = inject(ExposureStore);
  protected readonly lineLabels = LINE_OF_BUSINESS_LABELS;
}
