import { ChangeDetectionStrategy, Component, effect, inject, input, untracked } from '@angular/core';
import { RouterLink } from '@angular/router';
import {
  ClaimFactsComponent,
  ErrorAlertComponent,
  InformationRequestsComponent,
  PageStateComponent,
  StatusBadgeComponent,
  StatusTimelineComponent,
} from '@claims/shared';
import { ApprovalInput, ClaimActionsComponent } from './claim-actions.component';
import { StaffClaimStore } from './staff-claim.store';

@Component({
  selector: 'staff-claim-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    RouterLink,
    ClaimActionsComponent,
    ClaimFactsComponent,
    ErrorAlertComponent,
    InformationRequestsComponent,
    PageStateComponent,
    StatusBadgeComponent,
    StatusTimelineComponent,
  ],
  providers: [StaffClaimStore],
  templateUrl: './staff-claim.page.html',
  styleUrl: './staff-claim.page.scss',
})
export class StaffClaimPage {
  /** Route parameter, bound by the router. */
  readonly claimId = input.required<string>();

  protected readonly store = inject(StaffClaimStore);

  constructor() {
    effect(() => {
      const claimId = this.claimId();
      untracked(() => this.store.open(claimId));
    });
  }

  protected approve(input: ApprovalInput): void {
    this.store.approve(input.approvedAmount, input.note);
  }
}
