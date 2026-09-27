import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { environment } from '../../environments/environment';
import { AppNotification, Connections, Meeting, MeetingRequest, Note, Person, ProfileUpdate } from './models';

@Injectable({ providedIn: 'root' })
export class Api {
  private readonly http = inject(HttpClient);
  private readonly base = environment.apiUrl;

  me() {
    return this.http.get<Person>(`${this.base}/me`);
  }

  syncMe(details: { name?: string; email?: string; pictureUrl?: string }) {
    return this.http.post<Person>(`${this.base}/me/sync`, details);
  }

  updateMe(update: ProfileUpdate) {
    return this.http.put<Person>(`${this.base}/me`, update);
  }

  deleteMe() {
    return this.http.delete<void>(`${this.base}/me`);
  }

  searchPeople(q: string) {
    return this.http.get<Person[]>(`${this.base}/people`, { params: { q } });
  }

  person(id: string) {
    return this.http.get<Person>(`${this.base}/people/${id}`);
  }

  connections() {
    return this.http.get<Connections>(`${this.base}/connections`);
  }

  connect(profileId: string) {
    return this.http.post<Connections>(`${this.base}/connections`, { profileId });
  }

  acceptConnection(id: number) {
    return this.http.post<Connections>(`${this.base}/connections/${id}/accept`, {});
  }

  removeConnection(id: number) {
    return this.http.delete<Connections>(`${this.base}/connections/${id}`);
  }

  notesAbout(personId: string) {
    return this.http.get<Note[]>(`${this.base}/people/${personId}/notes`);
  }

  recentNotes() {
    return this.http.get<Note[]>(`${this.base}/notes/recent`);
  }

  addNote(personId: string, body: string) {
    return this.http.post<Note>(`${this.base}/people/${personId}/notes`, { body });
  }

  updateNote(id: number, body: string) {
    return this.http.put<Note>(`${this.base}/notes/${id}`, { body });
  }

  deleteNote(id: number) {
    return this.http.delete<void>(`${this.base}/notes/${id}`);
  }

  meetings(from: Date, to: Date) {
    const params = new HttpParams().set('from', from.toISOString()).set('to', to.toISOString());
    return this.http.get<Meeting[]>(`${this.base}/meetings`, { params });
  }

  meetingsWith(personId: string) {
    return this.http.get<Meeting[]>(`${this.base}/people/${personId}/meetings`);
  }

  createMeeting(request: MeetingRequest) {
    return this.http.post<Meeting>(`${this.base}/meetings`, request);
  }

  updateMeeting(id: number, request: MeetingRequest) {
    return this.http.put<Meeting>(`${this.base}/meetings/${id}`, request);
  }

  deleteMeeting(id: number) {
    return this.http.delete<void>(`${this.base}/meetings/${id}`);
  }

  meetingInvitations() {
    return this.http.get<Meeting[]>(`${this.base}/meetings/invitations`);
  }

  acceptMeeting(id: number) {
    return this.http.post<Meeting>(`${this.base}/meetings/${id}/accept`, {});
  }

  /** Declining cancels the meeting for both people. */
  declineMeeting(id: number) {
    return this.http.post<void>(`${this.base}/meetings/${id}/decline`, {});
  }

  notifications() {
    return this.http.get<AppNotification[]>(`${this.base}/notifications`);
  }

  markNotificationsRead() {
    return this.http.post<void>(`${this.base}/notifications/read`, {});
  }
}
