import { Component, inject, signal } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

import { Session } from './core/session';
import { Avatar } from './shared/avatar';

@Component({
  selector: 'app-shell',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, Avatar],
  templateUrl: './shell.html',
})
export class Shell {
  protected readonly session = inject(Session);
  protected readonly menuOpen = signal(false);

  protected readonly nav = [
    { label: 'Home', path: '/', exact: true },
    { label: 'People', path: '/people', exact: false },
    { label: 'Calendar', path: '/calendar', exact: false },
  ];

  constructor() {
    this.session.load();
  }
}
