import { Routes } from '@angular/router';
import { ClaimantClaimPage } from './claim/claimant-claim.page';
import { MyClaimsPage } from './my-claims/my-claims.page';
import { ReportIncidentPage } from './report-incident/report-incident.page';

/** Exposed to the shell through Native Federation and mounted under /claimant. Links inside are relative. */
export const routes: Routes = [
  { path: '', title: 'My claims', component: MyClaimsPage },
  { path: 'report', title: 'Report an incident', component: ReportIncidentPage },
  { path: 'claims/:claimId', title: 'Your claim', component: ClaimantClaimPage },
];
