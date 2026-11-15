import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import {
  LINE_OF_BUSINESS_LABELS,
  PageStateComponent,
  StatusBadgeComponent,
  isAwaitingClaimant,
  MoneyPipe,
} from '@claims/shared';
import { MyClaimsStore } from './my-claims.store';

@Component({
  selector: 'claimant-my-claims-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MoneyPipe, RouterLink, DatePipe, PageStateComponent, StatusBadgeComponent],
  providers: [MyClaimsStore],
  templateUrl: './my-claims.page.html',
  styleUrl: './my-claims.page.scss',
})
export class MyClaimsPage {
  protected readonly store = inject(MyClaimsStore);
  protected readonly lineLabels = LINE_OF_BUSINESS_LABELS;
  protected readonly isAwaitingClaimant = isAwaitingClaimant;
}
