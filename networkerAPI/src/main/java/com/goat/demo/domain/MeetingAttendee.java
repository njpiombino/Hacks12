package com.goat.demo.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/** One person invited to a meeting, and their answer. */
@Entity
@Table(name = "meeting_attendees", uniqueConstraints = @UniqueConstraint(columnNames = { "meeting_id", "profile_id" }))
public class MeetingAttendee {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	private Meeting meeting;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	private Profile profile;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Meeting.InviteStatus status = Meeting.InviteStatus.PENDING;

	protected MeetingAttendee() {
	}

	MeetingAttendee(Meeting meeting, Profile profile) {
		this.meeting = meeting;
		this.profile = profile;
	}

	public Long getId() {
		return id;
	}

	public Meeting getMeeting() {
		return meeting;
	}

	public Profile getProfile() {
		return profile;
	}

	public Meeting.InviteStatus getStatus() {
		return status;
	}

	public void setStatus(Meeting.InviteStatus status) {
		this.status = status;
	}

}
