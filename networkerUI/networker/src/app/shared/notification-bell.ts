import { DatePipe } from '@angular/common';
import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Router } from '@angular/router';
import { fromEvent, interval, merge, startWith } from 'rxjs';

import { Api } from '../core/api';
import { AppNotification } from '../core/models';
import { Avatar } from './avatar';

/** How often to check for new notifications while the app is open. */
const POLL_MS = 30_000;

/** Header bell that lists what the other people in your meetings have changed. */
@Component({
  selector: 'app-notification-bell',
  imports: [DatePipe, Avatar],
  templateUrl: './notification-bell.html',
})
export class NotificationBell {
  private readonly api = inject(Api);
  private readonly router = inject(Router);

  protected readonly items = signal<AppNotification[]>([]);
  protected readonly open = signal(false);
  protected readonly unread = computed(() => this.items().filter((n) => !n.read).length);

  constructor() {
    merge(interval(POLL_MS), fromEvent(window, 'focus'))
      .pipe(startWith(null), takeUntilDestroyed(inject(DestroyRef)))
      .subscribe(() => this.load());
  }

  private load() {
    this.api.notifications().subscribe((n) => this.items.set(n));
  }

  protected toggle() {
    const opening = !this.open();
    this.open.set(opening);
    if (opening && this.unread()) {
      // Mark them read on the server, but keep the highlight until the panel closes so you can see what's new.
      this.api.markNotificationsRead().subscribe();
    } else if (!opening) {
      this.items.update((list) => list.map((n) => ({ ...n, read: true })));
    }
  }

  protected close() {
    if (this.open()) this.toggle();
  }

  protected go(n: AppNotification) {
    this.close();
    if (n.type !== 'CANCELLED' && n.type !== 'DECLINED') {
      this.router.navigate(['/calendar']);
    }
  }

  /** The headline, e.g. "Carol changed the time and place of Lunch". */
  protected headline(n: AppNotification): string {
    const who = n.actor.name ?? 'Someone';
    switch (n.type) {
      case 'INVITED':
        return `${who} invited you to ${n.title}`;
      case 'ACCEPTED':
        return `${who} accepted ${n.title}`;
      case 'DECLINED':
        return `${who} declined ${n.title}, so it's been cancelled`;
      case 'CANCELLED':
        return `${who} cancelled ${n.title}`;
      case 'UPDATED': {
        const parts = n.changes.map((c) => ({ TIME: 'time', PLACE: 'place', TITLE: 'title' })[c]);
        const list = parts.length > 1 ? `${parts.slice(0, -1).join(', ')} and ${parts[parts.length - 1]}` : parts[0];
        return `${who} changed the ${list} of ${n.previousTitle ?? n.title}`;
      }
    }
  }
}
