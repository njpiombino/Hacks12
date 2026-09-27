package com.goat.demo.web;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Request and response shapes for the REST API. */
public final class Dto {

	private Dto() {
	}

	/** How the current user relates to another person. */
	public enum Relation {
		SELF, NONE, OUTGOING, INCOMING, CONNECTED
	}

	public record Person(UUID id, String name, String headline, String location, String bio, String email,
			String pictureUrl, String portfolioUrl, List<String> interests, boolean hideLocation, boolean hideEmail,
			Relation relation, Long connectionId, ResumeInfo resume) {
	}

	/** About an uploaded resume; the file comes from GET /api/people/{id}/resume. */
	public record ResumeInfo(String fileName, int sizeBytes, Instant uploadedAt) {
	}

	public record PersonSummary(UUID id, String name, String pictureUrl) {
	}

	public record ConnectionItem(Long id, Person person, Instant since) {
	}

	public record Connections(List<ConnectionItem> connected, List<ConnectionItem> incoming,
			List<ConnectionItem> outgoing) {
	}

	public record NoteView(Long id, PersonSummary subject, String body, Instant createdAt, Instant updatedAt) {
	}

	/** Whether the attendee has accepted a meeting invitation yet. */
	public enum InviteStatus {
		PENDING, ACCEPTED
	}

	public record AttendeeView(PersonSummary person, InviteStatus status) {
	}

	/**
	 * A meeting as the signed-in user sees it. {@code myStatus} is their own answer when they're an attendee, and
	 * null when they organized it.
	 */
	public record MeetingView(Long id, String title, Instant startsAt, Instant endsAt, String location,
			String description, boolean mine, PersonSummary organizer, List<AttendeeView> attendees,
			InviteStatus myStatus) {
	}

	/**
	 * Something another person did to a meeting. {@code type} is INVITED, UPDATED, CANCELLED, ACCEPTED, DECLINED
	 * (the last attendee declined, so it's cancelled) or DROPPED_OUT (one of several attendees declined); for
	 * UPDATED, {@code changes} lists any of TIME, PLACE and TITLE. The title, time and place are as they were right
	 * after the change.
	 */
	public record NotificationView(Long id, String type, PersonSummary actor, Long meetingId, String title,
			String previousTitle, Instant startsAt, String location, List<String> changes, Instant createdAt,
			boolean read) {
	}

	public record ProfileUpdate(@NotBlank @Size(max = 120) String name, @Size(max = 160) String headline,
			@Size(max = 120) String location, @Size(max = 2000) String bio,
			@Size(max = 2_000_000) String pictureUrl, @Size(max = 300) String portfolioUrl, List<String> interests,
			boolean hideLocation, boolean hideEmail) {
	}

	/** Details from the Auth0 ID token, used to fill in a brand-new profile. */
	public record ProfileSync(String name, String email, String pictureUrl) {
	}

	public record ConnectRequest(@NotNull UUID profileId) {
	}

	public record NoteRequest(@NotBlank @Size(max = 10000) String body) {
	}

	public record MeetingRequest(@NotBlank @Size(max = 200) String title, @NotNull Instant startsAt,
			@NotNull Instant endsAt, @Size(max = 200) String location, @Size(max = 2000) String description,
			@Size(max = 20) List<UUID> attendeeIds) {
	}

}
