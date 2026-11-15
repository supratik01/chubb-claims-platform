import { Signal, computed, signal, untracked } from '@angular/core';
import { Observable, Subscription } from 'rxjs';
import { ApiError, toApiError } from '../api/api-error';

/** The three states every server read can be in. Templates switch on `status`; nothing else is possible. */
export type RequestState<T> =
  | { readonly status: 'loading' }
  | { readonly status: 'loaded'; readonly data: T }
  | { readonly status: 'error'; readonly error: ApiError };

/**
 * One server read owned by a store. Components only read `state` and `data`; the owning store decides when to load.
 *
 * A background refresh keeps the current data on screen while it runs and keeps it if the refresh fails, so a
 * periodic refresh never makes the page flicker or replaces good data with an error.
 */
export class QueryState<T> {
  private readonly current = signal<RequestState<T>>({ status: 'loading' });
  private readonly lastLoaded = signal<Date | null>(null);
  private inFlight?: Subscription;

  readonly state: Signal<RequestState<T>> = this.current.asReadonly();
  readonly data: Signal<T | null> = computed(() => {
    const state = this.current();
    return state.status === 'loaded' ? state.data : null;
  });
  readonly loadedAt: Signal<Date | null> = this.lastLoaded.asReadonly();

  constructor(private readonly fetch: () => Observable<T>) {}

  /**
   * Starts a load. It runs untracked, so calling it from an effect (for example when a route parameter changes)
   * makes the effect depend only on what the caller read, never on this query's own state.
   */
  load(options: { background?: boolean } = {}): void {
    untracked(() => {
      const keepData = options.background === true && this.current().status === 'loaded';
      if (!keepData) this.current.set({ status: 'loading' });
      this.inFlight?.unsubscribe();
      this.inFlight = this.fetch().subscribe({
        next: (data) => this.set(data),
        error: (error: unknown) => {
          if (!keepData) this.current.set({ status: 'error', error: toApiError(error) });
        },
      });
    });
  }

  /** Replaces the data with a fresher copy, such as the claim a command returned. */
  set(data: T): void {
    this.current.set({ status: 'loaded', data });
    this.lastLoaded.set(new Date());
  }

  cancel(): void {
    this.inFlight?.unsubscribe();
  }
}

/**
 * The state of a command (a write). At most one runs at a time per store, which also stops double submits.
 * The error stays until the next attempt or until the store clears it.
 */
export class CommandState {
  private readonly running = signal(false);
  private readonly failure = signal<ApiError | null>(null);

  readonly pending: Signal<boolean> = this.running.asReadonly();
  readonly error: Signal<ApiError | null> = this.failure.asReadonly();

  run<T>(command: Observable<T>, onSuccess: (result: T) => void, onError?: (error: ApiError) => void): void {
    if (untracked(this.running)) return;
    this.running.set(true);
    this.failure.set(null);
    command.subscribe({
      next: (result) => {
        this.running.set(false);
        onSuccess(result);
      },
      error: (error: unknown) => {
        const apiError = toApiError(error);
        this.running.set(false);
        this.failure.set(apiError);
        onError?.(apiError);
      },
    });
  }

  clearError(): void {
    this.failure.set(null);
  }
}
