import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { MANAGER, MemoryStorage } from '@claims/shared/testing';
import { SESSION_STORAGE, SessionStore } from './session.store';

describe('SessionStore', () => {
  let storage: MemoryStorage;

  function setUp(): { store: SessionStore; http: HttpTestingController } {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: SESSION_STORAGE, useValue: storage },
      ],
    });
    return { store: TestBed.inject(SessionStore), http: TestBed.inject(HttpTestingController) };
  }

  beforeEach(() => (storage = new MemoryStorage()));

  it('signs in, exposes the role and keeps the session for a page reload', () => {
    const { store, http } = setUp();

    store.signIn('manager.kenji', 'secret').subscribe();
    const request = http.expectOne('/api/v1/auth/login');
    expect(request.request.body).toEqual({ username: 'manager.kenji', password: 'secret' });
    request.flush({
      accessToken: 'token-1',
      tokenType: 'Bearer',
      expiresInSeconds: 3600,
      user: MANAGER,
    });

    expect(store.role()).toBe('CLAIMS_MANAGER');
    expect(store.isStaff()).toBe(true);
    expect(store.isManager()).toBe(true);
    expect(store.accessToken()).toBe('token-1');
    expect(storage.getItem('claims.session')).toContain('token-1');
  });

  it('restores an unexpired session from storage', () => {
    storage.setItem(
      'claims.session',
      JSON.stringify({ accessToken: 'stored', expiresAt: Date.now() + 60_000, user: MANAGER }),
    );
    const { store } = setUp();
    expect(store.user()?.displayName).toBe('Kenji Sato');
    expect(store.accessToken()).toBe('stored');
  });

  it('ignores an expired session in storage', () => {
    storage.setItem(
      'claims.session',
      JSON.stringify({ accessToken: 'old', expiresAt: Date.now() - 1, user: MANAGER }),
    );
    const { store } = setUp();
    expect(store.isAuthenticated()).toBe(false);
    expect(store.accessToken()).toBeNull();
  });

  it('forgets the user and the stored token on sign-out', () => {
    storage.setItem(
      'claims.session',
      JSON.stringify({ accessToken: 'stored', expiresAt: Date.now() + 60_000, user: MANAGER }),
    );
    const { store } = setUp();
    store.signOut();
    expect(store.isAuthenticated()).toBe(false);
    expect(storage.getItem('claims.session')).toBeNull();
  });
});
