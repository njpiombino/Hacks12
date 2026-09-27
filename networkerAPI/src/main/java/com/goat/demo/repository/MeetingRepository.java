package com.goat.demo.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.goat.demo.domain.Meeting;
import com.goat.demo.domain.Profile;

/** "Involved" means organizing the meeting or being invited to it. */
public interface MeetingRepository extends JpaRepository<Meeting, Long> {

	@Query("""
			select distinct m from Meeting m
			join fetch m.owner left join fetch m.attendees at left join fetch at.profile
			where (m.owner = :me or exists (select 1 from MeetingAttendee a where a.meeting = m and a.profile = :me))
			  and m.startsAt < :to and m.endsAt > :from
			order by m.startsAt
			""")
	List<Meeting> findOverlapping(@Param("me") Profile me, @Param("from") Instant from, @Param("to") Instant to);

	@Query("""
			select distinct m from Meeting m
			join fetch m.owner left join fetch m.attendees at left join fetch at.profile
			where (m.owner = :me or exists (select 1 from MeetingAttendee a where a.meeting = m and a.profile = :me))
			  and (m.owner = :other or exists (select 1 from MeetingAttendee a where a.meeting = m and a.profile = :other))
			order by m.startsAt desc
			""")
	List<Meeting> findWith(@Param("me") Profile me, @Param("other") Profile other);

	@Query("""
			select distinct m from Meeting m
			join fetch m.owner left join fetch m.attendees at left join fetch at.profile
			where exists (select 1 from MeetingAttendee a where a.meeting = m and a.profile = :me
			                and a.status = com.goat.demo.domain.Meeting.InviteStatus.PENDING)
			  and m.endsAt > :now
			order by m.startsAt
			""")
	List<Meeting> findPendingInvitations(@Param("me") Profile me, @Param("now") Instant now);

	@Query("""
			select distinct m from Meeting m
			join fetch m.owner left join fetch m.attendees at left join fetch at.profile
			where (m.owner = :me or exists (select 1 from MeetingAttendee a where a.meeting = m and a.profile = :me))
			  and m.endsAt > :now
			""")
	List<Meeting> findUpcomingInvolving(@Param("me") Profile me, @Param("now") Instant now);

	/** Their invitations, plus every attendee row on meetings they organize. */
	@Modifying
	@Query("""
			delete from MeetingAttendee a
			where a.profile = :profile or a.meeting in (select m from Meeting m where m.owner = :profile)
			""")
	int deleteAttendeesInvolving(@Param("profile") Profile profile);

	@Modifying
	@Query("delete from Meeting m where m.owner = :profile")
	int deleteOwnedBy(@Param("profile") Profile profile);

}
