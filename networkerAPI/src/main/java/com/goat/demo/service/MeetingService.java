package com.goat.demo.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.goat.demo.domain.Meeting;
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
		Meeting meeting = new Meeting(me);
		apply(me, meeting, request);
		meetings.save(meeting);
		if (meeting.getAttendee() != null) {
			notifications.send(meeting.getAttendee(), me, Notification.Type.INVITED, meeting);
		}
		return Views.meeting(meeting, me);
	}

	public Dto.MeetingView update(Profile me, Long id, Dto.MeetingRequest request) {
		Meeting meeting = findOwned(me, id);
		Profile before = meeting.getAttendee();
		String oldTitle = meeting.getTitle();
		Instant oldStart = meeting.getStartsAt();
		Instant oldEnd = meeting.getEndsAt();
		String oldLocation = meeting.getLocation();

		// Someone taken off the meeting hears it was cancelled, as they last knew it.
		if (before != null && (request.attendeeId() == null || !request.attendeeId().equals(before.getId()))) {
			notifications.send(before, me, Notification.Type.CANCELLED, meeting);
		}

		apply(me, meeting, request);

		Profile after = meeting.getAttendee();
		if (after != null && (before == null || !before.getId().equals(after.getId()))) {
			notifications.send(after, me, Notification.Type.INVITED, meeting);
		}
		else if (after != null) {
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
			if (!changes.isEmpty()) {
				notifications.sendUpdate(after, me, meeting, changes, oldTitle);
			}
		}
		return Views.meeting(meeting, me);
	}

	public void delete(Profile me, Long id) {
		Meeting meeting = findOwned(me, id);
		if (meeting.getAttendee() != null) {
			notifications.send(meeting.getAttendee(), me, Notification.Type.CANCELLED, meeting);
		}
		meetings.delete(meeting);
	}

	/** Upcoming meetings the user has been invited to and hasn't answered yet. */
	@Transactional(readOnly = true)
	public List<Dto.MeetingView> invitations(Profile me) {
		return meetings.findPendingInvitations(me, Instant.now()).stream().map(m -> Views.meeting(m, me)).toList();
	}

	public Dto.MeetingView accept(Profile me, Long id) {
		Meeting meeting = findInvited(me, id);
		if (meeting.getInviteStatus() != Meeting.InviteStatus.ACCEPTED) {
			meeting.setInviteStatus(Meeting.InviteStatus.ACCEPTED);
			notifications.send(meeting.getOwner(), me, Notification.Type.ACCEPTED, meeting);
		}
		return Views.meeting(meeting, me);
	}

	/** Turning down an invitation cancels the meeting, so it comes off both calendars. */
	public void decline(Profile me, Long id) {
		Meeting meeting = findInvited(me, id);
		notifications.send(meeting.getOwner(), me, Notification.Type.DECLINED, meeting);
		meetings.delete(meeting);
	}

	private void apply(Profile me, Meeting meeting, Dto.MeetingRequest request) {
		if (!request.endsAt().isAfter(request.startsAt())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A meeting has to end after it starts");
		}
		Profile attendee = null;
		if (request.attendeeId() != null) {
			attendee = profiles.get(request.attendeeId());
			if (!connections.areConnected(me, attendee)) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You can only invite your connections");
			}
		}
		// A new invitee, or a new time for the current one, needs a fresh answer.
		boolean sameAttendee = attendee != null && meeting.isAttendee(attendee);
		boolean sameTime = request.startsAt().equals(meeting.getStartsAt()) && request.endsAt().equals(meeting.getEndsAt());
		if (attendee == null) {
			meeting.setInviteStatus(null);
		}
		else if (!sameAttendee || !sameTime) {
			meeting.setInviteStatus(Meeting.InviteStatus.PENDING);
		}
		meeting.setTitle(request.title().strip());
		meeting.setStartsAt(request.startsAt());
		meeting.setEndsAt(request.endsAt());
		meeting.setLocation(request.location() == null || request.location().isBlank() ? null : request.location().strip());
		meeting.setDescription(request.description() == null || request.description().isBlank() ? null : request.description().strip());
		meeting.setAttendee(attendee);
	}

	private Meeting findInvited(Profile me, Long id) {
		Meeting meeting = meetings.findById(id)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No such meeting"));
		if (!meeting.isAttendee(me)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the person invited can answer this");
		}
		return meeting;
	}

	private Meeting findOwned(Profile me, Long id) {
		Meeting meeting = meetings.findById(id)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No such meeting"));
		if (!meeting.getOwner().getId().equals(me.getId())) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the organizer can change this meeting");
		}
		return meeting;
	}

}
