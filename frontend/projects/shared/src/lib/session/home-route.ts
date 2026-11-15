import { Role } from '../api/models';

/** The landing page for each role. The shell routes there after sign-in and when a role reaches the wrong area. */
export function homeRouteFor(role: Role): string {
  return role === 'CLAIMANT' ? '/claimant' : '/staff';
}
