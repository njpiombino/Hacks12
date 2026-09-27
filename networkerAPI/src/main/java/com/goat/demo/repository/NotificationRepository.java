package com.goat.demo.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.goat.demo.domain.Notification;
import com.goat.demo.domain.Profile;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

	@Query("select n from Notification n join fetch n.actor where n.recipient = :recipient order by n.createdAt desc, n.id desc")
	List<Notification> findRecent(@Param("recipient") Profile recipient, Limit limit);

	@Modifying
	@Query("update Notification n set n.readAt = :now where n.recipient = :recipient and n.readAt is null")
	int markAllRead(@Param("recipient") Profile recipient, @Param("now") Instant now);

}
