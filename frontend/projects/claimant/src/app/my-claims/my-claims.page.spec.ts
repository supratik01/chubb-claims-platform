import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { REFRESH_INTERVAL_MS } from '@claims/shared';
import { aClaimSummary } from '@claims/shared/testing';
import { MyClaimsPage } from './my-claims.page';

describe('MyClaimsPage', () => {
  async function render(items: ReturnType<typeof aClaimSummary>[]) {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: REFRESH_INTERVAL_MS, useValue: 0 },
      ],
    });
    const fixture = TestBed.createComponent(MyClaimsPage);
    await fixture.whenStable();
    TestBed.inject(HttpTestingController)
      .expectOne((request) => request.url === '/api/v1/claims')
      .flush({ items, page: 0, size: 100, totalItems: items.length, totalPages: 1 });
    await fixture.whenStable();
    return fixture.nativeElement as HTMLElement;
  }

  it('flags the claims that are waiting on the claimant', async () => {
    const element = await render([
      aClaimSummary({ id: 'a', claimNumber: 'CLM-1', status: 'INFO_REQUESTED' }),
      aClaimSummary({ id: 'b', claimNumber: 'CLM-2', status: 'UNDER_REVIEW' }),
    ]);
    expect(element.textContent).toContain('One claim needs an answer from you.');
    const cards = element.querySelectorAll('.claim');
    expect(cards[0].textContent).toContain('Action required');
    expect(cards[1].textContent).not.toContain('Action required');
  });

  it('invites a claimant with no claims to report their first incident', async () => {
    const element = await render([]);
    expect(element.textContent).toContain('You have not reported any incidents yet.');
    expect(element.textContent).toContain('Report your first incident');
  });
});
