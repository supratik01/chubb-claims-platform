import { CurrencyPipe, DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { ClaimDetail } from '../api/models';
import { LINE_OF_BUSINESS_LABELS } from './labels';

/**
 * The facts of a claim as a description list. Staff also see the sum insured and the reserve, which the API
 * returns only to staff.
 */
@Component({
  selector: 'cc-claim-facts',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CurrencyPipe, DatePipe],
  template: `
    @let c = claim();
    <dl class="facts">
      <div><dt>Policy</dt><dd>{{ c.policyNumber }}</dd></div>
      <div><dt>Cover</dt><dd>{{ lineLabels[c.lineOfBusiness] }}, {{ c.countryCode }}</dd></div>
      <div><dt>Claimant</dt><dd>{{ c.claimantName }}</dd></div>
      <div><dt>Incident date</dt><dd>{{ c.incidentDate | date: 'mediumDate' }}</dd></div>
      @if (c.incidentLocation) {
        <div><dt>Location</dt><dd>{{ c.incidentLocation }}</dd></div>
      }
      <div><dt>Amount claimed</dt><dd>{{ c.claimedAmount | currency: c.currency : 'code' }}</dd></div>
      @if (c.sumInsured != null) {
        <div><dt>Sum insured</dt><dd>{{ c.sumInsured | currency: c.currency : 'code' }}</dd></div>
      }
      @if (c.reserveAmount != null) {
        <div><dt>Outstanding reserve</dt><dd>{{ c.reserveAmount | currency: c.currency : 'code' }}</dd></div>
      }
      @if (c.approvedAmount != null) {
        <div><dt>Approved amount</dt><dd>{{ c.approvedAmount | currency: c.currency : 'code' }}</dd></div>
      }
      <div><dt>Handled by</dt><dd>{{ c.assignedOfficerName ?? 'Not yet assigned' }}</dd></div>
      <div><dt>Reported</dt><dd>{{ c.submittedAt | date: 'medium' }}</dd></div>
    </dl>
    <h3 class="facts__heading">What happened</h3>
    <p class="facts__description">{{ c.description }}</p>
  `,
  styles: `
    .facts {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(12rem, 1fr));
      gap: var(--cc-space-4);
      margin: 0 0 var(--cc-space-6);
    }
    dt { color: var(--cc-color-text-muted); font-size: 0.875rem; }
    dd { margin: 0; font-weight: 600; }
    .facts__heading { margin-bottom: var(--cc-space-2); }
    .facts__description { margin: 0; white-space: pre-line; }
  `,
})
export class ClaimFactsComponent {
  readonly claim = input.required<ClaimDetail>();
  protected readonly lineLabels = LINE_OF_BUSINESS_LABELS;
}
