import { HttpErrorResponse } from '@angular/common/http';
import { ErrorCode, Problem } from './models';

/** What the UI needs to know about a failed call, already turned into words a user can act on. */
export interface ApiError {
  readonly status: number;
  readonly code: ErrorCode | null;
  readonly message: string;
  readonly fieldErrors: readonly { field: string; message: string }[];
}

const MESSAGES: Partial<Record<ErrorCode, string>> = {
  UNAUTHENTICATED: 'Your session has ended. Please sign in again.',
  ACCESS_DENIED: 'You do not have permission to do that.',
  NOT_FOUND: 'We could not find that claim, or it is not available to you.',
  CONCURRENT_MODIFICATION:
    'Someone else changed this claim a moment ago. It has been reloaded; please try again.',
  VALIDATION_FAILED: 'Some details need correcting.',
  INTERNAL_ERROR: 'Something went wrong on our side. Please try again in a moment.',
};

/**
 * Converts any error from an API call into an ApiError. The backend always answers with an RFC 9457 problem
 * document; codes with a fixed meaning get a fixed message, and business-rule and workflow refusals use the
 * server's own explanation, which is written for users.
 */
export function toApiError(error: unknown): ApiError {
  if (!(error instanceof HttpErrorResponse)) {
    return {
      status: -1,
      code: null,
      message: 'Something went wrong. Please try again.',
      fieldErrors: [],
    };
  }
  if (error.status === 0) {
    return {
      status: 0,
      code: null,
      message: 'The claims service could not be reached. Check your connection and try again.',
      fieldErrors: [],
    };
  }
  const problem = isProblem(error.error) ? error.error : null;
  const code = problem?.code ?? null;
  const message =
    (code && MESSAGES[code]) ??
    problem?.detail ??
    problem?.title ??
    'Something went wrong. Please try again.';
  return { status: error.status, code, message, fieldErrors: problem?.errors ?? [] };
}

function isProblem(body: unknown): body is Problem {
  return typeof body === 'object' && body !== null && 'code' in body && 'status' in body;
}
