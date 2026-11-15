import { DestroyRef, InjectionToken, inject } from '@angular/core';

/** How often lists and open claims refresh in the background. Tests set it to 0 to turn refreshing off. */
export const REFRESH_INTERVAL_MS = new InjectionToken<number>('REFRESH_INTERVAL_MS', {
  providedIn: 'root',
  factory: () => 10_000,
});

/**
 * Calls `refresh` on an interval while the page is visible, and stops when the owner is destroyed.
 * Must be called in an injection context (a store's constructor).
 *
 * This is how the asynchronous nature of claims reaches the screen: a decision made by someone else appears
 * without the user reloading. Server push was considered and deferred (NOTES.md, D16).
 */
export function refreshWhileVisible(refresh: () => void): void {
  const intervalMs = inject(REFRESH_INTERVAL_MS);
  if (intervalMs <= 0) return;
  const timer = setInterval(() => {
    if (document.visibilityState === 'visible') refresh();
  }, intervalMs);
  inject(DestroyRef).onDestroy(() => clearInterval(timer));
}
