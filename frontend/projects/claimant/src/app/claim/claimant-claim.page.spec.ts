import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { REFRESH_INTERVAL_MS } from '@claims/shared';
import { aClaim, anInformationRequest } from '@claims/shared/testing';
import { ClaimantClaimPage } from './claimant-claim.page';

describe('ClaimantClaimPage', () => {
  async function open(claim: ReturnType<typeof aClaim>) {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: REFRESH_INTERVAL_MS, useValue: 0 },
      ],
    });
    const http = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(ClaimantClaimPage);
    fixture.componentRef.setInput('claimId', claim.id);
    await fixture.whenStable();
    http.expectOne(`/api/v1/claims/${claim.id}`).flush(claim);
    await fixture.whenStable();
    return { fixture, http, element: fixture.nativeElement as HTMLElement };
  }

  it('sends the answer and shows the claim the server returns', async () => {
    const waiting = aClaim({
      status: 'INFO_REQUESTED',
      informationRequests: [anInformationRequest()],
    });
    const { fixture, http, element } = await open(waiting);
    expect(element.textContent).toContain('We need more information from you.');

    const textarea = element.querySelector('textarea')!;
    textarea.value = 'Serviced in March.';
    textarea.dispatchEvent(new Event('input'));
    element.querySelector('.request__form')!.dispatchEvent(new Event('submit'));
    await fixture.whenStable();

    const request = http.expectOne(
      '/api/v1/claims/claim-1/information-requests/request-1/response',
    );
    expect(request.request.body).toEqual({ response: 'Serviced in March.' });
    request.flush(
      aClaim({
        status: 'UNDER_REVIEW',
        informationRequests: [anInformationRequest({ response: 'Serviced in March.' })],
      }),
    );
    await fixture.whenStable();

    expect(element.textContent).toContain('Answer: Serviced in March.');
    expect(element.querySelector('textarea')).toBeNull();
  });

  it('shows the decision and its reason once the claim is rejected', async () => {
    const { element } = await open(
      aClaim({ status: 'REJECTED', decisionReason: 'Wear and tear is excluded.' }),
    );
    expect(element.querySelector('#decision-heading')?.textContent).toContain('not approved');
    expect(element.textContent).toContain('Wear and tear is excluded.');
  });
});
