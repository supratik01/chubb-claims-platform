import { Injectable, InjectionToken, computed, inject, signal } from '@angular/core';
import { Observable, map, tap } from 'rxjs';
import { ClaimsApiClient } from '../api/claims-api.client';
import { LoginResponse, Role, UserSummary } from '../api/models';

interface Session {
  readonly accessToken: string;
  readonly expiresAt: number;
  readonly user: UserSummary;
}

const STORAGE_KEY = 'claims.session';

/**
 * Where the session survives a page reload. sessionStorage is cleared when the tab closes. A token readable by
 * script is exposed to XSS; the trade-off against an httpOnly cookie is recorded in NOTES.md (D20).
 */
export const SESSION_STORAGE = new InjectionToken<Storage | null>('SESSION_STORAGE', {
  providedIn: 'root',
  factory: () => (typeof sessionStorage === 'undefined' ? null : sessionStorage),
});

/**
 * The only state shared between the shell and the micro-frontends: who is signed in, with which role and token.
 * The shared library is a federation singleton, so there is exactly one instance of this store at runtime.
 */
@Injectable({ providedIn: 'root' })
export class SessionStore {
  private readonly api = inject(ClaimsApiClient);
  private readonly storage = inject(SESSION_STORAGE);
  private readonly session = signal<Session | null>(this.restore());

  readonly user = computed(() => this.session()?.user ?? null);
  readonly role = computed<Role | null>(() => this.session()?.user.role ?? null);
  readonly isAuthenticated = computed(() => this.session() !== null);
  readonly isStaff = computed(
    () => this.role() === 'CLAIMS_OFFICER' || this.role() === 'CLAIMS_MANAGER',
  );
  readonly isManager = computed(() => this.role() === 'CLAIMS_MANAGER');

  signIn(username: string, password: string): Observable<UserSummary> {
    return this.api.login({ username, password }).pipe(
      tap((response) => this.start(response)),
      map((response) => response.user),
    );
  }

  signOut(): void {
    this.session.set(null);
    this.storage?.removeItem(STORAGE_KEY);
  }

  /** The bearer token, or null when there is none or it has expired. */
  accessToken(): string | null {
    const session = this.session();
    if (!session) return null;
    if (session.expiresAt <= Date.now()) {
      this.signOut();
      return null;
    }
    return session.accessToken;
  }

  private start(response: LoginResponse): void {
    const session: Session = {
      accessToken: response.accessToken,
      expiresAt: Date.now() + response.expiresInSeconds * 1000,
      user: response.user,
    };
    this.session.set(session);
    this.storage?.setItem(STORAGE_KEY, JSON.stringify(session));
  }

  private restore(): Session | null {
    const raw = this.storage?.getItem(STORAGE_KEY);
    if (!raw) return null;
    try {
      const session = JSON.parse(raw) as Session;
      return session.expiresAt > Date.now() ? session : null;
    } catch {
      return null;
    }
  }
}
