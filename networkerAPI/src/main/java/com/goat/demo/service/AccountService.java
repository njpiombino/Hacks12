package com.goat.demo.service;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.goat.demo.domain.Meeting;
import com.goat.demo.domain.Profile;
import com.goat.demo.repository.ConnectionRepository;
import com.goat.demo.repository.MeetingRepository;
import com.goat.demo.repository.NoteRepository;
import com.goat.demo.repository.NotificationRepository;
import com.goat.demo.repository.ProfileRepository;

@Service
public class AccountService {

	private final ProfileRepository profiles;

	private final MeetingService meetingService;

	private final MeetingRepository meetings;

	private final NotificationRepository notifications;

	private final NoteRepository notes;

	private final ConnectionRepository connections;

	public AccountService(ProfileRepository profiles, MeetingService meetingService, MeetingRepository meetings,
			NotificationRepository notifications, NoteRepository notes, ConnectionRepository connections) {
		this.profiles = profiles;
		this.meetingService = meetingService;
		this.meetings = meetings;
		this.notifications = notifications;
		this.notes = notes;
		this.connections = connections;
	}

	/**
	 * Deletes the profile and everything tied to it. Upcoming meetings go the usual way first, so people hear about
	 * it: the ones they organize are cancelled, and they decline the ones they're invited to. Past meetings are
	 * removed quietly. Their Auth0 login is untouched, so signing in again starts a fresh, empty profile.
	 */
	@Transactional
	public void delete(Profile me) {
		for (Meeting m : meetings.findUpcomingInvolving(me, Instant.now())) {
			if (m.isOwner(me)) {
				meetingService.delete(me, m.getId());
			}
			else {
				meetingService.decline(me, m.getId());
			}
		}
		notifications.deleteForRecipient(me);
		notifications.detachActor(me);
		meetings.deleteAttendeesInvolving(me);
		meetings.deleteOwnedBy(me);
		notes.deleteInvolving(me);
		connections.deleteInvolving(me);
		profiles.deleteById(me.getId());
	}

}
