package com.goat.demo.service;

import com.goat.demo.domain.Meeting;
import com.goat.demo.domain.Note;
import com.goat.demo.domain.Profile;
import com.goat.demo.web.Dto;
import com.goat.demo.web.Dto.Relation;

/** Maps entities to API views. */
public final class Views {

	private Views() {
	}

	public static Dto.Person person(Profile p, Relation relation, Long connectionId) {
		// Email is only shared with people you're actually connected to.
		boolean showEmail = relation == Relation.CONNECTED || relation == Relation.SELF;
		return new Dto.Person(p.getId(), p.getName(), p.getHeadline(), p.getLocation(), p.getBio(),
				showEmail ? p.getEmail() : null, p.getPictureUrl(), relation, connectionId);
	}

	public static Dto.PersonSummary summary(Profile p) {
		return p == null ? null : new Dto.PersonSummary(p.getId(), p.getName(), p.getPictureUrl());
	}

	public static Dto.NoteView note(Note n) {
		return new Dto.NoteView(n.getId(), summary(n.getSubject()), n.getBody(), n.getCreatedAt(), n.getUpdatedAt());
	}

	public static Dto.MeetingView meeting(Meeting m, Profile me) {
		boolean mine = m.getOwner().getId().equals(me.getId());
		Profile with = mine ? m.getAttendee() : m.getOwner();
		return new Dto.MeetingView(m.getId(), m.getTitle(), m.getStartsAt(), m.getEndsAt(), m.getLocation(),
				m.getDescription(), mine, summary(with));
	}

}
