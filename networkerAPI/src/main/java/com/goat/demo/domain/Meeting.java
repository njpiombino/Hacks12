package com.goat.demo.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

/**
 * A meeting on the owner's calendar, optionally with some of their connections. Each attendee accepts or declines
 * on their own; declining takes them off the meeting, and once the last attendee declines it's cancelled.
 */
@Entity
@Table(name = "meetings")
public class Meeting {

	public enum InviteStatus {
		PENDING, ACCEPTED
	}

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	private Profile owner;

	@OneToMany(mappedBy = "meeting", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("id")
	private List<MeetingAttendee> attendees = new ArrayList<>();

	@Column(nullable = false)
	private String title;

	@Column(nullable = false)
	private Instant startsAt;

	@Column(nullable = false)
	private Instant endsAt;

	private String location;

	@Column(length = 2000)
	private String description;

	protected Meeting() {
	}

	public Meeting(Profile owner) {
		this.owner = owner;
	}

	public Long getId() {
		return id;
	}

	public Profile getOwner() {
		return owner;
	}

	public boolean isOwner(Profile profile) {
		return owner.getId().equals(profile.getId());
	}

	public List<MeetingAttendee> getAttendees() {
		return attendees;
	}

	public Optional<MeetingAttendee> attendee(Profile profile) {
		return attendees.stream().filter(a -> a.getProfile().getId().equals(profile.getId())).findFirst();
	}

	public MeetingAttendee invite(Profile profile) {
		MeetingAttendee attendee = new MeetingAttendee(this, profile);
		attendees.add(attendee);
		return attendee;
	}

	public void remove(MeetingAttendee attendee) {
		attendees.remove(attendee);
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public Instant getStartsAt() {
		return startsAt;
	}

	public void setStartsAt(Instant startsAt) {
		this.startsAt = startsAt;
	}

	public Instant getEndsAt() {
		return endsAt;
	}

	public void setEndsAt(Instant endsAt) {
		this.endsAt = endsAt;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

}
