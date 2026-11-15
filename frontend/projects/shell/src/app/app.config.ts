import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { TitleStrategy, provideRouter, withComponentInputBinding } from '@angular/router';
import { authInterceptor } from '@claims/shared';
import { routes } from './app.routes';
import { PageTitleStrategy } from './page-title.strategy';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    // Route parameters arrive as component inputs, in the shell and in the micro-frontends' routes alike.
    provideRouter(routes, withComponentInputBinding()),
    provideHttpClient(withInterceptors([authInterceptor])),
    { provide: TitleStrategy, useClass: PageTitleStrategy },
  ],
};
