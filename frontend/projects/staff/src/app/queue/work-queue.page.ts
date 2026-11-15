import { CurrencyPipe, DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, ElementRef, effect, inject, input, untracked } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import {
  ErrorAlertComponent,
  LINE_OF_BUSINESS_LABELS,
  PageStateComponent,
  StatusBadgeComponent,
} from '@claims/shared';
import { QUEUE_VIEWS, QueueView, queueView } from './queue-views';
import { WorkQueueStore } from './work-queue.store';

/**
 * The officer's work queue. The selected view lives in the URL (?view=mine), so it survives opening a claim and
 * coming back, and the store simply follows it.
 */
@Component({
  selector: 'staff-work-queue-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, CurrencyPipe, DatePipe, ErrorAlertComponent, PageStateComponent, StatusBadgeComponent],
  providers: [WorkQueueStore],
  templateUrl: './work-queue.page.html',
  styleUrl: './work-queue.page.scss',
})
export class WorkQueuePage {
  /** Query parameter, bound by the router. */
  readonly view = input<string>();

  protected readonly store = inject(WorkQueueStore);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);

  protected readonly views = QUEUE_VIEWS;
  protected readonly lineLabels = LINE_OF_BUSINESS_LABELS;

  constructor() {
    effect(() => {
      const view = queueView(this.view());
      untracked(() => this.store.show(view));
    });
  }

  protected select(view: QueueView): void {
    void this.router.navigate([], { relativeTo: this.route, queryParams: { view } });
  }

  /** Arrow keys move between tabs, as in the WAI-ARIA tabs pattern. */
  protected onTabKey(event: KeyboardEvent, index: number): void {
    const offsets: Record<string, number> = { ArrowRight: 1, ArrowLeft: -1 };
    let next: number;
    if (event.key in offsets) next = (index + offsets[event.key] + this.views.length) % this.views.length;
    else if (event.key === 'Home') next = 0;
    else if (event.key === 'End') next = this.views.length - 1;
    else return;
    event.preventDefault();
    this.select(this.views[next].view);
    this.host.nativeElement.querySelector<HTMLElement>(`#tab-${this.views[next].view}`)?.focus();
  }

  protected pickUp(claimId: string): void {
    this.store.pickUp(claimId, (claim) =>
      void this.router.navigate(['..', 'claims', claim.id], { relativeTo: this.route }),
    );
  }
}
