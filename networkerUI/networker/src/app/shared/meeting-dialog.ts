import { Component, OnInit, computed, inject, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { Api } from '../core/api';
import { fromInputs, toDateInput, toTimeInput } from '../core/dates';
import { awaitingReplies, declineCancels, needsMyAnswer } from '../core/meetings';
import { InviteStatus, Meeting, PersonSummary } from '../core/models';
import { Avatar } from './avatar';

/** Create or edit a meeting, or answer an invitation. Rendered as a modal sheet over the page. */
@Component({
  selector: 'app-meeting-dialog',
  imports: [FormsModule, Avatar],
  templateUrl: './meeting-dialog.html',
})
export class MeetingDialog implements OnInit {
  private readonly api = inject(Api);

  /** Existing meeting to edit; omit to create a new one. */
  readonly meeting = input<Meeting | null>(null);
  readonly day = input<Date | null>(null);
  /** Pre-selects someone to invite (e.g. from a person's page). */
  readonly withPerson = input<PersonSummary | null>(null);

  readonly saved = output<void>();
  readonly closed = output<void>();

  private readonly connections = signal<PersonSummary[]>([]);
  protected readonly invited = signal<string[]>([]);
  protected readonly saving = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly needsMyAnswer = needsMyAnswer;
  protected readonly awaitingReplies = awaitingReplies;

  /** Who can be invited: your connections, plus anyone already on the meeting. */
  protected readonly people = computed(() => {
    const list = [...this.connections()];
    for (const a of this.meeting()?.attendees ?? []) {
      if (!list.some((p) => p.id === a.person.id)) list.push(a.person);
    }
    return list;
  });

  protected form = {
    title: '',
    date: '',
    start: '10:00',
    end: '10:30',
    location: '',
    description: '',
  };

  ngOnInit() {
    const m = this.meeting();
    if (m) {
      const start = new Date(m.startsAt);
      const end = new Date(m.endsAt);
      this.form = {
        title: m.title,
        date: toDateInput(start),
        start: toTimeInput(start),
        end: toTimeInput(end),
        location: m.location ?? '',
        description: m.description ?? '',
      };
      this.invited.set(m.attendees.map((a) => a.person.id));
    } else {
      this.form.date = toDateInput(this.day() ?? new Date());
      const who = this.withPerson();
      if (who) {
        this.invited.set([who.id]);
        if (who.name) this.form.title = `Coffee with ${who.name.split(' ')[0]}`;
      }
    }

    this.api.connections().subscribe((c) =>
      this.connections.set(c.connected.map((i) => ({ id: i.person.id, name: i.person.name, pictureUrl: i.person.pictureUrl }))),
    );
  }

  protected isInvited(id: string) {
    return this.invited().includes(id);
  }

  protected toggle(id: string) {
    this.invited.update((ids) => (ids.includes(id) ? ids.filter((i) => i !== id) : [...ids, id]));
  }

  /** Current answer from someone already on the meeting, if they are. */
  protected statusOf(id: string): InviteStatus | null {
    return this.meeting()?.attendees.find((a) => a.person.id === id)?.status ?? null;
  }

  protected save() {
    const startsAt = fromInputs(this.form.date, this.form.start);
    const endsAt = fromInputs(this.form.date, this.form.end);
    if (endsAt <= startsAt) {
      this.error.set('The meeting needs to end after it starts.');
      return;
    }
    const request = {
      title: this.form.title.trim(),
      startsAt: startsAt.toISOString(),
      endsAt: endsAt.toISOString(),
      attendeeIds: this.invited(),
      location: this.form.location.trim() || null,
      description: this.form.description.trim() || null,
    };
    const m = this.meeting();
    this.saving.set(true);
    this.error.set(null);
    (m ? this.api.updateMeeting(m.id, request) : this.api.createMeeting(request)).subscribe({
      next: () => this.saved.emit(),
      error: (e) => {
        this.saving.set(false);
        this.error.set(e.error?.detail ?? 'Something went wrong saving that meeting.');
      },
    });
  }

  protected remove() {
    const m = this.meeting();
    if (!m) return;
    const warning = m.attendees.length ? ' Everyone invited will be told it was cancelled.' : '';
    if (!confirm(`Delete "${m.title}"?${warning}`)) return;
    this.api.deleteMeeting(m.id).subscribe(() => this.saved.emit());
  }

  protected accept() {
    const m = this.meeting();
    if (!m) return;
    this.saving.set(true);
    this.api.acceptMeeting(m.id).subscribe({
      next: () => this.saved.emit(),
      error: (e) => {
        this.saving.set(false);
        this.error.set(e.error?.detail ?? 'Something went wrong accepting that meeting.');
      },
    });
  }

  protected decline() {
    const m = this.meeting();
    if (!m) return;
    const organizer = m.organizer.name ?? 'the organizer';
    const outcome = declineCancels(m)
      ? `It will be cancelled for you and ${organizer}.`
      : `You'll be taken off it, and ${organizer} will be told.`;
    if (!confirm(`Decline "${m.title}"? ${outcome}`)) return;
    this.saving.set(true);
    this.api.declineMeeting(m.id).subscribe({
      next: () => this.saved.emit(),
      error: (e) => {
        this.saving.set(false);
        this.error.set(e.error?.detail ?? 'Something went wrong declining that meeting.');
      },
    });
  }
}
