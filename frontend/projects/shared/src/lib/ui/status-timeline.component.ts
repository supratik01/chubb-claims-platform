import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { StatusHistoryEntry } from '../api/models';
import { StatusBadgeComponent } from './status-badge.component';

/** The claim's status history, oldest first, as an ordered list so assistive technology reads it in sequence. */
@Component({
  selector: 'cc-status-timeline',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DatePipe, StatusBadgeComponent],
  template: `
    <ol class="timeline">
      @for (entry of entries(); track $index) {
        <li class="timeline__entry">
          <div class="timeline__head">
            <cc-status-badge [status]="entry.toStatus" />
            <time [attr.datetime]="entry.changedAt">{{ entry.changedAt | date: 'medium' }}</time>
          </div>
          <p class="timeline__by">by {{ entry.changedByName }}</p>
          @if (entry.note) {
            <p class="timeline__note">{{ entry.note }}</p>
          }
        </li>
      }
    </ol>
  `,
  styles: `
    .timeline { list-style: none; margin: 0; padding: 0; border-left: 2px solid var(--cc-color-border); }
    .timeline__entry { position: relative; padding: 0 0 var(--cc-space-4) var(--cc-space-4); }
    .timeline__entry::before {
      content: '';
      position: absolute;
      left: -7px;
      top: 0.45rem;
      width: 12px;
      height: 12px;
      border-radius: 50%;
      background: var(--cc-color-primary);
    }
    .timeline__head { display: flex; flex-wrap: wrap; gap: var(--cc-space-3); align-items: center; }
    .timeline__head time, .timeline__by { color: var(--cc-color-text-muted); font-size: 0.875rem; }
    .timeline__by { margin: var(--cc-space-1) 0 0; }
    .timeline__note { margin: var(--cc-space-1) 0 0; }
  `,
})
export class StatusTimelineComponent {
  readonly entries = input.required<readonly StatusHistoryEntry[]>();
}
