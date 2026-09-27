package com.goat.demo.web;

import java.util.List;
import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.goat.demo.domain.Connection;
import com.goat.demo.domain.Profile;
import com.goat.demo.repository.ProfileRepository;
import com.goat.demo.service.ConnectionService;
import com.goat.demo.service.MeetingService;
import com.goat.demo.service.NoteService;
import com.goat.demo.service.ProfileService;
import com.goat.demo.service.Views;
import com.goat.demo.web.Dto.Relation;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
public class PeopleController {

	private final ProfileService profiles;

	private final ProfileRepository profileRepository;

	private final ConnectionService connections;

	private final NoteService notes;

	private final MeetingService meetings;

	public PeopleController(ProfileService profiles, ProfileRepository profileRepository,
			ConnectionService connections, NoteService notes, MeetingService meetings) {
		this.profiles = profiles;
		this.profileRepository = profileRepository;
		this.connections = connections;
		this.notes = notes;
		this.meetings = meetings;
	}

	/** Finds people to connect with. Existing connections are left out; pending requests stay in. */
	@GetMapping("/people")
	public List<Dto.Person> search(@AuthenticationPrincipal Jwt jwt, @RequestParam(defaultValue = "") String q) {
		Profile me = profiles.current(jwt);
		var byPerson = connections.byOtherPerson(me);
		return profileRepository.search(q.strip(), me.getId())
			.stream()
			.filter(p -> p.getName() != null)
			.map(p -> {
				Connection c = byPerson.get(p.getId());
				return Views.person(p, ConnectionService.relation(me, c), c == null ? null : c.getId());
			})
			// Already-connected people belong in "Your connections", not the search results.
			.filter(p -> p.relation() != Relation.CONNECTED)
			.toList();
	}

	@GetMapping("/people/{id}")
	public Dto.Person person(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
		Profile me = profiles.current(jwt);
		Profile other = profiles.get(id);
		if (other.getId().equals(me.getId())) {
			return Views.person(me, Relation.SELF, null);
		}
		Connection c = connections.byOtherPerson(me).get(other.getId());
		return Views.person(other, ConnectionService.relation(me, c), c == null ? null : c.getId());
	}

	@GetMapping("/people/{id}/notes")
	public List<Dto.NoteView> notesAbout(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
		return notes.about(profiles.current(jwt), id);
	}

	@PostMapping("/people/{id}/notes")
	public Dto.NoteView addNote(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
			@Valid @RequestBody Dto.NoteRequest request) {
		return notes.create(profiles.current(jwt), id, request);
	}

	@GetMapping("/people/{id}/meetings")
	public List<Dto.MeetingView> meetingsWith(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
		return meetings.with(profiles.current(jwt), id);
	}

	@GetMapping("/notes/recent")
	public List<Dto.NoteView> recentNotes(@AuthenticationPrincipal Jwt jwt) {
		return notes.recent(profiles.current(jwt), 6);
	}

	@PutMapping("/notes/{id}")
	public Dto.NoteView updateNote(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
			@Valid @RequestBody Dto.NoteRequest request) {
		return notes.update(profiles.current(jwt), id, request);
	}

	@DeleteMapping("/notes/{id}")
	public void deleteNote(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
		notes.delete(profiles.current(jwt), id);
	}

}
