import { DestroyRef, Injectable, inject, signal } from '@angular/core';
import {
  ClaimDetail,
  ClaimsApiClient,
  CommandState,
  QueryState,
  refreshWhileVisible,
} from '@claims/shared';
import { EMPTY } from 'rxjs';

/** State for one claim as the claimant sees it, and the one thing the claimant can do to it: answer a question. */
@Injectable()
export class ClaimantClaimStore {
  private readonly api = inject(ClaimsApiClient);
  private readonly claimId = signal<string | null>(null);
  private readonly query = new QueryState<ClaimDetail>(() => {
    const id = this.claimId();
    return id ? this.api.getClaim(id) : EMPTY;
  });
  private readonly answering = new CommandState();

  readonly state = this.query.state;
  readonly claim = this.query.data;
  readonly answerPending = this.answering.pending;
  readonly answerError = this.answering.error;

  constructor() {
    refreshWhileVisible(() => {
      if (!this.answering.pending()) this.query.load({ background: true });
    });
    inject(DestroyRef).onDestroy(() => this.query.cancel());
  }

  open(claimId: string): void {
    this.claimId.set(claimId);
    this.answering.clearError();
    this.query.load();
  }

  reload(): void {
    this.query.load();
  }

  answer(requestId: string, response: string): void {
    const id = this.claimId();
    if (!id) return;
    this.answering.run(
      this.api.respondToInformationRequest(id, requestId, { response }),
      (claim) => this.query.set(claim),
      (error) => {
        if (error.status === 409) this.query.load({ background: true });
      },
    );
  }
}
