import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { API_BASE_URL } from '../api/claims-api.client';
import { SessionStore } from './session.store';

/**
 * Attaches the bearer token to API calls and ends the session when the API says it is no longer valid.
 * Registered once, by the shell; the micro-frontends' routes run inside the shell's injector and use it too.
 */
export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const session = inject(SessionStore);
  const router = inject(Router);
  const baseUrl = inject(API_BASE_URL);

  if (!request.url.startsWith(baseUrl)) return next(request);

  const token = session.accessToken();
  const authorised = token
    ? request.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : request;
  const isLogin = request.url.endsWith('/auth/login');

  return next(authorised).pipe(
    catchError((error: unknown) => {
      if (!isLogin && error instanceof HttpErrorResponse && error.status === 401) {
        session.signOut();
        void router.navigate(['/login'], { queryParams: { reason: 'expired' } });
      }
      return throwError(() => error);
    }),
  );
};
