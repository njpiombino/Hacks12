import { Component, computed, input } from '@angular/core';

import { PersonSummary } from '../core/models';
import { Avatar } from './avatar';

/** Up to three overlapping avatars, then "+N". */
@Component({
  selector: 'app-people-stack',
  imports: [Avatar],
  template: `
    <span class="flex items-center">
      @for (p of shown(); track p.id; let first = $first) {
        <span class="flex rounded-full ring-2 ring-card" [class.-ml-2]="!first">
          <app-avatar [name]="p.name" [src]="p.pictureUrl" size="sm" />
        </span>
      }
      @if (extra()) {
        <span class="-ml-2 flex size-7 items-center justify-center rounded-full bg-sunken text-[0.65rem] font-semibold text-ink-2 ring-2 ring-card">
          +{{ extra() }}
        </span>
      }
    </span>
  `,
})
export class PeopleStack {
  readonly people = input.required<PersonSummary[]>();

  protected readonly shown = computed(() => this.people().slice(0, 3));
  protected readonly extra = computed(() => Math.max(0, this.people().length - 3));
}
