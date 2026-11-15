import { DestroyRef, Injectable, computed, inject, signal } from '@angular/core';
import { ClaimDetail, ClaimsApiClient, CommandState, QueryState, refreshWhileVisible } from '@claims/shared';
import { QUEUE_VIEWS, QueueViewDefinition } from './queue-views';

@Injectable()
export class WorkQueueStore {
  private readonly api = inject(ClaimsApiClient);
  private readonly current = signal<QueueViewDefinition>(QUEUE_VIEWS[0]);
  private readonly query = new QueryState(() => this.api.listClaims(this.current().query));
  private readonly pickUpCommand = new CommandState();

  readonly view = this.current.asReadonly();
  readonly state = this.query.state;
  readonly claims = computed(() => this.query.data()?.items ?? []);
  readonly total = computed(() => this.query.data()?.totalItems ?? 0);
  readonly pickingUp = this.pickUpCommand.pending;
  readonly pickUpError = this.pickUpCommand.error;

  constructor() {
    refreshWhileVisible(() => this.query.load({ background: true }));
    inject(DestroyRef).onDestroy(() => this.query.cancel());
  }

  show(view: QueueViewDefinition): void {
    this.current.set(view);
    this.pickUpCommand.clearError();
    this.query.load();
  }

  reload(): void {
    this.query.load();
  }

  /** Assigns the claim to the caller. If someone else got there first, the list is refreshed to show it. */
  pickUp(claimId: string, onPickedUp: (claim: ClaimDetail) => void): void {
    this.pickUpCommand.run(this.api.pickUpClaim(claimId), onPickedUp, () => this.query.load({ background: true }));
  }
}
