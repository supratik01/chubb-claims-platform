import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { App, pathOf } from './app';

@Component({ template: '<button type="button">Tab</button>' })
class PageComponent {}

describe('App', () => {
  async function render() {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideRouter([
          { path: 'one', component: PageComponent },
          { path: 'two', component: PageComponent },
        ]),
      ],
    });
    const fixture = TestBed.createComponent(App);
    document.body.appendChild(fixture.nativeElement);
    const router = TestBed.inject(Router);
    await router.navigateByUrl('/one');
    await fixture.whenStable();
    return { fixture, router, main: (fixture.nativeElement as HTMLElement).querySelector('main')! };
  }

  it('moves focus to the main region when the page changes', async () => {
    const { router, main } = await render();
    await router.navigateByUrl('/two');
    expect(document.activeElement).toBe(main);
  });

  it('leaves focus where it is when only the query string changes', async () => {
    const { fixture, router } = await render();
    const button = (fixture.nativeElement as HTMLElement).querySelector(
      'main button',
    ) as HTMLButtonElement;
    button.focus();
    await router.navigateByUrl('/one?view=mine');
    expect(document.activeElement).toBe(button);
  });

  it('compares paths without their query string or fragment', () => {
    expect(pathOf('/staff/queue?view=mine#top')).toBe('/staff/queue');
  });
});
