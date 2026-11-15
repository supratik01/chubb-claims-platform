import { DestroyRef, Injectable, computed, inject, signal } from '@angular/core';
import {
  ClaimDetail,
  ClaimsApiClient,
  CommandState,
  QueryState,
  SessionStore,
  refreshWhileVisible,
} from '@claims/shared';
import { EMPTY, Observable } from 'rxjs';
import { availableActions, waitingReason } from './claim-actions';

/**
 * State for one claim under review, and every workflow command on it. Each command returns the updated claim,
 * which replaces the current one, so the screen always shows what the server accepted.
 */
@Injectable()
export class StaffClaimStore {
  private readonly api = inject(ClaimsApiClient);
  private readonly session = inject(SessionStore);
  private readonly claimId = signal<string | null>(null);
  private readonly query = new QueryState<ClaimDetail>(() => {
    const id = this.claimId();
    return id ? this.api.getClaim(id) : EMPTY;
  });
  private readonly command = new CommandState();

  readonly state = this.query.state;
  readonly claim = this.query.data;
  readonly busy = this.command.pending;
  readonly error = this.command.error;
  readonly actions = computed(() => {
    const claim = this.claim();
    const user = this.session.user();
    return claim && user ? availableActions(claim, user) : [];
  });
  readonly waitingReason = computed(() => {
    const claim = this.claim();
    return claim ? waitingReason(claim, this.actions()) : null;
  });

  constructor() {
    refreshWhileVisible(() => {
      if (!this.command.pending()) this.query.load({ background: true });
    });
    inject(DestroyRef).onDestroy(() => this.query.cancel());
  }

  open(claimId: string): void {
    this.claimId.set(claimId);
    this.command.clearError();
    this.query.load();
  }

  reload(): void {
    this.query.load();
  }

  pickUp(): void {
    this.execute((id) => this.api.pickUpClaim(id));
  }

  requestInformation(question: string): void {
    this.execute((id) => this.api.requestInformation(id, { question }));
  }

  approve(approvedAmount: number, note: string | null): void {
    this.execute((id) => this.api.decideClaim(id, { outcome: 'APPROVED', approvedAmount, reason: note }));
  }

  reject(reason: string): void {
    this.execute((id) => this.api.decideClaim(id, { outcome: 'REJECTED', reason }));
  }

  settle(): void {
    this.execute((id) => this.api.settleClaim(id));
  }

  private execute(command: (claimId: string) => Observable<ClaimDetail>): void {
    const id = this.claimId();
    if (!id) return;
    this.command.run(
      command(id),
      (claim) => this.query.set(claim),
      // A conflict means the claim moved on (someone else acted first): show its current state.
      (error) => {
        if (error.status === 409) this.query.load({ background: true });
      },
    );
  }
}
