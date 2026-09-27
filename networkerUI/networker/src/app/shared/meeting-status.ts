import { Component, computed, input } from '@angular/core';

import { needsMyAnswer } from '../core/meetings';
import { Meeting } from '../core/models';

/** A small chip when a meeting is still waiting on answers: yours, or your invitees'. */
@Component({
  selector: 'app-meeting-status',
  template: `
    @if (label(); as text) {
      <span class="chip" [class.chip-clay]="mineToAnswer()">{{ text }}</span>
    }
  `,
})
export class MeetingStatus {
  readonly meeting = input.required<Meeting>();

  protected readonly mineToAnswer = computed(() => needsMyAnswer(this.meeting()));

  protected readonly label = computed(() => {
    const m = this.meeting();
    if (needsMyAnswer(m)) return 'Needs your answer';
    if (!m.mine) return null;
    const waiting = m.attendees.filter((a) => a.status === 'PENDING').length;
    if (!waiting) return null;
    return m.attendees.length === 1 ? 'Awaiting reply' : `Waiting on ${waiting} of ${m.attendees.length}`;
  });
}
