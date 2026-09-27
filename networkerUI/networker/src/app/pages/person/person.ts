import { DatePipe } from '@angular/common';
import { Component, computed, effect, inject, input, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { Api } from '../../core/api';
import { Meeting, Note, Person } from '../../core/models';
import { Avatar } from '../../shared/avatar';
import { MeetingDialog } from '../../shared/meeting-dialog';
import { MeetingStatus } from '../../shared/meeting-status';

@Component({
  selector: 'app-person',
  imports: [RouterLink, FormsModule, DatePipe, Avatar, MeetingDialog, MeetingStatus],
  templateUrl: './person.html',
})
export class PersonPage {
  private readonly api = inject(Api);

  /** Route parameter. */
  readonly id = input.required<string>();

  protected readonly person = signal<Person | null>(null);
  protected readonly notes = signal<Note[]>([]);
  protected readonly meetings = signal<Meeting[]>([]);
  protected readonly notFound = signal(false);

  protected draft = '';
  protected readonly editingNoteId = signal<number | null>(null);
  protected editDraft = '';

  protected readonly dialogOpen = signal(false);
  protected readonly editingMeeting = signal<Meeting | null>(null);

  protected readonly now = Date.now();
  protected readonly upcoming = computed(() => this.meetings().filter((m) => Date.parse(m.endsAt) >= this.now).reverse());
  protected readonly past = computed(() => this.meetings().filter((m) => Date.parse(m.endsAt) < this.now));

  constructor() {
    effect(() => this.load(this.id()));
  }

  private load(id: string) {
    this.person.set(null);
    this.notFound.set(false);
    this.api.person(id).subscribe({
      next: (p) => this.person.set(p),
      error: () => this.notFound.set(true),
    });
    this.api.notesAbout(id).subscribe((n) => this.notes.set(n));
    this.loadMeetings();
  }

  private loadMeetings() {
    this.api.meetingsWith(this.id()).subscribe((m) => this.meetings.set(m));
  }

  protected connect() {
    this.api.connect(this.id()).subscribe(() => this.refreshPerson());
  }

  protected accept(connectionId: number) {
    this.api.acceptConnection(connectionId).subscribe(() => this.refreshPerson());
  }

  protected disconnect(connectionId: number, verb: string) {
    if (verb === 'Remove' && !confirm(`Remove ${this.person()?.name} from your connections? Your notes stay put.`)) return;
    this.api.removeConnection(connectionId).subscribe(() => this.refreshPerson());
  }

  private refreshPerson() {
    this.api.person(this.id()).subscribe((p) => this.person.set(p));
  }

  protected addNote() {
    const body = this.draft.trim();
    if (!body) return;
    this.api.addNote(this.id(), body).subscribe((n) => {
      this.notes.update((list) => [n, ...list]);
      this.draft = '';
    });
  }

  protected startEdit(note: Note) {
    this.editingNoteId.set(note.id);
    this.editDraft = note.body;
  }

  protected saveEdit(note: Note) {
    const body = this.editDraft.trim();
    if (!body) return;
    this.api.updateNote(note.id, body).subscribe((updated) => {
      this.notes.update((list) => list.map((n) => (n.id === updated.id ? updated : n)));
      this.editingNoteId.set(null);
    });
  }

  protected deleteNote(note: Note) {
    if (!confirm('Delete this note?')) return;
    this.api.deleteNote(note.id).subscribe(() => this.notes.update((list) => list.filter((n) => n.id !== note.id)));
  }

  protected onMeetingSaved() {
    this.dialogOpen.set(false);
    this.editingMeeting.set(null);
    this.loadMeetings();
  }

  protected toHref(url: string): string {
    return /^https?:\/\//i.test(url) ? url : `https://${url}`;
  }
}
