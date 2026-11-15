import { Assignment, ClaimListQuery } from '@claims/shared';

export type QueueView = 'unassigned' | 'mine' | 'all';

export interface QueueViewDefinition {
  readonly view: QueueView;
  readonly label: string;
  readonly emptyMessage: string;
  readonly query: ClaimListQuery;
}

/**
 * The three views of the work queue. "Mine" shows only open work, so an officer sees what still needs them;
 * both lists are oldest first, so the claim that has waited longest is at the top.
 */
export const QUEUE_VIEWS: readonly QueueViewDefinition[] = [
  {
    view: 'unassigned',
    label: 'Unassigned',
    emptyMessage: 'No new claims are waiting. The queue is clear.',
    query: { assignment: 'UNASSIGNED' satisfies Assignment, size: 100 },
  },
  {
    view: 'mine',
    label: 'My open claims',
    emptyMessage: 'You have no open claims. Pick one up from the unassigned queue.',
    query: {
      assignment: 'MINE',
      status: ['UNDER_REVIEW', 'INFO_REQUESTED', 'APPROVED'],
      size: 100,
    },
  },
  {
    view: 'all',
    label: 'All claims',
    emptyMessage: 'There are no claims yet.',
    query: { assignment: 'ALL', size: 100 },
  },
];

export function queueView(value: string | undefined): QueueViewDefinition {
  return QUEUE_VIEWS.find((definition) => definition.view === value) ?? QUEUE_VIEWS[0];
}
