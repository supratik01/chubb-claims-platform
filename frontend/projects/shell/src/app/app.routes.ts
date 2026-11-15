import { inject } from '@angular/core';
import { Routes } from '@angular/router';
import { SessionStore, homeRouteFor, requireRole, requireSignedOut } from '@claims/shared';
import { LoginPage } from './login/login.page';
import { NotFoundPage } from './pages/not-found.page';
import { loadRemoteRoutes } from './remotes';

/**
 * The shell owns the top-level routes only. Each user type's area is a micro-frontend loaded at runtime, and
 * `canMatch` checks the role before its code is downloaded.
 */
export const routes: Routes = [
  { path: 'login', title: 'Sign in', canMatch: [requireSignedOut], component: LoginPage },
  {
    path: 'claimant',
    canMatch: [requireRole('CLAIMANT')],
    loadChildren: loadRemoteRoutes('claimant'),
  },
  {
    path: 'staff',
    canMatch: [requireRole('CLAIMS_OFFICER', 'CLAIMS_MANAGER')],
    loadChildren: loadRemoteRoutes('staff'),
  },
  {
    path: '',
    pathMatch: 'full',
    redirectTo: () => {
      const role = inject(SessionStore).role();
      return role ? homeRouteFor(role) : '/login';
    },
  },
  { path: '**', title: 'Page not found', component: NotFoundPage },
];
