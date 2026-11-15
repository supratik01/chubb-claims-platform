import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { PageStateComponent } from '@claims/shared';
import { WorkloadStore } from './workload.store';

/** Team workload, read live from the claim table: what is waiting on whom. */
@Component({
  selector: 'staff-workload-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DatePipe, RouterLink, PageStateComponent],
  providers: [WorkloadStore],
  template: `
    <section class="cc-page">
      <header class="cc-page-header">
        <h1>Team workload</h1>
        @if (store.loadedAt(); as loadedAt) {
          <p class="cc-muted">Updated {{ loadedAt | date: 'mediumTime' }}</p>
        }
      </header>

      <cc-page-state [state]="store.state()" loadingMessage="Loading workload…" (retry)="store.reload()">
        <div class="cc-card backlog">
          <p class="backlog__figure">{{ store.unassigned() }}</p>
          <p>
            {{ store.unassigned() === 1 ? 'claim is' : 'claims are' }} waiting to be picked up.
            <a routerLink="../queue">Open the queue</a>
          </p>
        </div>

        @if (store.officers().length === 0) {
          <p class="cc-muted">No claims have been assigned to an officer yet.</p>
        } @else {
          <div class="cc-table-wrapper">
            <table class="cc-table">
              <caption>Claims per officer, by whose move it is</caption>
              <thead>
                <tr>
                  <th scope="col">Officer</th>
                  <th scope="col" class="cc-numeric">Under review<br /><span class="hint">waiting on officer</span></th>
                  <th scope="col" class="cc-numeric">Awaiting information<br /><span class="hint">waiting on claimant</span></th>
                  <th scope="col" class="cc-numeric">Awaiting settlement<br /><span class="hint">approved, unpaid</span></th>
                  <th scope="col" class="cc-numeric">Closed<br /><span class="hint">rejected or settled</span></th>
                </tr>
              </thead>
              <tbody>
                @for (officer of store.officers(); track officer.officerId) {
                  <tr>
                    <th scope="row">{{ officer.officerName }}</th>
                    <td class="cc-numeric">{{ officer.underReview }}</td>
                    <td class="cc-numeric">{{ officer.awaitingInformation }}</td>
                    <td class="cc-numeric">{{ officer.awaitingSettlement }}</td>
                    <td class="cc-numeric">{{ officer.closed }}</td>
                  </tr>
                }
              </tbody>
              <tfoot>
                @let totals = store.totals();
                <tr>
                  <th scope="row">Team total</th>
                  <td class="cc-numeric">{{ totals.underReview }}</td>
                  <td class="cc-numeric">{{ totals.awaitingInformation }}</td>
                  <td class="cc-numeric">{{ totals.awaitingSettlement }}</td>
                  <td class="cc-numeric">{{ totals.closed }}</td>
                </tr>
              </tfoot>
            </table>
          </div>
        }
      </cc-page-state>
    </section>
  `,
  styles: `
    .backlog { display: flex; align-items: center; gap: var(--cc-space-4); }
    .backlog p { margin: 0; }
    .backlog__figure { font-size: 2rem; font-weight: 700; color: var(--cc-color-primary); }
    .hint { font-weight: 400; font-size: 0.75rem; }
    tfoot th, tfoot td { font-weight: 700; border-bottom: none; }
  `,
})
export class WorkloadPage {
  protected readonly store = inject(WorkloadStore);
}
