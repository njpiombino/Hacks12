import { Injectable, inject, signal } from '@angular/core';
import { AuthService } from '@auth0/auth0-angular';
import { filter, switchMap, take } from 'rxjs';

import { Api } from './api';
import { Person } from './models';

/** Holds the signed-in user's profile, creating it from their Auth0 details on first login. */
@Injectable({ providedIn: 'root' })
export class Session {
  private readonly auth = inject(AuthService);
  private readonly api = inject(Api);

  readonly me = signal<Person | null>(null);
  readonly error = signal<string | null>(null);

  load() {
    this.auth.user$
      .pipe(
        filter((user) => !!user),
        take(1),
        switchMap((user) =>
          this.api.syncMe({ name: user!.name, email: user!.email, pictureUrl: user!.picture }),
        ),
      )
      .subscribe({
        next: (me) => this.me.set(me),
        error: () => this.error.set("We couldn't reach the Networker server. Is the API running?"),
      });
  }

  logout() {
    this.auth.logout({ logoutParams: { returnTo: window.location.origin } });
  }
}
