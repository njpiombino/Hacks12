import { Component, computed, input } from '@angular/core';

const TINTS = ['#2f5d4e', '#b85c38', '#6b5b95', '#3d6e8f', '#8a6d2f', '#8f4a5c'];

/** A round portrait, falling back to initials on a warm tint derived from the name. */
@Component({
  selector: 'app-avatar',
  template: `
    @if (src()) {
      <img [src]="src()" [alt]="name() ?? ''" referrerpolicy="no-referrer" class="rounded-full object-cover ring-2 ring-card" [class]="sizeClass()" />
    } @else {
      <span
        class="inline-flex select-none items-center justify-center rounded-full font-display font-semibold text-[#fdfbf5] ring-2 ring-card"
        [class]="sizeClass()"
        [style.background]="tint()"
        aria-hidden="true"
        >{{ initials() }}</span
      >
    }
  `,
  host: { class: 'inline-flex shrink-0' },
})
export class Avatar {
  readonly name = input<string | null>(null);
  readonly src = input<string | null>(null);
  readonly size = input<'sm' | 'md' | 'lg' | 'xl' | 'hero'>('md');

  protected readonly initials = computed(() => {
    const parts = (this.name() ?? '?').trim().split(/\s+/);
    return ((parts[0]?.[0] ?? '') + (parts.length > 1 ? parts[parts.length - 1][0] : '')).toUpperCase() || '?';
  });

  protected readonly tint = computed(() => {
    const name = this.name() ?? '';
    let hash = 0;
    for (const ch of name) hash = (hash * 31 + ch.charCodeAt(0)) >>> 0;
    return TINTS[hash % TINTS.length];
  });

  protected readonly sizeClass = computed(
    () =>
      ({
        sm: 'size-8 text-xs',
        md: 'size-11 text-sm',
        lg: 'size-16 text-xl',
        xl: 'size-24 text-3xl',
        hero: 'size-40 text-6xl',
      })[this.size()],
  );
}
