package com.goat.demo.web;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.goat.demo.service.GeminiService;
import com.goat.demo.service.ProfileService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

	private final ProfileService profiles;

	private final GeminiService gemini;

	public ChatController(ProfileService profiles, GeminiService gemini) {
		this.profiles = profiles;
		this.gemini = gemini;
	}

	@PostMapping
	public Dto.ChatResponse chat(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody Dto.ChatRequest request) {
		String reply = gemini.chat(profiles.current(jwt), request.message(), request.history(), request.timezone());
		return new Dto.ChatResponse(reply);
	}

}
