import { CurrencyPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, effect, inject, input, untracked } from '@angular/core';
import { RouterLink } from '@angular/router';
import {
  ClaimFactsComponent,
  ErrorAlertComponent,
  InformationAnswer,
  InformationRequestsComponent,
  PageStateComponent,
  StatusBadgeComponent,
  StatusTimelineComponent,
} from '@claims/shared';
import { ClaimantClaimStore } from './claimant-claim.store';
import { nextStepFor } from './next-step';

@Component({
  selector: 'claimant-claim-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    RouterLink,
    CurrencyPipe,
    ClaimFactsComponent,
    ErrorAlertComponent,
    InformationRequestsComponent,
    PageStateComponent,
    StatusBadgeComponent,
    StatusTimelineComponent,
  ],
  providers: [ClaimantClaimStore],
  templateUrl: './claimant-claim.page.html',
  styleUrl: './claimant-claim.page.scss',
})
export class ClaimantClaimPage {
  /** Route parameter, bound by the router. */
  readonly claimId = input.required<string>();

  protected readonly store = inject(ClaimantClaimStore);
  protected readonly nextStep = computed(() => {
    const claim = this.store.claim();
    return claim ? nextStepFor(claim.status) : '';
  });

  constructor() {
    effect(() => {
      const claimId = this.claimId();
      untracked(() => this.store.open(claimId));
    });
  }

  protected answer(answer: InformationAnswer): void {
    this.store.answer(answer.requestId, answer.response);
  }
}
