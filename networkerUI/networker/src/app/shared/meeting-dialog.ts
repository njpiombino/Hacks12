import { Component, OnInit, inject, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { Api } from '../core/api';
import { fromInputs, toDateInput, toTimeInput } from '../core/dates';
import { Meeting, PersonSummary } from '../core/models';

/** Create or edit a meeting. Rendered as a modal sheet over the page. */
@Component({
  selector: 'app-meeting-dialog',
  imports: [FormsModule],
  templateUrl: './meeting-dialog.html',
})
export class MeetingDialog implements OnInit {
  private readonly api = inject(Api);

  /** Existing meeting to edit; omit to create a new one. */
  readonly meeting = input<Meeting | null>(null);
  readonly day = input<Date | null>(null);
  /** Pre-selects who the meeting is with (e.g. from a person's page). */
  readonly withPerson = input<PersonSummary | null>(null);

  readonly saved = output<void>();
  readonly closed = output<void>();

  protected readonly people = signal<PersonSummary[]>([]);
  protected readonly saving = signal(false);
  protected readonly error = signal<string | null>(null);

  protected form = {
    title: '',
    date: '',
    start: '10:00',
    end: '10:30',
    attendeeId: null as string | null,
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
        attendeeId: m.with?.id ?? null,
        location: m.location ?? '',
        description: m.description ?? '',
      };
    } else {
      this.form.date = toDateInput(this.day() ?? new Date());
      this.form.attendeeId = this.withPerson()?.id ?? null;
      const who = this.withPerson()?.name;
      if (who) this.form.title = `Coffee with ${who.split(' ')[0]}`;
    }

    this.api.connections().subscribe((c) =>
      this.people.set(c.connected.map((i) => ({ id: i.person.id, name: i.person.name, pictureUrl: i.person.pictureUrl }))),
    );
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
      attendeeId: this.form.attendeeId,
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
    if (!m || !confirm(`Delete "${m.title}"?`)) return;
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
    const organizer = m?.with?.name ?? 'the organizer';
    if (!m || !confirm(`Decline "${m.title}"? It will be cancelled for you and ${organizer}.`)) return;
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
