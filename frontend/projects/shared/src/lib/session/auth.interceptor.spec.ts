import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { MANAGER, MemoryStorage } from '@claims/shared/testing';
import { authInterceptor } from './auth.interceptor';
import { SESSION_STORAGE, SessionStore } from './session.store';

describe('authInterceptor', () => {
  let http: HttpClient;
  let backend: HttpTestingController;
  let session: SessionStore;
  let router: Router;

  beforeEach(() => {
    const storage = new MemoryStorage();
    storage.setItem(
      'claims.session',
      JSON.stringify({ accessToken: 'token-1', expiresAt: Date.now() + 60_000, user: MANAGER }),
    );
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        { provide: SESSION_STORAGE, useValue: storage },
      ],
    });
    http = TestBed.inject(HttpClient);
    backend = TestBed.inject(HttpTestingController);
    session = TestBed.inject(SessionStore);
    router = TestBed.inject(Router);
  });

  it('sends the bearer token with API calls', () => {
    http.get('/api/v1/claims').subscribe();
    expect(backend.expectOne('/api/v1/claims').request.headers.get('Authorization')).toBe(
      'Bearer token-1',
    );
  });

  it('does not send the token to other addresses', () => {
    http.get('/mfe/claimant/remoteEntry.json').subscribe();
    expect(
      backend.expectOne('/mfe/claimant/remoteEntry.json').request.headers.has('Authorization'),
    ).toBe(false);
  });

  it('signs the user out and returns to sign-in when the API rejects the token', () => {
    const navigate = vi.spyOn(router, 'navigate').mockResolvedValue(true);
    http.get('/api/v1/claims').subscribe({ error: () => undefined });
    backend.expectOne('/api/v1/claims').flush(null, { status: 401, statusText: 'Unauthorized' });

    expect(session.isAuthenticated()).toBe(false);
    expect(navigate).toHaveBeenCalledWith(['/login'], { queryParams: { reason: 'expired' } });
  });

  it('leaves the session alone when a sign-in attempt is refused', () => {
    http.post('/api/v1/auth/login', {}).subscribe({ error: () => undefined });
    backend
      .expectOne('/api/v1/auth/login')
      .flush(null, { status: 401, statusText: 'Unauthorized' });
    expect(session.isAuthenticated()).toBe(true);
  });
});
