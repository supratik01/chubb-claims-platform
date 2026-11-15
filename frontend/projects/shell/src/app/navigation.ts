import { Role } from '@claims/shared';

export interface NavLink {
  readonly label: string;
  readonly path: string;
  readonly exact: boolean;
}

/** The main navigation for each role. Managers see everything officers see, plus exposure. */
export function navigationFor(role: Role): readonly NavLink[] {
  switch (role) {
    case 'CLAIMANT':
      return [
        { label: 'My claims', path: '/claimant', exact: true },
        { label: 'Report an incident', path: '/claimant/report', exact: false },
      ];
    case 'CLAIMS_OFFICER':
      return STAFF_LINKS;
    case 'CLAIMS_MANAGER':
      return [...STAFF_LINKS, { label: 'Exposure', path: '/staff/exposure', exact: false }];
  }
}

const STAFF_LINKS: readonly NavLink[] = [
  { label: 'Work queue', path: '/staff/queue', exact: false },
  { label: 'Team workload', path: '/staff/workload', exact: false },
];
