import { Component, effect, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { Api } from '../../core/api';
import { ProfileUpdate } from '../../core/models';
import { Session } from '../../core/session';
import { Avatar } from '../../shared/avatar';

@Component({
  selector: 'app-profile',
  imports: [FormsModule, RouterLink, Avatar],
  templateUrl: './profile.html',
})
export class Profile {
  private readonly api = inject(Api);
  protected readonly session = inject(Session);

  protected form: ProfileUpdate = { name: '', headline: '', location: '', bio: '', pictureUrl: '' };
  protected readonly loaded = signal(false);
  protected readonly saving = signal(false);
  protected readonly savedAt = signal<Date | null>(null);
  protected readonly error = signal<string | null>(null);

  constructor() {
    effect(() => {
      const me = this.session.me();
      if (me && !this.loaded()) {
        this.form = {
          name: me.name ?? '',
          headline: me.headline ?? '',
          location: me.location ?? '',
          bio: me.bio ?? '',
          pictureUrl: me.pictureUrl ?? '',
        };
        this.loaded.set(true);
      }
    });
  }

  protected save() {
    this.saving.set(true);
    this.error.set(null);
    this.api.updateMe(this.form).subscribe({
      next: (me) => {
        this.session.me.set(me);
        this.saving.set(false);
        this.savedAt.set(new Date());
      },
      error: (e) => {
        this.saving.set(false);
        this.error.set(e.error?.detail ?? "Couldn't save your profile.");
      },
    });
  }
}
