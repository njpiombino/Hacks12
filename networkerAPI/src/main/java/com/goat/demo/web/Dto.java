package com.goat.demo.web;

import java.time.Instant;
import java.util.List;

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

	public record Person(Long id, String name, String headline, String location, String bio, String email,
			String pictureUrl, Relation relation, Long connectionId) {
	}

	public record PersonSummary(Long id, String name, String pictureUrl) {
	}

	public record ConnectionItem(Long id, Person person, Instant since) {
	}

	public record Connections(List<ConnectionItem> connected, List<ConnectionItem> incoming,
			List<ConnectionItem> outgoing) {
	}

	public record NoteView(Long id, PersonSummary subject, String body, Instant createdAt, Instant updatedAt) {
	}

	public record MeetingView(Long id, String title, Instant startsAt, Instant endsAt, String location,
			String description, boolean mine, PersonSummary with) {
	}

	public record ProfileUpdate(@NotBlank @Size(max = 120) String name, @Size(max = 160) String headline,
			@Size(max = 120) String location, @Size(max = 2000) String bio, @Size(max = 1000) String pictureUrl) {
	}

	/** Details from the Auth0 ID token, used to fill in a brand-new profile. */
	public record ProfileSync(String name, String email, String pictureUrl) {
	}

	public record ConnectRequest(@NotNull Long profileId) {
	}

	public record NoteRequest(@NotBlank @Size(max = 10000) String body) {
	}

	public record MeetingRequest(@NotBlank @Size(max = 200) String title, @NotNull Instant startsAt,
			@NotNull Instant endsAt, @Size(max = 200) String location, @Size(max = 2000) String description,
			Long attendeeId) {
	}

}
