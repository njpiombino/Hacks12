package com.goat.demo.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** A meeting on the owner's calendar, optionally with one of their connections (who sees it too). */
@Entity
@Table(name = "meetings")
public class Meeting {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	private Profile owner;

	@ManyToOne(fetch = FetchType.LAZY)
	private Profile attendee;

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

	public Profile getAttendee() {
		return attendee;
	}

	public void setAttendee(Profile attendee) {
		this.attendee = attendee;
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
