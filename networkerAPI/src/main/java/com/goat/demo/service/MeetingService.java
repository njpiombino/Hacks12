package com.goat.demo.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.goat.demo.domain.Meeting;
import com.goat.demo.domain.MeetingAttendee;
import com.goat.demo.domain.Notification;
import com.goat.demo.domain.Profile;
import com.goat.demo.repository.MeetingRepository;
import com.goat.demo.web.Dto;

@Service
@Transactional
public class MeetingService {

	private final MeetingRepository meetings;

	private final ProfileService profiles;

	private final ConnectionService connections;

	private final NotificationService notifications;

	public MeetingService(MeetingRepository meetings, ProfileService profiles, ConnectionService connections,
			NotificationService notifications) {
		this.meetings = meetings;
		this.profiles = profiles;
		this.connections = connections;
		this.notifications = notifications;
	}

	@Transactional(readOnly = true)
	public List<Dto.MeetingView> between(Profile me, Instant from, Instant to) {
		return meetings.findOverlapping(me, from, to).stream().map(m -> Views.meeting(m, me)).toList();
	}

	@Transactional(readOnly = true)
	public List<Dto.MeetingView> with(Profile me, UUID otherId) {
		return meetings.findWith(me, profiles.get(otherId)).stream().map(m -> Views.meeting(m, me)).toList();
	}

	public Dto.MeetingView create(Profile me, Dto.MeetingRequest request) {
		Map<UUID, Profile> invited = resolveAttendees(me, request);
		Meeting meeting = new Meeting(me);
		applyDetails(meeting, request);
		invited.values().forEach(meeting::invite);
		meetings.save(meeting);
		for (MeetingAttendee a : meeting.getAttendees()) {
			notifications.send(a.getProfile(), me, Notification.Type.INVITED, meeting);
		}
		return Views.meeting(meeting, me);
	}

	public Dto.MeetingView update(Profile me, Long id, Dto.MeetingRequest request) {
		Meeting meeting = findOwned(me, id);
		Map<UUID, Profile> invited = resolveAttendees(me, request);
		String oldTitle = meeting.getTitle();
		Instant oldStart = meeting.getStartsAt();
		Instant oldEnd = meeting.getEndsAt();
		String oldLocation = meeting.getLocation();

		// People taken off the meeting hear it was cancelled, as they last knew it.
		for (MeetingAttendee a : List.copyOf(meeting.getAttendees())) {
			if (!invited.containsKey(a.getProfile().getId())) {
				notifications.send(a.getProfile(), me, Notification.Type.CANCELLED, meeting);
				meeting.remove(a);
			}
		}

		applyDetails(meeting, request);

		List<Notification.Change> changes = new ArrayList<>();
		if (!meeting.getStartsAt().equals(oldStart) || !meeting.getEndsAt().equals(oldEnd)) {
			changes.add(Notification.Change.TIME);
		}
		if (!Objects.equals(meeting.getLocation(), oldLocation)) {
			changes.add(Notification.Change.PLACE);
		}
		if (!meeting.getTitle().equals(oldTitle)) {
			changes.add(Notification.Change.TITLE);
		}

		// Everyone still on it hears what changed; a new time needs a fresh answer from each of them.
		for (MeetingAttendee a : meeting.getAttendees()) {
			if (changes.contains(Notification.Change.TIME)) {
				a.setStatus(Meeting.InviteStatus.PENDING);
			}
			if (!changes.isEmpty()) {
				notifications.sendUpdate(a.getProfile(), me, meeting, changes, oldTitle);
			}
			invited.remove(a.getProfile().getId());
		}

		// Whoever is left in the list is new to the meeting.
		for (Profile p : invited.values()) {
			meeting.invite(p);
			notifications.send(p, me, Notification.Type.INVITED, meeting);
		}
		return Views.meeting(meeting, me);
	}

	public void delete(Profile me, Long id) {
		Meeting meeting = findOwned(me, id);
		for (MeetingAttendee a : meeting.getAttendees()) {
			notifications.send(a.getProfile(), me, Notification.Type.CANCELLED, meeting);
		}
		meetings.delete(meeting);
	}

	/** Upcoming meetings the user has been invited to and hasn't answered yet. */
	@Transactional(readOnly = true)
	public List<Dto.MeetingView> invitations(Profile me) {
		return meetings.findPendingInvitations(me, Instant.now()).stream().map(m -> Views.meeting(m, me)).toList();
	}

	public Dto.MeetingView accept(Profile me, Long id) {
		MeetingAttendee mine = findInvited(me, id);
		Meeting meeting = mine.getMeeting();
		if (mine.getStatus() != Meeting.InviteStatus.ACCEPTED) {
			mine.setStatus(Meeting.InviteStatus.ACCEPTED);
			notifications.send(meeting.getOwner(), me, Notification.Type.ACCEPTED, meeting);
		}
		return Views.meeting(meeting, me);
	}

	/**
	 * Takes the user off the meeting. If they were the last attendee, the meeting is cancelled and comes off the
	 * organizer's calendar too.
	 */
	public void decline(Profile me, Long id) {
		MeetingAttendee mine = findInvited(me, id);
		Meeting meeting = mine.getMeeting();
		meeting.remove(mine);
		if (meeting.getAttendees().isEmpty()) {
			notifications.send(meeting.getOwner(), me, Notification.Type.DECLINED, meeting);
			meetings.delete(meeting);
		}
		else {
			notifications.send(meeting.getOwner(), me, Notification.Type.DROPPED_OUT, meeting);
		}
	}

	private void applyDetails(Meeting meeting, Dto.MeetingRequest request) {
		if (!request.endsAt().isAfter(request.startsAt())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A meeting has to end after it starts");
		}
		meeting.setTitle(request.title().strip());
		meeting.setStartsAt(request.startsAt());
		meeting.setEndsAt(request.endsAt());
		meeting.setLocation(request.location() == null || request.location().isBlank() ? null : request.location().strip());
		meeting.setDescription(request.description() == null || request.description().isBlank() ? null : request.description().strip());
	}

	/** The people to invite, in the order given, after checking each is one of the organizer's connections. */
	private Map<UUID, Profile> resolveAttendees(Profile me, Dto.MeetingRequest request) {
		Map<UUID, Profile> invited = new LinkedHashMap<>();
		if (request.attendeeIds() == null) {
			return invited;
		}
		for (UUID id : request.attendeeIds()) {
			if (id == null || invited.containsKey(id)) {
				continue;
			}
			if (id.equals(me.getId())) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You're the organizer, so you're already coming");
			}
			Profile attendee = profiles.get(id);
			if (!connections.areConnected(me, attendee)) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You can only invite your connections");
			}
			invited.put(id, attendee);
		}
		return invited;
	}

	private MeetingAttendee findInvited(Profile me, Long id) {
		Meeting meeting = meetings.findById(id)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No such meeting"));
		return meeting.attendee(me)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the people invited can answer this"));
	}

	private Meeting findOwned(Profile me, Long id) {
		Meeting meeting = meetings.findById(id)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No such meeting"));
		if (!meeting.isOwner(me)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the organizer can change this meeting");
		}
		return meeting;
	}

}
