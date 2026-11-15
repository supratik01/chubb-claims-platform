import { Subject, of, throwError } from 'rxjs';
import { HttpErrorResponse } from '@angular/common/http';
import { CommandState, QueryState } from './request-state';

describe('QueryState', () => {
  it('moves from loading to loaded with the fetched data', () => {
    const response = new Subject<string>();
    const query = new QueryState(() => response);

    query.load();
    expect(query.state().status).toBe('loading');

    response.next('claims');
    expect(query.state()).toEqual({ status: 'loaded', data: 'claims' });
    expect(query.loadedAt()).toBeInstanceOf(Date);
  });

  it('shows an error when the first load fails', () => {
    const query = new QueryState(() => throwError(() => new HttpErrorResponse({ status: 0 })));
    query.load();
    expect(query.state().status).toBe('error');
  });

  it('keeps the current data when a background refresh fails', () => {
    let fail = false;
    const query = new QueryState(() =>
      fail ? throwError(() => new HttpErrorResponse({ status: 503 })) : of('first'),
    );
    query.load();
    fail = true;
    query.load({ background: true });
    expect(query.state()).toEqual({ status: 'loaded', data: 'first' });
  });

  it('does not show a loading state during a background refresh', () => {
    const response = new Subject<string>();
    let calls = 0;
    const query = new QueryState(() => (calls++ === 0 ? of('first') : response));
    query.load();
    query.load({ background: true });
    expect(query.state()).toEqual({ status: 'loaded', data: 'first' });
  });
});

describe('CommandState', () => {
  it('ignores a second command while one is running, which prevents double submits', () => {
    const command = new CommandState();
    const first = new Subject<string>();
    let secondStarted = false;

    command.run(first, () => undefined);
    command.run(of('second').pipe(), () => (secondStarted = true));

    expect(command.pending()).toBe(true);
    expect(secondStarted).toBe(false);
  });

  it('records the error of a failed command and clears it on the next attempt', () => {
    const command = new CommandState();
    command.run(
      throwError(() => new HttpErrorResponse({ status: 0 })),
      () => undefined,
    );
    expect(command.error()?.status).toBe(0);

    command.run(of('ok'), () => undefined);
    expect(command.error()).toBeNull();
    expect(command.pending()).toBe(false);
  });
});
