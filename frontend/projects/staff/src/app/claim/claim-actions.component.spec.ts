import { ComponentFixture, TestBed } from '@angular/core/testing';
import { aClaim } from '@claims/shared/testing';
import { ApprovalInput, ClaimActionsComponent } from './claim-actions.component';

describe('ClaimActionsComponent', () => {
  let fixture: ComponentFixture<ClaimActionsComponent>;
  let element: HTMLElement;
  let approvals: ApprovalInput[];
  let rejections: string[];
  let questions: string[];

  async function render(
    claim = aClaim({ status: 'UNDER_REVIEW', claimedAmount: 4200, sumInsured: 80000 }),
  ) {
    fixture = TestBed.createComponent(ClaimActionsComponent);
    fixture.componentRef.setInput('claim', claim);
    fixture.componentRef.setInput('actions', ['REQUEST_INFORMATION', 'DECIDE']);
    approvals = [];
    rejections = [];
    questions = [];
    fixture.componentInstance.approve.subscribe((value) => approvals.push(value));
    fixture.componentInstance.reject.subscribe((value) => rejections.push(value));
    fixture.componentInstance.requestInformation.subscribe((value) => questions.push(value));
    await fixture.whenStable();
    element = fixture.nativeElement;
  }

  function set(selector: string, value: string, event = 'input'): void {
    const field = element.querySelector<HTMLInputElement>(selector)!;
    field.value = value;
    field.dispatchEvent(new Event(event));
  }

  function choose(outcome: 'APPROVED' | 'REJECTED'): void {
    const radio = element.querySelector<HTMLInputElement>(
      `input[type="radio"][value="${outcome}"]`,
    )!;
    radio.checked = true;
    radio.dispatchEvent(new Event('change'));
  }

  async function submitDecision(): Promise<void> {
    element.querySelectorAll('form')[1].dispatchEvent(new Event('submit'));
    await fixture.whenStable();
  }

  it('suggests the claimed amount as the approved amount', async () => {
    await render();
    expect(element.querySelector<HTMLInputElement>('#approvedAmount')!.value).toBe('4200');
  });

  it('suggests no more than the sum insured when more was claimed', async () => {
    await render(aClaim({ status: 'UNDER_REVIEW', claimedAmount: 90000, sumInsured: 80000 }));
    expect(element.querySelector<HTMLInputElement>('#approvedAmount')!.value).toBe('80000');
  });

  it('emits an approval with the amount and the optional note', async () => {
    await render();
    set('#approvedAmount', '3800');
    set('#reason', 'Less the excess.');
    await submitDecision();
    expect(approvals).toEqual([{ approvedAmount: 3800, note: 'Less the excess.' }]);
  });

  it('refuses to approve more than the sum insured', async () => {
    await render();
    set('#approvedAmount', '90000');
    await submitDecision();
    expect(approvals).toEqual([]);
    expect(element.textContent).toContain('cannot exceed the sum insured');
  });

  it('requires a reason to reject', async () => {
    await render();
    choose('REJECTED');
    await fixture.whenStable();
    await submitDecision();
    expect(rejections).toEqual([]);
    expect(element.querySelector('#reason')!.getAttribute('aria-invalid')).toBe('true');

    set('#reason', 'Wear and tear is excluded.');
    await submitDecision();
    expect(rejections).toEqual(['Wear and tear is excluded.']);
  });

  it('sends a question for the claimant and clears the box', async () => {
    await render();
    set('#question', 'When was the pipe last serviced?');
    element.querySelectorAll('form')[0].dispatchEvent(new Event('submit'));
    await fixture.whenStable();
    expect(questions).toEqual(['When was the pipe last serviced?']);
    expect(element.querySelector<HTMLTextAreaElement>('#question')!.value).toBe('');
  });

  it('offers only the actions it is given', async () => {
    await render();
    fixture.componentRef.setInput('actions', ['PICK_UP']);
    await fixture.whenStable();
    expect(element.querySelectorAll('form').length).toBe(0);
    expect(element.textContent).toContain('Pick up this claim');
  });
});
