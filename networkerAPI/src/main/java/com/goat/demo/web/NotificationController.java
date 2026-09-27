package com.goat.demo.web;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.goat.demo.service.NotificationService;
import com.goat.demo.service.ProfileService;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

	private final ProfileService profiles;

	private final NotificationService notifications;

	public NotificationController(ProfileService profiles, NotificationService notifications) {
		this.profiles = profiles;
		this.notifications = notifications;
	}

	@GetMapping
	public List<Dto.NotificationView> recent(@AuthenticationPrincipal Jwt jwt) {
		return notifications.recent(profiles.current(jwt));
	}

	@PostMapping("/{id}/read")
	public void markRead(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
		notifications.markRead(profiles.current(jwt), id);
	}

	@PostMapping("/read")
	public void markAllRead(@AuthenticationPrincipal Jwt jwt) {
		notifications.markAllRead(profiles.current(jwt));
	}

}
