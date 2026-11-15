import { DestroyRef, Injectable, computed, inject } from '@angular/core';
import { ClaimsApiClient, QueryState, isAwaitingClaimant, refreshWhileVisible } from '@claims/shared';

/** State for the claimant's claim list. Provided by the page, so it lives exactly as long as the page. */
@Injectable()
export class MyClaimsStore {
  private readonly api = inject(ClaimsApiClient);
  private readonly query = new QueryState(() => this.api.listClaims({ size: 100 }));

  readonly state = this.query.state;
  readonly claims = computed(() => this.query.data()?.items ?? []);
  readonly awaitingMe = computed(() => this.claims().filter((claim) => isAwaitingClaimant(claim.status)).length);

  constructor() {
    this.query.load();
    refreshWhileVisible(() => this.query.load({ background: true }));
    inject(DestroyRef).onDestroy(() => this.query.cancel());
  }

  reload(): void {
    this.query.load();
  }
}
