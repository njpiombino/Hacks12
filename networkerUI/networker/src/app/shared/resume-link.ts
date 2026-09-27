import { Component, inject, input, signal } from '@angular/core';

import { Api } from '../core/api';
import { ResumeInfo } from '../core/models';

/** Opens someone's resume PDF in a new tab. */
@Component({
  selector: 'app-resume-link',
  template: `
    <button type="button" class="inline-flex items-center gap-1.5 text-forest hover:underline disabled:opacity-60" [disabled]="opening()" [title]="resume().fileName" (click)="open()">
      <svg viewBox="0 0 24 24" class="size-4" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
        <path d="M14 3H7a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V8z" />
        <path d="M14 3v5h5M9 13h6M9 17h6" />
      </svg>
      {{ opening() ? 'Opening…' : label() }}
    </button>
    @if (error()) {
      <span class="ml-2 text-xs text-clay">{{ error() }}</span>
    }
  `,
})
export class ResumeLink {
  private readonly api = inject(Api);

  readonly personId = input.required<string>();
  readonly resume = input.required<ResumeInfo>();
  readonly label = input('Resume');

  protected readonly opening = signal(false);
  protected readonly error = signal<string | null>(null);

  protected open() {
    // Open the tab now, while we still have the click; browsers block pop-ups opened after a request.
    const tab = window.open('', '_blank');
    this.opening.set(true);
    this.error.set(null);
    this.api.resumeFile(this.personId()).subscribe({
      next: (blob) => {
        this.opening.set(false);
        const url = URL.createObjectURL(blob);
        if (tab) {
          tab.location.href = url;
        } else {
          window.location.href = url;
        }
        setTimeout(() => URL.revokeObjectURL(url), 60_000);
      },
      error: () => {
        tab?.close();
        this.opening.set(false);
        this.error.set("Couldn't open it.");
      },
    });
  }
}
