import { DestroyRef, Injectable, computed, inject } from '@angular/core';
import { ClaimsApiClient, QueryState, refreshWhileVisible } from '@claims/shared';

@Injectable()
export class WorkloadStore {
  private readonly api = inject(ClaimsApiClient);
  private readonly query = new QueryState(() => this.api.getWorkload());

  readonly state = this.query.state;
  readonly loadedAt = this.query.loadedAt;
  readonly officers = computed(() => this.query.data()?.officers ?? []);
  readonly unassigned = computed(() => this.query.data()?.unassignedCount ?? 0);
  readonly totals = computed(() =>
    this.officers().reduce(
      (sum, officer) => ({
        underReview: sum.underReview + officer.underReview,
        awaitingInformation: sum.awaitingInformation + officer.awaitingInformation,
        awaitingSettlement: sum.awaitingSettlement + officer.awaitingSettlement,
        closed: sum.closed + officer.closed,
      }),
      { underReview: 0, awaitingInformation: 0, awaitingSettlement: 0, closed: 0 },
    ),
  );

  constructor() {
    this.query.load();
    refreshWhileVisible(() => this.query.load({ background: true }));
    inject(DestroyRef).onDestroy(() => this.query.cancel());
  }

  reload(): void {
    this.query.load();
  }
}
