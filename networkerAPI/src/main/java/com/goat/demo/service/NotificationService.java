package com.goat.demo.service;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Limit;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.goat.demo.domain.Meeting;
import com.goat.demo.domain.Notification;
import com.goat.demo.domain.Profile;
import com.goat.demo.repository.NotificationRepository;
import com.goat.demo.web.Dto;

@Service
@Transactional
public class NotificationService {

	private static final int RECENT = 30;

	private final NotificationRepository notifications;

	public NotificationService(NotificationRepository notifications) {
		this.notifications = notifications;
	}

	/** Records that {@code actor} did something to {@code meeting} that {@code recipient} should hear about. */
	public Notification send(Profile recipient, Profile actor, Notification.Type type, Meeting meeting) {
		return notifications.save(new Notification(recipient, actor, type, meeting));
	}

	public void sendUpdate(Profile recipient, Profile actor, Meeting meeting, List<Notification.Change> changes,
			String previousTitle) {
		Notification n = new Notification(recipient, actor, Notification.Type.UPDATED, meeting);
		n.setChanges(changes);
		if (changes.contains(Notification.Change.TITLE)) {
			n.setPreviousTitle(previousTitle);
		}
		notifications.save(n);
	}

	@Transactional(readOnly = true)
	public List<Dto.NotificationView> recent(Profile me) {
		return notifications.findRecent(me, Limit.of(RECENT)).stream().map(Views::notification).toList();
	}

	public void markRead(Profile me, Long id) {
		Notification n = notifications.findById(id)
			.filter(found -> found.getRecipient().getId().equals(me.getId()))
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No such notification"));
		n.markRead();
	}

	public void markAllRead(Profile me) {
		notifications.markAllRead(me, Instant.now());
	}

}
