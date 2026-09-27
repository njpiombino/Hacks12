export type Relation = 'SELF' | 'NONE' | 'OUTGOING' | 'INCOMING' | 'CONNECTED';

export interface Person {
  id: string;
  name: string | null;
  headline: string | null;
  location: string | null;
  bio: string | null;
  email: string | null;
  pictureUrl: string | null;
  portfolioUrl: string | null;
  interests: string[];
  hideLocation: boolean;
  hideEmail: boolean;
  relation: Relation;
  connectionId: number | null;
}

export interface PersonSummary {
  id: string;
  name: string | null;
  pictureUrl: string | null;
}

export interface ConnectionItem {
  id: number;
  person: Person;
  since: string;
}

export interface Connections {
  connected: ConnectionItem[];
  incoming: ConnectionItem[];
  outgoing: ConnectionItem[];
}

export interface Note {
  id: number;
  subject: PersonSummary;
  body: string;
  createdAt: string;
  updatedAt: string;
}

export interface Meeting {
  id: number;
  title: string;
  startsAt: string;
  endsAt: string;
  location: string | null;
  description: string | null;
  mine: boolean;
  with: PersonSummary | null;
  /** The invitee's answer; null for meetings with no one else. */
  inviteStatus: 'PENDING' | 'ACCEPTED' | null;
}

export type NotificationType = 'INVITED' | 'UPDATED' | 'CANCELLED' | 'ACCEPTED' | 'DECLINED';

/** Something the other person did to a meeting. Title, time and place are as they were right after the change. */
export interface AppNotification {
  id: number;
  type: NotificationType;
  actor: PersonSummary;
  meetingId: number | null;
  title: string;
  previousTitle: string | null;
  startsAt: string;
  location: string | null;
  /** For UPDATED: which of these changed. */
  changes: ('TIME' | 'PLACE' | 'TITLE')[];
  createdAt: string;
  read: boolean;
}

export interface MeetingRequest {
  title: string;
  startsAt: string;
  endsAt: string;
  location: string | null;
  description: string | null;
  attendeeId: string | null;
}

export interface ProfileUpdate {
  name: string;
  headline: string | null;
  location: string | null;
  bio: string | null;
  pictureUrl: string | null;
  portfolioUrl: string | null;
  interests: string[];
  hideLocation: boolean;
  hideEmail: boolean;
}
