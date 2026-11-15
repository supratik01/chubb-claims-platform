import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { REFRESH_INTERVAL_MS } from '@claims/shared';
import { ExposurePage } from './exposure.page';

describe('ExposurePage', () => {
  it('reports outstanding amounts per currency without adding currencies together', async () => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: REFRESH_INTERVAL_MS, useValue: 0 },
      ],
    });
    const fixture = TestBed.createComponent(ExposurePage);
    await fixture.whenStable();
    TestBed.inject(HttpTestingController)
      .expectOne('/api/v1/dashboard/exposure')
      .flush({
        totals: [
          { currency: 'AUD', openClaims: 2, outstandingAmount: 35800 },
          { currency: 'SGD', openClaims: 2, outstandingAmount: 22700 },
        ],
        breakdown: [
          {
            countryCode: 'SG',
            lineOfBusiness: 'PROPERTY',
            currency: 'SGD',
            openClaims: 1,
            outstandingAmount: 18500,
            updatedAt: '2026-11-15T09:00:00Z',
          },
        ],
      });
    await fixture.whenStable();

    const element = fixture.nativeElement as HTMLElement;
    const amounts = [...element.querySelectorAll('.total__amount')].map((amount) =>
      amount.textContent?.trim(),
    );
    expect(amounts).toEqual([
      expect.stringMatching(/^AUD\s35,800\.00$/),
      expect.stringMatching(/^SGD\s22,700\.00$/),
    ]);
    expect(element.querySelector('.total')?.textContent).toContain('2 open claims');
    expect(element.textContent).not.toContain('58,500');
  });
});
