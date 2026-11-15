import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { REFRESH_INTERVAL_MS } from '@claims/shared';
import { aClaim, aClaimSummary } from '@claims/shared/testing';
import { WorkQueuePage } from './work-queue.page';

describe('WorkQueuePage', () => {
  let fixture: ComponentFixture<WorkQueuePage>;
  let http: HttpTestingController;
  let element: HTMLElement;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: REFRESH_INTERVAL_MS, useValue: 0 },
      ],
    });
    http = TestBed.inject(HttpTestingController);
  });

  async function render(view?: string) {
    fixture = TestBed.createComponent(WorkQueuePage);
    if (view) fixture.componentRef.setInput('view', view);
    await fixture.whenStable();
    element = fixture.nativeElement;
  }

  function page(items: ReturnType<typeof aClaimSummary>[]) {
    return { items, page: 0, size: 100, totalItems: items.length, totalPages: 1 };
  }

  it('opens on the unassigned queue', async () => {
    await render();
    const request = http.expectOne((r) => r.url === '/api/v1/claims');
    expect(request.request.params.get('assignment')).toBe('UNASSIGNED');
    request.flush(page([aClaimSummary()]));
    await fixture.whenStable();

    const selected = element.querySelector('[role="tab"][aria-selected="true"]');
    expect(selected?.textContent?.trim()).toBe('Unassigned');
    expect(element.querySelector('caption')?.textContent).toContain('1 claim');
  });

  it("asks only for the officer's open claims in the My open claims view", async () => {
    await render('mine');
    const request = http.expectOne((r) => r.url === '/api/v1/claims');
    expect(request.request.params.get('assignment')).toBe('MINE');
    expect(request.request.params.getAll('status')).toEqual([
      'UNDER_REVIEW',
      'INFO_REQUESTED',
      'APPROVED',
    ]);
  });

  it('picks up a claim and opens it', async () => {
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    await render();
    http
      .expectOne((r) => r.url === '/api/v1/claims')
      .flush(page([aClaimSummary({ id: 'claim-9' })]));
    await fixture.whenStable();

    element.querySelector<HTMLButtonElement>('button[aria-label^="Pick up claim"]')!.click();
    http
      .expectOne({ method: 'POST', url: '/api/v1/claims/claim-9/assignment' })
      .flush(aClaim({ id: 'claim-9' }));

    expect(navigate).toHaveBeenCalledWith(['..', 'claims', 'claim-9'], expect.anything());
  });

  it('explains a lost race and refreshes the queue', async () => {
    await render();
    http
      .expectOne((r) => r.url === '/api/v1/claims')
      .flush(page([aClaimSummary({ id: 'claim-9' })]));
    await fixture.whenStable();

    element.querySelector<HTMLButtonElement>('button[aria-label^="Pick up claim"]')!.click();
    http
      .expectOne('/api/v1/claims/claim-9/assignment')
      .flush(
        { status: 409, title: 'Conflict', code: 'CONCURRENT_MODIFICATION' },
        { status: 409, statusText: 'Conflict' },
      );
    await fixture.whenStable();

    expect(element.querySelector('[role="alert"]')?.textContent).toContain(
      'Someone else changed this claim',
    );
    http.expectOne((r) => r.url === '/api/v1/claims');
  });
});
