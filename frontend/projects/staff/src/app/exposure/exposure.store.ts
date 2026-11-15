import { DestroyRef, Injectable, computed, inject } from '@angular/core';
import { ClaimsApiClient, QueryState, refreshWhileVisible } from '@claims/shared';

/**
 * Outstanding liability exposure. The figures come from a read model that the Kafka consumer maintains, so they
 * can lag a status change by a moment; the page says when they were last refreshed.
 */
@Injectable()
export class ExposureStore {
  private readonly api = inject(ClaimsApiClient);
  private readonly query = new QueryState(() => this.api.getExposure());

  readonly state = this.query.state;
  readonly loadedAt = this.query.loadedAt;
  readonly totals = computed(() => this.query.data()?.totals ?? []);
  readonly breakdown = computed(() =>
    [...(this.query.data()?.breakdown ?? [])].sort(
      (a, b) =>
        a.currency.localeCompare(b.currency) ||
        a.countryCode.localeCompare(b.countryCode) ||
        a.lineOfBusiness.localeCompare(b.lineOfBusiness),
    ),
  );
  readonly isEmpty = computed(() => this.totals().every((total) => total.openClaims === 0));

  constructor() {
    this.query.load();
    refreshWhileVisible(() => this.query.load({ background: true }));
    inject(DestroyRef).onDestroy(() => this.query.cancel());
  }

  reload(): void {
    this.query.load();
  }
}
