import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { SESSION_STORAGE } from '@claims/shared';
import { CLAIMANT, MemoryStorage } from '@claims/shared/testing';
import { LoginPage } from './login.page';

describe('LoginPage', () => {
  let fixture: ComponentFixture<LoginPage>;
  let element: HTMLElement;
  let http: HttpTestingController;
  let navigateByUrl: ReturnType<typeof vi.spyOn>;

  beforeEach(async () => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: SESSION_STORAGE, useValue: new MemoryStorage() },
      ],
    });
    navigateByUrl = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(LoginPage);
    element = fixture.nativeElement;
    await fixture.whenStable();
  });

  function type(selector: string, value: string): void {
    const input = element.querySelector<HTMLInputElement>(selector)!;
    input.value = value;
    input.dispatchEvent(new Event('input'));
  }

  async function submit(): Promise<void> {
    element.querySelector('form')!.dispatchEvent(new Event('submit'));
    await fixture.whenStable();
  }

  it('marks empty fields as invalid and does not call the API', async () => {
    await submit();
    expect(element.querySelector('#username')!.getAttribute('aria-invalid')).toBe('true');
    expect(element.textContent).toContain('Enter your username.');
    http.expectNone('/api/v1/auth/login');
  });

  it("signs in and opens the home page for the user's role", async () => {
    type('#username', ' claimant.mei ');
    type('#password', 'secret');
    await submit();

    const request = http.expectOne('/api/v1/auth/login');
    expect(request.request.body).toEqual({ username: 'claimant.mei', password: 'secret' });
    request.flush({
      accessToken: 't',
      tokenType: 'Bearer',
      expiresInSeconds: 3600,
      user: CLAIMANT,
    });

    expect(navigateByUrl).toHaveBeenCalledWith('/claimant');
  });

  it('returns to the page the user asked for, but never to another site', async () => {
    fixture.componentRef.setInput('returnUrl', '//evil.example');
    type('#username', 'claimant.mei');
    type('#password', 'secret');
    await submit();
    http
      .expectOne('/api/v1/auth/login')
      .flush({ accessToken: 't', tokenType: 'Bearer', expiresInSeconds: 3600, user: CLAIMANT });
    expect(navigateByUrl).toHaveBeenCalledWith('/claimant');
  });

  it('says plainly when the credentials are wrong', async () => {
    type('#username', 'claimant.mei');
    type('#password', 'wrong');
    await submit();
    http
      .expectOne('/api/v1/auth/login')
      .flush(
        {
          status: 401,
          title: 'Unauthorized',
          code: 'INVALID_CREDENTIALS',
          detail: 'Invalid username or password',
        },
        { status: 401, statusText: 'Unauthorized' },
      );
    await fixture.whenStable();

    expect(element.querySelector('[role="alert"]')?.textContent).toContain(
      'The username or password is not correct.',
    );
    expect(navigateByUrl).not.toHaveBeenCalled();
  });
});
