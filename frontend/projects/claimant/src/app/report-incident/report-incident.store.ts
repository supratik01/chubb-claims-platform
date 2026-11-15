import { DestroyRef, Injectable, computed, inject } from '@angular/core';
import { ClaimDetail, ClaimsApiClient, CommandState, QueryState, SubmitClaimRequest } from '@claims/shared';

@Injectable()
export class ReportIncidentStore {
  private readonly api = inject(ClaimsApiClient);
  private readonly policyQuery = new QueryState(() => this.api.listMyPolicies());
  private readonly submission = new CommandState();

  readonly policiesState = this.policyQuery.state;
  readonly policies = computed(() => this.policyQuery.data() ?? []);
  readonly submitting = this.submission.pending;
  readonly submitError = this.submission.error;

  constructor() {
    this.policyQuery.load();
    inject(DestroyRef).onDestroy(() => this.policyQuery.cancel());
  }

  reloadPolicies(): void {
    this.policyQuery.load();
  }

  submit(request: SubmitClaimRequest, onCreated: (claim: ClaimDetail) => void): void {
    this.submission.run(this.api.submitClaim(request), onCreated);
  }
}
