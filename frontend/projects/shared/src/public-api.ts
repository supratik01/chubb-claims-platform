/*
 * Public surface of the shared library. The shell and both micro-frontends import only from here.
 * It holds no feature state: only the session, the API client, state helpers, presentational components and the theme.
 */
export * from './lib/api/models';
export * from './lib/api/api-error';
export * from './lib/api/claims-api.client';
export * from './lib/session/session.store';
export * from './lib/session/home-route';
export * from './lib/session/guards';
export * from './lib/session/auth.interceptor';
export * from './lib/state/request-state';
export * from './lib/state/refresh';
export * from './lib/ui/labels';
export * from './lib/ui/status-badge.component';
export * from './lib/ui/error-alert.component';
export * from './lib/ui/page-state.component';
export * from './lib/ui/status-timeline.component';
export * from './lib/ui/information-requests.component';
export * from './lib/ui/claim-facts.component';
