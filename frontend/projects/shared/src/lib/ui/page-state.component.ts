import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { RequestState } from '../state/request-state';

/**
 * Renders the loading, error and empty states of a page the same way everywhere. The loaded content is projected
 * and shown only when there is something to show.
 */
@Component({
  selector: 'cc-page-state',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @switch (state().status) {
      @case ('loading') {
        <p class="state state--loading" role="status">
          <span class="spinner" aria-hidden="true"></span>{{ loadingMessage() }}
        </p>
      }
      @case ('error') {
        <div class="state state--error" role="alert">
          <p>{{ errorMessage() }}</p>
          <button type="button" class="cc-button cc-button--secondary" (click)="retry.emit()">Try again</button>
        </div>
      }
      @default {
        @if (empty()) {
          <div class="state state--empty">
            <p>{{ emptyMessage() }}</p>
            <ng-content select="[ccEmptyAction]" />
          </div>
        } @else {
          <ng-content />
        }
      }
    }
  `,
  styles: `
    .state {
      background: var(--cc-color-surface);
      border: 1px dashed var(--cc-color-border);
      border-radius: var(--cc-radius);
      padding: var(--cc-space-6);
      text-align: center;
      color: var(--cc-color-text-muted);
    }
    .state--error { border-style: solid; border-color: var(--cc-color-danger); color: var(--cc-tone-danger-fg); }
    .state p { margin: 0 0 var(--cc-space-3); }
    .state--loading { display: flex; gap: var(--cc-space-3); justify-content: center; align-items: center; }
    .state--loading { margin: 0; }
    .spinner {
      width: 1.25rem;
      height: 1.25rem;
      border: 3px solid var(--cc-color-border);
      border-top-color: var(--cc-color-primary);
      border-radius: 50%;
      animation: spin 0.8s linear infinite;
    }
    @keyframes spin { to { transform: rotate(360deg); } }
  `,
})
export class PageStateComponent {
  readonly state = input.required<RequestState<unknown>>();
  /** True when the data loaded but there is nothing to list. */
  readonly empty = input(false);
  readonly emptyMessage = input('Nothing to show yet.');
  readonly loadingMessage = input('Loading…');
  readonly retry = output<void>();

  protected readonly errorMessage = computed(() => {
    const state = this.state();
    return state.status === 'error' ? state.error.message : '';
  });
}
