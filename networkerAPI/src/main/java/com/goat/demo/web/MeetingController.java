package com.goat.demo.web;

import java.time.Instant;
import java.util.List;

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

import com.goat.demo.service.MeetingService;
import com.goat.demo.service.ProfileService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/meetings")
public class MeetingController {

	private final ProfileService profiles;

	private final MeetingService meetings;

	public MeetingController(ProfileService profiles, MeetingService meetings) {
		this.profiles = profiles;
		this.meetings = meetings;
	}

	@GetMapping
	public List<Dto.MeetingView> list(@AuthenticationPrincipal Jwt jwt, @RequestParam Instant from,
			@RequestParam Instant to) {
		return meetings.between(profiles.current(jwt), from, to);
	}

	@PostMapping
	public Dto.MeetingView create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody Dto.MeetingRequest request) {
		return meetings.create(profiles.current(jwt), request);
	}

	@PutMapping("/{id}")
	public Dto.MeetingView update(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
			@Valid @RequestBody Dto.MeetingRequest request) {
		return meetings.update(profiles.current(jwt), id, request);
	}

	@DeleteMapping("/{id}")
	public void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
		meetings.delete(profiles.current(jwt), id);
	}

	@GetMapping("/invitations")
	public List<Dto.MeetingView> invitations(@AuthenticationPrincipal Jwt jwt) {
		return meetings.invitations(profiles.current(jwt));
	}

	@PostMapping("/{id}/accept")
	public Dto.MeetingView accept(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
		return meetings.accept(profiles.current(jwt), id);
	}

	@PostMapping("/{id}/decline")
	public void decline(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
		meetings.decline(profiles.current(jwt), id);
	}

}
