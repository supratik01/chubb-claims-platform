import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { aClaim, aPolicy } from '@claims/shared/testing';
import { ReportIncidentPage } from './report-incident.page';

describe('ReportIncidentPage', () => {
  let fixture: ComponentFixture<ReportIncidentPage>;
  let element: HTMLElement;
  let http: HttpTestingController;
  let navigate: ReturnType<typeof vi.spyOn>;

  const daysFromToday = (days: number) =>
    new Date(Date.now() + days * 86_400_000).toISOString().slice(0, 10);
  const policy = aPolicy({ coverStart: daysFromToday(-200), coverEnd: daysFromToday(165) });

  beforeEach(async () => {
    TestBed.configureTestingModule({
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
    navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(ReportIncidentPage);
    element = fixture.nativeElement;
    await fixture.whenStable();
    http.expectOne('/api/v1/policies').flush([policy]);
    await fixture.whenStable();
  });

  function set(selector: string, value: string, event = 'input'): void {
    const field = element.querySelector<HTMLInputElement | HTMLSelectElement>(selector)!;
    field.value = value;
    field.dispatchEvent(new Event(event));
  }

  async function submit(): Promise<void> {
    element.querySelector('form')!.dispatchEvent(new Event('submit'));
    await fixture.whenStable();
  }

  it("lists the claimant's policies to choose from", () => {
    expect(element.querySelector('#policyId')!.textContent).toContain('POL-SG-M-10021');
  });

  it('explains every missing field and sends nothing', async () => {
    await submit();
    expect(element.querySelectorAll('[aria-invalid="true"]').length).toBe(4);
    expect(element.textContent).toContain('Choose the policy this incident is covered by.');
    expect(element.textContent).toContain('Describe what happened.');
    http.expectNone('/api/v1/claims');
  });

  it('submits the claim in the shape the contract defines and opens it', async () => {
    set('#policyId', policy.id, 'change');
    set('#incidentDate', daysFromToday(-1));
    set('#incidentLocation', 'Bukit Timah Road');
    set('#description', '  Another car reversed into my parked car.  ');
    set('#claimedAmount', '4200');
    await submit();

    const request = http.expectOne({ method: 'POST', url: '/api/v1/claims' });
    expect(request.request.body).toEqual({
      policyId: policy.id,
      incidentDate: daysFromToday(-1),
      incidentLocation: 'Bukit Timah Road',
      description: 'Another car reversed into my parked car.',
      claimedAmount: 4200,
    });
    request.flush(aClaim({ id: 'new-claim' }));

    expect(navigate).toHaveBeenCalledWith(['..', 'claims', 'new-claim'], expect.anything());
  });

  it("shows the server's reason when it refuses the claim", async () => {
    set('#policyId', policy.id, 'change');
    set('#incidentDate', daysFromToday(-1));
    set('#description', 'Another car reversed into my parked car.');
    set('#claimedAmount', '4200');
    await submit();
    http
      .expectOne('/api/v1/claims')
      .flush(
        {
          status: 422,
          title: 'Unprocessable',
          code: 'BUSINESS_RULE_VIOLATED',
          detail: 'The policy is not active',
        },
        { status: 422, statusText: 'Unprocessable Content' },
      );
    await fixture.whenStable();

    expect(element.querySelector('[role="alert"]')?.textContent).toContain(
      'The policy is not active',
    );
    expect(navigate).not.toHaveBeenCalled();
  });
});
