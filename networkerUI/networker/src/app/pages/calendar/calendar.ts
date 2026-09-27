import { DatePipe } from '@angular/common';
import { Component, computed, effect, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { Api } from '../../core/api';
import { addDays, monthGrid, sameDay, startOfDay } from '../../core/dates';
import { awaitingReplies, namesOf, needsMyAnswer, otherPeople } from '../../core/meetings';
import { Meeting } from '../../core/models';
import { Session } from '../../core/session';
import { MeetingDialog } from '../../shared/meeting-dialog';
import { MeetingStatus } from '../../shared/meeting-status';
import { PeopleStack } from '../../shared/people-stack';

@Component({
  selector: 'app-calendar',
  imports: [DatePipe, MeetingDialog, MeetingStatus, PeopleStack],
  templateUrl: './calendar.html',
})
export class Calendar {
  private readonly api = inject(Api);
  private readonly session = inject(Session);

  protected readonly namesOf = namesOf;

  protected readonly weekdays = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];
  protected readonly today = startOfDay(new Date());
  protected readonly month = signal(new Date(this.today.getFullYear(), this.today.getMonth(), 1));
  protected readonly selected = signal(this.today);
  protected readonly meetings = signal<Meeting[]>([]);

  protected readonly creatingOn = signal<Date | null>(null);
  protected readonly editing = signal<Meeting | null>(null);

  protected readonly days = computed(() => monthGrid(this.month()));

  protected readonly byDay = computed(() => {
    const map = new Map<string, Meeting[]>();
    for (const m of this.meetings()) {
      const key = startOfDay(new Date(m.startsAt)).toDateString();
      map.set(key, [...(map.get(key) ?? []), m]);
    }
    return map;
  });

  protected readonly selectedMeetings = computed(() => this.byDay().get(this.selected().toDateString()) ?? []);

  constructor() {
    effect(() => this.load(this.days()));
  }

  private load(days: Date[] = this.days()) {
    this.api.meetings(days[0], addDays(days[days.length - 1], 1)).subscribe((m) => this.meetings.set(m));
  }

  protected meetingsOn(day: Date) {
    return this.byDay().get(day.toDateString()) ?? [];
  }

  protected shift(months: number) {
    const m = this.month();
    this.month.set(new Date(m.getFullYear(), m.getMonth() + months, 1));
  }

  protected goToday() {
    this.month.set(new Date(this.today.getFullYear(), this.today.getMonth(), 1));
    this.selected.set(this.today);
  }

  protected pick(day: Date) {
    this.selected.set(day);
    if (day.getMonth() !== this.month().getMonth()) {
      this.month.set(new Date(day.getFullYear(), day.getMonth(), 1));
    }
  }

  protected isToday(day: Date) {
    return sameDay(day, this.today);
  }

  protected isSelected(day: Date) {
    return sameDay(day, this.selected());
  }

  protected inMonth(day: Date) {
    return day.getMonth() === this.month().getMonth();
  }

  protected others(m: Meeting) {
    return otherPeople(m, this.session.me()?.id);
  }

  /** Solid for confirmed meetings, dashed outline while an answer is outstanding. */
  protected chipClass(m: Meeting) {
    if (awaitingReplies(m) || needsMyAnswer(m)) {
      return m.mine ? 'border border-dashed border-forest text-forest' : 'border border-dashed border-clay text-[#8a3f22]';
    }
    return m.mine ? 'bg-forest text-[#fdfbf5]' : 'bg-clay-soft text-[#8a3f22]';
  }

  protected onSaved() {
    this.creatingOn.set(null);
    this.editing.set(null);
    this.load();
  }
}
