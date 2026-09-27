import { DatePipe } from '@angular/common';
import { Component, computed, effect, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { Api } from '../../core/api';
import { addDays, monthGrid, sameDay, startOfDay } from '../../core/dates';
import { Meeting } from '../../core/models';
import { Avatar } from '../../shared/avatar';
import { MeetingDialog } from '../../shared/meeting-dialog';

@Component({
  selector: 'app-calendar',
  imports: [DatePipe, RouterLink, Avatar, MeetingDialog],
  templateUrl: './calendar.html',
})
export class Calendar {
  private readonly api = inject(Api);

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

  /** Solid for confirmed meetings, dashed outline while the invitee hasn't answered. */
  protected chipClass(m: Meeting) {
    if (m.inviteStatus === 'PENDING') {
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
