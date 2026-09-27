import { Component, ElementRef, ViewChild, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { Api } from '../../core/api';
import { ChatMessage } from '../../core/models';

@Component({
  selector: 'app-chat',
  imports: [FormsModule],
  templateUrl: './chat.html',
})
export class Chat {
  private readonly api = inject(Api);

  @ViewChild('scrollAnchor') private scrollAnchor?: ElementRef<HTMLElement>;

  protected readonly messages = signal<ChatMessage[]>([
    { role: 'model', text: "Hi! I can help you find people in your network or schedule a meeting with a connection. What do you need?" },
  ]);
  protected readonly sending = signal(false);
  protected readonly error = signal<string | null>(null);
  protected draft = '';

  protected send() {
    const text = this.draft.trim();
    if (!text || this.sending()) return;

    const history = this.messages();
    this.messages.update((list) => [...list, { role: 'user', text }]);
    this.draft = '';
    this.sending.set(true);
    this.error.set(null);
    this.scrollDown();

    this.api.chat(text, history).subscribe({
      next: (res) => {
        this.messages.update((list) => [...list, { role: 'model', text: res.reply }]);
        this.sending.set(false);
        this.scrollDown();
      },
      error: (e) => {
        this.sending.set(false);
        this.error.set(e.error?.detail ?? "Couldn't reach the chatbot. Try again in a moment.");
      },
    });
  }

  private scrollDown() {
    queueMicrotask(() => this.scrollAnchor?.nativeElement.scrollIntoView({ behavior: 'smooth' }));
  }
}
