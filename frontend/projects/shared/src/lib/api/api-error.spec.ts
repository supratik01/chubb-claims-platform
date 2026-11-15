import { HttpErrorResponse } from '@angular/common/http';
import { toApiError } from './api-error';

describe('toApiError', () => {
  it('explains that the service could not be reached when there is no response', () => {
    const error = toApiError(new HttpErrorResponse({ status: 0 }));
    expect(error.message).toContain('could not be reached');
  });

  it('uses the server explanation for a business rule refusal', () => {
    const error = toApiError(
      new HttpErrorResponse({
        status: 422,
        error: {
          status: 422,
          title: 'Unprocessable',
          code: 'BUSINESS_RULE_VIOLATED',
          detail: 'The incident date cannot be in the future',
        },
      }),
    );
    expect(error.code).toBe('BUSINESS_RULE_VIOLATED');
    expect(error.message).toBe('The incident date cannot be in the future');
  });

  it('replaces technical details with a fixed message for codes that have one', () => {
    const error = toApiError(
      new HttpErrorResponse({
        status: 404,
        error: {
          status: 404,
          title: 'Not Found',
          code: 'NOT_FOUND',
          detail: 'Claim 0b9d… was not found',
        },
      }),
    );
    expect(error.message).not.toContain('0b9d');
    expect(error.message).toContain('could not find that claim');
  });

  it('keeps the field errors of a validation failure', () => {
    const error = toApiError(
      new HttpErrorResponse({
        status: 400,
        error: {
          status: 400,
          title: 'Bad Request',
          code: 'VALIDATION_FAILED',
          errors: [{ field: 'description', message: 'size must be between 10 and 2000' }],
        },
      }),
    );
    expect(error.fieldErrors).toEqual([
      { field: 'description', message: 'size must be between 10 and 2000' },
    ]);
  });
});
