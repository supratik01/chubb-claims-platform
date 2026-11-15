import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  inject,
  input,
  signal,
} from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import {
  ApiError,
  ErrorAlertComponent,
  SessionStore,
  homeRouteFor,
  toApiError,
} from '@claims/shared';

interface DemoAccount {
  readonly username: string;
  readonly description: string;
}

@Component({
  selector: 'app-login-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, ErrorAlertComponent],
  templateUrl: './login.page.html',
  styleUrl: './login.page.scss',
})
export class LoginPage {
  private readonly session = inject(SessionStore);
  private readonly router = inject(Router);
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);

  /** Query parameters, bound by the router. */
  readonly returnUrl = input<string>();
  readonly reason = input<string>();

  protected readonly form = inject(FormBuilder).nonNullable.group({
    username: ['', Validators.required],
    password: ['', Validators.required],
  });
  protected readonly submitting = signal(false);
  protected readonly error = signal<ApiError | null>(null);
  protected readonly submitted = signal(false);

  protected readonly demoAccounts: readonly DemoAccount[] = [
    { username: 'claimant.mei', description: 'Claimant, Singapore policies' },
    { username: 'claimant.jack', description: 'Claimant, Australia policies' },
    { username: 'officer.sam', description: 'Claims officer' },
    { username: 'officer.priya', description: 'Claims officer' },
    { username: 'manager.kenji', description: 'Claims manager (sees exposure)' },
  ];

  protected invalid(field: 'username' | 'password'): boolean {
    return this.submitted() && this.form.controls[field].invalid;
  }

  protected useAccount(username: string): void {
    this.form.controls.username.setValue(username);
    this.host.nativeElement.querySelector<HTMLInputElement>('#password')?.focus();
  }

  protected signIn(): void {
    this.submitted.set(true);
    if (this.form.invalid) {
      this.host.nativeElement.querySelector<HTMLElement>('[aria-invalid="true"]')?.focus();
      return;
    }
    if (this.submitting()) return;
    this.submitting.set(true);
    this.error.set(null);
    const { username, password } = this.form.getRawValue();
    this.session.signIn(username.trim(), password).subscribe({
      next: (user) => {
        this.submitting.set(false);
        const target = this.safeReturnUrl() ?? homeRouteFor(user.role);
        void this.router.navigateByUrl(target);
      },
      error: (error: unknown) => {
        this.submitting.set(false);
        const apiError = toApiError(error);
        this.error.set(
          apiError.code === 'INVALID_CREDENTIALS'
            ? { ...apiError, message: 'The username or password is not correct.' }
            : apiError,
        );
      },
    });
  }

  /** Only same-application paths are followed, so the login page cannot be used to redirect elsewhere. */
  private safeReturnUrl(): string | null {
    const url = this.returnUrl();
    return url && url.startsWith('/') && !url.startsWith('//') ? url : null;
  }
}
