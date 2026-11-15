import { inject } from '@angular/core';
import { CanMatchFn, Router } from '@angular/router';
import { Role } from '../api/models';
import { homeRouteFor } from './home-route';
import { SessionStore } from './session.store';

/**
 * Lets a route match only for the given roles. Used as `canMatch`, so a micro-frontend's code is not even
 * downloaded for a user who may not see it. The backend enforces the same rules; this guard is for the user,
 * not for security.
 */
export function requireRole(...roles: Role[]): CanMatchFn {
  return (_route, segments) => {
    const session = inject(SessionStore);
    const router = inject(Router);
    const role = session.role();
    if (!role) {
      const returnUrl = '/' + segments.map((segment) => segment.path).join('/');
      return router.createUrlTree(['/login'], { queryParams: { returnUrl } });
    }
    return roles.includes(role) ? true : router.createUrlTree([homeRouteFor(role)]);
  };
}

/** The sign-in page is only for people who are not signed in. */
export const requireSignedOut: CanMatchFn = () => {
  const role = inject(SessionStore).role();
  return role ? inject(Router).createUrlTree([homeRouteFor(role)]) : true;
};
