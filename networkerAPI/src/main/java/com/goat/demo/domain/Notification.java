package com.goat.demo.domain;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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

/**
 * Tells someone that the other person in a meeting did something to it. Keeps its own copy of the meeting's
 * title, time and place, so it still reads right after the meeting changes again or is deleted.
 */
@Entity
@Table(name = "notifications")
public class Notification {

	/**
	 * DECLINED means the last attendee declined, so the meeting was cancelled; DROPPED_OUT means one attendee of
	 * several declined and the meeting goes on without them.
	 */
	public enum Type {
		INVITED, UPDATED, CANCELLED, ACCEPTED, DECLINED, DROPPED_OUT
	}

	/** What an UPDATED notification is about. */
	public enum Change {
		TIME, PLACE, TITLE
	}

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	private Profile recipient;

	/** Who made the change. Null once they've deleted their account. */
	@ManyToOne(fetch = FetchType.LAZY)
	private Profile actor;

	/** The actor's name when this was sent, shown after they've deleted their account. */
	private String actorName;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Type type;

	/** Not a foreign key: the meeting may since have been deleted. */
	private Long meetingId;

	@Column(nullable = false)
	private String title;

	/** The title before an UPDATED notification's TITLE change. */
	private String previousTitle;

	@Column(nullable = false)
	private Instant startsAt;

	private String location;

	/** Comma-separated {@link Change}s, for UPDATED notifications. */
	private String changes;

	@Column(nullable = false)
	private Instant createdAt = Instant.now();

	private Instant readAt;

	protected Notification() {
	}

	public Notification(Profile recipient, Profile actor, Type type, Meeting meeting) {
		this.recipient = recipient;
		this.actor = actor;
		this.actorName = actor.getName();
		this.type = type;
		this.meetingId = meeting.getId();
		this.title = meeting.getTitle();
		this.startsAt = meeting.getStartsAt();
		this.location = meeting.getLocation();
	}

	public Long getId() {
		return id;
	}

	public Profile getRecipient() {
		return recipient;
	}

	public Profile getActor() {
		return actor;
	}

	public String getActorName() {
		return actorName;
	}

	public Type getType() {
		return type;
	}

	public Long getMeetingId() {
		return meetingId;
	}

	public String getTitle() {
		return title;
	}

	public String getPreviousTitle() {
		return previousTitle;
	}

	public void setPreviousTitle(String previousTitle) {
		this.previousTitle = previousTitle;
	}

	public Instant getStartsAt() {
		return startsAt;
	}

	public String getLocation() {
		return location;
	}

	public List<Change> getChanges() {
		return changes == null ? List.of() : Arrays.stream(changes.split(",")).map(Change::valueOf).toList();
	}

	public void setChanges(List<Change> changes) {
		this.changes = changes.isEmpty() ? null : changes.stream().map(Change::name).collect(Collectors.joining(","));
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getReadAt() {
		return readAt;
	}

	public void markRead() {
		if (readAt == null) {
			readAt = Instant.now();
		}
	}

}
