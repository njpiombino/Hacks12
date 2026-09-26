import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Subject, debounceTime, distinctUntilChanged, switchMap } from 'rxjs';

import { Api } from '../../core/api';
import { Connections, Person } from '../../core/models';
import { Avatar } from '../../shared/avatar';

type Tab = 'connected' | 'find' | 'requests';

@Component({
  selector: 'app-people',
  imports: [RouterLink, FormsModule, DatePipe, Avatar],
  templateUrl: './people.html',
})
export class People {
  private readonly api = inject(Api);
  private readonly queries = new Subject<string>();

  protected readonly tab = signal<Tab>('connected');
  protected readonly connections = signal<Connections | null>(null);
  protected readonly results = signal<Person[] | null>(null);
  protected query = '';

  constructor() {
    this.api.connections().subscribe((c) => {
      this.connections.set(c);
      if (!c.connected.length) this.show('find');
    });
    this.queries
      .pipe(
        debounceTime(200),
        distinctUntilChanged(),
        switchMap((q) => this.api.searchPeople(q)),
      )
      .subscribe((r) => this.results.set(r));
  }

  protected show(tab: Tab) {
    this.tab.set(tab);
    if (tab === 'find' && !this.results()) this.api.searchPeople('').subscribe((r) => this.results.set(r));
  }

  protected search() {
    this.queries.next(this.query.trim());
  }

  protected connect(person: Person) {
    this.api.connect(person.id).subscribe((c) => this.afterChange(c));
  }

  protected accept(id: number) {
    this.api.acceptConnection(id).subscribe((c) => this.afterChange(c));
  }

  protected remove(id: number) {
    this.api.removeConnection(id).subscribe((c) => this.afterChange(c));
  }

  private afterChange(c: Connections) {
    this.connections.set(c);
    // Refresh relation badges in search results.
    if (this.results()) this.api.searchPeople(this.query.trim()).subscribe((r) => this.results.set(r));
  }
}
