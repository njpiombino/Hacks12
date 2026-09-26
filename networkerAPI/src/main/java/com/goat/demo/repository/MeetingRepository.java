package com.goat.demo.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.goat.demo.domain.Meeting;
import com.goat.demo.domain.Profile;

public interface MeetingRepository extends JpaRepository<Meeting, Long> {

	@Query("""
			select m from Meeting m
			join fetch m.owner left join fetch m.attendee
			where (m.owner = :me or m.attendee = :me)
			  and m.startsAt < :to and m.endsAt > :from
			order by m.startsAt
			""")
	List<Meeting> findOverlapping(@Param("me") Profile me, @Param("from") Instant from, @Param("to") Instant to);

	@Query("""
			select m from Meeting m
			join fetch m.owner left join fetch m.attendee
			where (m.owner = :me or m.attendee = :me)
			  and (m.owner = :other or m.attendee = :other)
			order by m.startsAt desc
			""")
	List<Meeting> findWith(@Param("me") Profile me, @Param("other") Profile other);

}
