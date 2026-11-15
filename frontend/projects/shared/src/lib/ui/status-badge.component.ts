import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { ClaimStatus } from '../api/models';
import { CLAIM_STATUS_LABELS } from './labels';

type Tone = 'info' | 'progress' | 'warning' | 'success' | 'danger' | 'neutral';

const TONES: Readonly<Record<ClaimStatus, Tone>> = {
  SUBMITTED: 'info',
  UNDER_REVIEW: 'progress',
  INFO_REQUESTED: 'warning',
  APPROVED: 'success',
  REJECTED: 'danger',
  SETTLED: 'neutral',
};

/** A claim status as a coloured label. The text always carries the meaning; colour only reinforces it. */
@Component({
  selector: 'cc-status-badge',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<span class="badge" [class]="'badge badge--' + tone()">{{ label() }}</span>`,
  styles: `
    .badge {
      display: inline-block;
      padding: 0.125rem 0.625rem;
      border-radius: 999px;
      font-size: 0.8125rem;
      font-weight: 600;
      white-space: nowrap;
    }
    .badge--info { color: var(--cc-tone-info-fg); background: var(--cc-tone-info-bg); }
    .badge--progress { color: var(--cc-tone-progress-fg); background: var(--cc-tone-progress-bg); }
    .badge--warning { color: var(--cc-tone-warning-fg); background: var(--cc-tone-warning-bg); }
    .badge--success { color: var(--cc-tone-success-fg); background: var(--cc-tone-success-bg); }
    .badge--danger { color: var(--cc-tone-danger-fg); background: var(--cc-tone-danger-bg); }
    .badge--neutral { color: var(--cc-tone-neutral-fg); background: var(--cc-tone-neutral-bg); }
  `,
})
export class StatusBadgeComponent {
  readonly status = input.required<ClaimStatus>();
  protected readonly label = computed(() => CLAIM_STATUS_LABELS[this.status()]);
  protected readonly tone = computed(() => TONES[this.status()]);
}
