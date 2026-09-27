import { Meeting, PersonSummary } from './models';

/** Everyone in the meeting except you: the organizer first (if that's not you), then the other attendees. */
export function otherPeople(m: Meeting, meId: string | undefined): PersonSummary[] {
  const attendees = m.attendees.map((a) => a.person).filter((p) => p.id !== meId);
  return m.mine ? attendees : [m.organizer, ...attendees];
}

/** "Maya", "Maya and Tom", "Maya, Tom and 2 others". */
export function namesOf(people: PersonSummary[]): string {
  const names = people.map((p) => p.name ?? 'Someone');
  if (names.length <= 2) return names.join(' and ');
  const rest = names.length - 2;
  return `${names[0]}, ${names[1]} and ${rest} other${rest === 1 ? '' : 's'}`;
}

/** You organized it and at least one invitee hasn't answered. */
export function awaitingReplies(m: Meeting): boolean {
  return m.mine && m.attendees.some((a) => a.status === 'PENDING');
}

/** You're invited and haven't answered. */
export function needsMyAnswer(m: Meeting): boolean {
  return m.myStatus === 'PENDING';
}

/** Whether declining would cancel the whole meeting, i.e. you're the only one invited. */
export function declineCancels(m: Meeting): boolean {
  return m.attendees.length <= 1;
}
