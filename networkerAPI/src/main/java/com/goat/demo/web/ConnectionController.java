package com.goat.demo.web;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.goat.demo.domain.Profile;
import com.goat.demo.service.ConnectionService;
import com.goat.demo.service.ProfileService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/connections")
public class ConnectionController {

	private final ProfileService profiles;

	private final ConnectionService connections;

	public ConnectionController(ProfileService profiles, ConnectionService connections) {
		this.profiles = profiles;
		this.connections = connections;
	}

	@GetMapping
	public Dto.Connections list(@AuthenticationPrincipal Jwt jwt) {
		return connections.overview(profiles.current(jwt));
	}

	@PostMapping
	public Dto.Connections request(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody Dto.ConnectRequest request) {
		Profile me = profiles.current(jwt);
		connections.request(me, profiles.get(request.profileId()));
		return connections.overview(me);
	}

	@PostMapping("/{id}/accept")
	public Dto.Connections accept(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
		Profile me = profiles.current(jwt);
		connections.accept(me, id);
		return connections.overview(me);
	}

	@DeleteMapping("/{id}")
	public Dto.Connections remove(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
		Profile me = profiles.current(jwt);
		connections.remove(me, id);
		return connections.overview(me);
	}

}
