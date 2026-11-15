import { Component, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { RequestState } from '../state/request-state';
import { PageStateComponent } from './page-state.component';

@Component({
  imports: [PageStateComponent],
  template: `
    <cc-page-state
      [state]="state()"
      [empty]="empty()"
      emptyMessage="No claims yet."
      (retry)="retries = retries + 1"
    >
      <p class="content">The list</p>
    </cc-page-state>
  `,
})
class HostComponent {
  readonly state = signal<RequestState<string[]>>({ status: 'loading' });
  readonly empty = signal(false);
  retries = 0;
}

describe('PageStateComponent', () => {
  async function render(state: RequestState<string[]>, empty = false) {
    const fixture = TestBed.createComponent(HostComponent);
    fixture.componentInstance.state.set(state);
    fixture.componentInstance.empty.set(empty);
    await fixture.whenStable();
    return { fixture, element: fixture.nativeElement as HTMLElement };
  }

  it('announces loading to assistive technology', async () => {
    const { element } = await render({ status: 'loading' });
    expect(element.querySelector('[role="status"]')?.textContent).toContain('Loading');
    expect(element.querySelector('.content')).toBeNull();
  });

  it('shows the error as an alert with a retry button that asks the page to load again', async () => {
    const { fixture, element } = await render({
      status: 'error',
      error: { status: 0, code: null, message: 'Service unreachable', fieldErrors: [] },
    });
    expect(element.querySelector('[role="alert"]')?.textContent).toContain('Service unreachable');
    element.querySelector('button')!.click();
    expect(fixture.componentInstance.retries).toBe(1);
  });

  it('shows the empty message instead of the content when nothing was found', async () => {
    const { element } = await render({ status: 'loaded', data: [] }, true);
    expect(element.textContent).toContain('No claims yet.');
    expect(element.querySelector('.content')).toBeNull();
  });

  it('shows the projected content once data has loaded', async () => {
    const { element } = await render({ status: 'loaded', data: ['a'] });
    expect(element.querySelector('.content')?.textContent).toBe('The list');
  });
});
