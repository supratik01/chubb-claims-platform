import { TestBed } from '@angular/core/testing';
import { anInformationRequest } from '@claims/shared/testing';
import { InformationAnswer, InformationRequestsComponent } from './information-requests.component';

describe('InformationRequestsComponent', () => {
  async function render(canRespond: boolean, requests = [anInformationRequest()]) {
    const fixture = TestBed.createComponent(InformationRequestsComponent);
    fixture.componentRef.setInput('requests', requests);
    fixture.componentRef.setInput('canRespond', canRespond);
    const answers: InformationAnswer[] = [];
    fixture.componentInstance.respond.subscribe((answer) => answers.push(answer));
    await fixture.whenStable();
    return { fixture, element: fixture.nativeElement as HTMLElement, answers };
  }

  it('offers a labelled answer box for an open question when the viewer may answer', async () => {
    const { element } = await render(true);
    const textarea = element.querySelector('textarea')!;
    expect(element.querySelector(`label[for="${textarea.id}"]`)?.textContent).toContain(
      'Your answer',
    );
  });

  it('shows staff that the question is still waiting, without an answer box', async () => {
    const { element } = await render(false);
    expect(element.querySelector('textarea')).toBeNull();
    expect(element.textContent).toContain("Awaiting the claimant's answer");
  });

  it('refuses a blank answer and says why', async () => {
    const { fixture, element, answers } = await render(true);
    element.querySelector('textarea')!.value = '   ';
    element.querySelector('textarea')!.dispatchEvent(new Event('input'));
    element.querySelector('form')!.dispatchEvent(new Event('submit'));
    await fixture.whenStable();

    expect(answers).toEqual([]);
    expect(element.querySelector('textarea')!.getAttribute('aria-invalid')).toBe('true');
    expect(element.textContent).toContain('Please enter an answer.');
  });

  it('emits the trimmed answer for the open question', async () => {
    const { fixture, element, answers } = await render(true);
    element.querySelector('textarea')!.value = '  Serviced in March.  ';
    element.querySelector('textarea')!.dispatchEvent(new Event('input'));
    element.querySelector('form')!.dispatchEvent(new Event('submit'));
    await fixture.whenStable();

    expect(answers).toEqual([{ requestId: 'request-1', response: 'Serviced in March.' }]);
  });

  it('shows an answered question with its answer', async () => {
    const { element } = await render(true, [anInformationRequest({ response: 'In March.' })]);
    expect(element.textContent).toContain('Answer: In March.');
    expect(element.querySelector('textarea')).toBeNull();
  });
});
