package com.goat.demo.web;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.goat.demo.service.AccountService;
import com.goat.demo.service.ProfileService;
import com.goat.demo.service.Views;
import com.goat.demo.web.Dto.Relation;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/me")
public class MeController {

	private final ProfileService profiles;

	private final AccountService accounts;

	public MeController(ProfileService profiles, AccountService accounts) {
		this.profiles = profiles;
		this.accounts = accounts;
	}

	@GetMapping
	public Dto.Person me(@AuthenticationPrincipal Jwt jwt) {
		return Views.person(profiles.current(jwt), Relation.SELF, null);
	}

	@PostMapping("/sync")
	public Dto.Person sync(@AuthenticationPrincipal Jwt jwt, @RequestBody Dto.ProfileSync sync) {
		return Views.person(profiles.sync(profiles.current(jwt), sync), Relation.SELF, null);
	}

	@PutMapping
	public Dto.Person update(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody Dto.ProfileUpdate update) {
		return Views.person(profiles.update(profiles.current(jwt), update), Relation.SELF, null);
	}

	@DeleteMapping
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@AuthenticationPrincipal Jwt jwt) {
		accounts.delete(profiles.current(jwt));
	}

}
