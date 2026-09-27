package com.goat.demo.service;

import java.util.List;

import com.goat.demo.domain.Meeting;
import com.goat.demo.domain.Note;
import com.goat.demo.domain.Notification;
import com.goat.demo.domain.Profile;
import com.goat.demo.web.Dto;
import com.goat.demo.web.Dto.Relation;

/** Maps entities to API views. */
public final class Views {

	private Views() {
	}

	public static Dto.Person person(Profile p, Relation relation, Long connectionId) {
		boolean isSelf = relation == Relation.SELF;
		// Email is only shared with people you're actually connected to, and never if you've hidden it.
		boolean showEmail = isSelf || (relation == Relation.CONNECTED && !p.isHideEmail());
		// Location can be hidden from everyone but yourself.
		boolean showLocation = isSelf || !p.isHideLocation();
		// Resumes, like email, are for connections only.
		boolean showResume = p.hasResume() && (isSelf || relation == Relation.CONNECTED);
		Dto.ResumeInfo resume = showResume
				? new Dto.ResumeInfo(p.getResumeFileName(), p.getResumeSize(), p.getResumeUploadedAt()) : null;
		return new Dto.Person(p.getId(), p.getName(), p.getHeadline(), showLocation ? p.getLocation() : null,
				p.getBio(), showEmail ? p.getEmail() : null, p.getPictureUrl(), p.getPortfolioUrl(), p.getInterests(),
				p.isHideLocation(), p.isHideEmail(), relation, connectionId, resume);
	}

	public static Dto.PersonSummary summary(Profile p) {
		return p == null ? null : new Dto.PersonSummary(p.getId(), p.getName(), p.getPictureUrl());
	}

	public static Dto.NoteView note(Note n) {
		return new Dto.NoteView(n.getId(), summary(n.getSubject()), n.getBody(), n.getCreatedAt(), n.getUpdatedAt());
	}

	public static Dto.MeetingView meeting(Meeting m, Profile me) {
		boolean mine = m.isOwner(me);
		List<Dto.AttendeeView> attendees = m.getAttendees()
			.stream()
			.map(a -> new Dto.AttendeeView(summary(a.getProfile()), status(a.getStatus())))
			.toList();
		Dto.InviteStatus myStatus = m.attendee(me).map(a -> status(a.getStatus())).orElse(null);
		return new Dto.MeetingView(m.getId(), m.getTitle(), m.getStartsAt(), m.getEndsAt(), m.getLocation(),
				m.getDescription(), mine, summary(m.getOwner()), attendees, myStatus);
	}

	private static Dto.InviteStatus status(Meeting.InviteStatus status) {
		return Dto.InviteStatus.valueOf(status.name());
	}

	public static Dto.NotificationView notification(Notification n) {
		return new Dto.NotificationView(n.getId(), n.getType().name(), summary(n.getActor()), n.getMeetingId(),
				n.getTitle(), n.getPreviousTitle(), n.getStartsAt(), n.getLocation(),
				n.getChanges().stream().map(Enum::name).toList(), n.getCreatedAt(), n.getReadAt() != null);
	}

}
