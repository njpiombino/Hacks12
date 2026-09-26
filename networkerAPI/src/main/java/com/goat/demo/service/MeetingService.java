package com.goat.demo.service;

import java.time.Instant;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.goat.demo.domain.Meeting;
import com.goat.demo.domain.Profile;
import com.goat.demo.repository.MeetingRepository;
import com.goat.demo.web.Dto;

@Service
@Transactional
public class MeetingService {

	private final MeetingRepository meetings;

	private final ProfileService profiles;

	private final ConnectionService connections;

	public MeetingService(MeetingRepository meetings, ProfileService profiles, ConnectionService connections) {
		this.meetings = meetings;
		this.profiles = profiles;
		this.connections = connections;
	}

	@Transactional(readOnly = true)
	public List<Dto.MeetingView> between(Profile me, Instant from, Instant to) {
		return meetings.findOverlapping(me, from, to).stream().map(m -> Views.meeting(m, me)).toList();
	}

	@Transactional(readOnly = true)
	public List<Dto.MeetingView> with(Profile me, Long otherId) {
		return meetings.findWith(me, profiles.get(otherId)).stream().map(m -> Views.meeting(m, me)).toList();
	}

	public Dto.MeetingView create(Profile me, Dto.MeetingRequest request) {
		Meeting meeting = new Meeting(me);
		apply(me, meeting, request);
		return Views.meeting(meetings.save(meeting), me);
	}

	public Dto.MeetingView update(Profile me, Long id, Dto.MeetingRequest request) {
		Meeting meeting = findOwned(me, id);
		apply(me, meeting, request);
		return Views.meeting(meeting, me);
	}

	public void delete(Profile me, Long id) {
		meetings.delete(findOwned(me, id));
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
		meeting.setTitle(request.title().strip());
		meeting.setStartsAt(request.startsAt());
		meeting.setEndsAt(request.endsAt());
		meeting.setLocation(request.location() == null || request.location().isBlank() ? null : request.location().strip());
		meeting.setDescription(request.description() == null || request.description().isBlank() ? null : request.description().strip());
		meeting.setAttendee(attendee);
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
