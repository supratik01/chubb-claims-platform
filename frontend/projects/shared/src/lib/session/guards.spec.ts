import { TestBed } from '@angular/core/testing';
import { Route, Router, UrlSegment, UrlTree, provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { CLAIMANT, MANAGER, MemoryStorage } from '@claims/shared/testing';
import { UserSummary } from '../api/models';
import { requireRole, requireSignedOut } from './guards';
import { SESSION_STORAGE } from './session.store';

function signedInAs(user: UserSummary | null): void {
  const storage = new MemoryStorage();
  if (user) {
    storage.setItem(
      'claims.session',
      JSON.stringify({ accessToken: 't', expiresAt: Date.now() + 60_000, user }),
    );
  }
  TestBed.configureTestingModule({
    providers: [
      provideRouter([]),
      provideHttpClient(),
      { provide: SESSION_STORAGE, useValue: storage },
    ],
  });
}

function run(guard: ReturnType<typeof requireRole>, path: string): boolean | UrlTree {
  const segments = path.split('/').map((part) => new UrlSegment(part, {}));
  return TestBed.runInInjectionContext(() =>
    guard({} as Route, segments, {} as Parameters<typeof guard>[2]),
  ) as boolean | UrlTree;
}

function url(result: boolean | UrlTree): string {
  return TestBed.inject(Router).serializeUrl(result as UrlTree);
}

describe('requireRole', () => {
  it('sends a signed-out user to sign-in and remembers where they were going', () => {
    signedInAs(null);
    expect(url(run(requireRole('CLAIMANT'), 'claimant/report'))).toBe(
      '/login?returnUrl=%2Fclaimant%2Freport',
    );
  });

  it('lets a user with an allowed role through', () => {
    signedInAs(MANAGER);
    expect(run(requireRole('CLAIMS_OFFICER', 'CLAIMS_MANAGER'), 'staff')).toBe(true);
  });

  it('sends a user with another role to their own home page', () => {
    signedInAs(CLAIMANT);
    expect(url(run(requireRole('CLAIMS_OFFICER', 'CLAIMS_MANAGER'), 'staff'))).toBe('/claimant');
  });
});

describe('requireSignedOut', () => {
  it('sends a signed-in user away from the sign-in page', () => {
    signedInAs(MANAGER);
    expect(url(run(requireSignedOut, 'login'))).toBe('/staff');
  });
});
