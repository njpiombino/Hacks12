import { DatePipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { Api } from '../../core/api';
import { addDays, startOfDay } from '../../core/dates';
import { Connections, Meeting, Note } from '../../core/models';
import { Session } from '../../core/session';
import { Avatar } from '../../shared/avatar';
import { MeetingDialog } from '../../shared/meeting-dialog';

@Component({
  selector: 'app-home',
  imports: [RouterLink, DatePipe, Avatar, MeetingDialog],
  templateUrl: './home.html',
})
export class Home {
  private readonly api = inject(Api);
  protected readonly session = inject(Session);

  protected readonly connections = signal<Connections | null>(null);
  protected readonly upcoming = signal<Meeting[] | null>(null);
  protected readonly invitations = signal<Meeting[]>([]);
  protected readonly notes = signal<Note[] | null>(null);
  protected readonly editing = signal<Meeting | null>(null);
  protected readonly creating = signal(false);

  protected readonly greeting = computed(() => {
    const h = new Date().getHours();
    const first = this.session.me()?.name?.split(' ')[0];
    const part = h < 12 ? 'Good morning' : h < 18 ? 'Good afternoon' : 'Good evening';
    return first ? `${part}, ${first}.` : `${part}.`;
  });

  /** How filled-out the profile is, for the "finish your profile" nudge. */
  protected readonly completeness = computed(() => {
    const me = this.session.me();
    const checks = me ? [!!me.headline, !!me.bio, !!me.location, me.interests.length > 0, !!me.portfolioUrl] : [];
    const done = checks.filter(Boolean).length;
    const total = checks.length || 5;
    return { done, total, pct: Math.round((done / total) * 100) };
  });

  protected readonly today = new Date();

  constructor() {
    this.refresh();
  }

  protected toHref(url: string): string {
    return /^https?:\/\//i.test(url) ? url : `https://${url}`;
  }

  protected refresh() {
    const from = new Date();
    this.api.meetings(from, addDays(startOfDay(from), 15)).subscribe((m) => this.upcoming.set(m));
    this.api.meetingInvitations().subscribe((m) => this.invitations.set(m));
    this.api.connections().subscribe((c) => this.connections.set(c));
    this.api.recentNotes().subscribe((n) => this.notes.set(n));
  }

  protected accept(id: number) {
    this.api.acceptConnection(id).subscribe((c) => this.connections.set(c));
  }

  protected decline(id: number) {
    this.api.removeConnection(id).subscribe((c) => this.connections.set(c));
  }

  protected acceptMeeting(m: Meeting) {
    this.api.acceptMeeting(m.id).subscribe(() => this.refresh());
  }

  protected declineMeeting(m: Meeting) {
    if (!confirm(`Decline "${m.title}"? It will be cancelled for you and ${m.with?.name ?? 'the organizer'}.`)) return;
    this.api.declineMeeting(m.id).subscribe(() => this.refresh());
  }

  protected onSaved() {
    this.editing.set(null);
    this.creating.set(false);
    this.refresh();
  }
}
