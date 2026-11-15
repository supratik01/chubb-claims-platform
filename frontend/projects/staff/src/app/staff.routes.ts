import { Routes } from '@angular/router';
import { requireRole } from '@claims/shared';
import { StaffClaimPage } from './claim/staff-claim.page';
import { ExposurePage } from './exposure/exposure.page';
import { WorkQueuePage } from './queue/work-queue.page';
import { WorkloadPage } from './workload/workload.page';

/** Exposed to the shell through Native Federation and mounted under /staff. Links inside are relative. */
export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'queue' },
  { path: 'queue', title: 'Work queue', component: WorkQueuePage },
  { path: 'claims/:claimId', title: 'Review claim', component: StaffClaimPage },
  { path: 'workload', title: 'Team workload', component: WorkloadPage },
  {
    path: 'exposure',
    title: 'Outstanding exposure',
    canMatch: [requireRole('CLAIMS_MANAGER')],
    component: ExposurePage,
  },
];
