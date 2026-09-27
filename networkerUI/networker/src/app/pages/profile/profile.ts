import { Component, effect, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { Api } from '../../core/api';
import { ProfileUpdate } from '../../core/models';
import { Session } from '../../core/session';
import { Avatar } from '../../shared/avatar';

const MAX_PHOTO_SIDE = 480;

@Component({
  selector: 'app-profile',
  imports: [FormsModule, RouterLink, Avatar],
  templateUrl: './profile.html',
})
export class Profile {
  private readonly api = inject(Api);
  protected readonly session = inject(Session);

  protected form: ProfileUpdate = {
    name: '',
    headline: '',
    location: '',
    bio: '',
    pictureUrl: '',
    portfolioUrl: '',
    interests: [],
    hideLocation: false,
    hideEmail: false,
  };
  /** Comma-separated text the interests chips are edited through. */
  protected interestsText = '';

  protected readonly loaded = signal(false);
  protected readonly saving = signal(false);
  protected readonly savedAt = signal<Date | null>(null);
  protected readonly error = signal<string | null>(null);
  protected readonly copied = signal(false);
  protected readonly photoError = signal<string | null>(null);
  protected readonly deleting = signal(false);
  protected readonly deleteError = signal<string | null>(null);

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
          portfolioUrl: me.portfolioUrl ?? '',
          interests: me.interests ?? [],
          hideLocation: me.hideLocation,
          hideEmail: me.hideEmail,
        };
        this.interestsText = (me.interests ?? []).join(', ');
        this.loaded.set(true);
      }
    });
  }

  protected save() {
    this.saving.set(true);
    this.error.set(null);
    const payload: ProfileUpdate = {
      ...this.form,
      interests: this.parseInterests(this.interestsText),
    };
    this.api.updateMe(payload).subscribe({
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

  private parseInterests(text: string): string[] {
    return Array.from(
      new Set(
        text
          .split(',')
          .map((s) => s.trim())
          .filter((s) => s.length > 0)
          .map((s) => (s.length > 40 ? s.slice(0, 40) : s)),
      ),
    ).slice(0, 15);
  }

  protected previewInterests() {
    return this.parseInterests(this.interestsText);
  }

  protected onPhotoSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    input.value = '';
    if (!file) return;
    if (!file.type.startsWith('image/')) {
      this.photoError.set('Please choose an image file.');
      return;
    }
    this.photoError.set(null);
    const reader = new FileReader();
    reader.onload = () => {
      const img = new Image();
      img.onload = () => {
        const canvas = document.createElement('canvas');
        canvas.width = MAX_PHOTO_SIDE;
        canvas.height = MAX_PHOTO_SIDE;
        const ctx = canvas.getContext('2d');
        if (!ctx) return;
        const scale = Math.max(MAX_PHOTO_SIDE / img.width, MAX_PHOTO_SIDE / img.height);
        const w = img.width * scale;
        const h = img.height * scale;
        ctx.drawImage(img, (MAX_PHOTO_SIDE - w) / 2, (MAX_PHOTO_SIDE - h) / 2, w, h);
        this.form.pictureUrl = canvas.toDataURL('image/jpeg', 0.85);
      };
      img.onerror = () => this.photoError.set("Couldn't read that image.");
      img.src = reader.result as string;
    };
    reader.readAsDataURL(file);
  }

  protected deleteAccount() {
    if (
      !confirm(
        'Delete your account? Your upcoming meetings will be cancelled (or declined, if you were invited) and everyone involved will be told. Your profile, connections and notes will be gone for good. This can\'t be undone.',
      )
    )
      return;
    this.deleting.set(true);
    this.deleteError.set(null);
    this.api.deleteMe().subscribe({
      next: () => this.session.logout(),
      error: (e) => {
        this.deleting.set(false);
        this.deleteError.set(e.error?.detail ?? "Couldn't delete your account.");
      },
    });
  }

  protected copyLink() {
    const me = this.session.me();
    if (!me) return;
    navigator.clipboard
      .writeText(`${window.location.origin}/people/${me.id}`)
      .then(() => {
        this.copied.set(true);
        setTimeout(() => this.copied.set(false), 1500);
      })
      .catch(() => this.error.set("Couldn't copy the link."));
  }
}
