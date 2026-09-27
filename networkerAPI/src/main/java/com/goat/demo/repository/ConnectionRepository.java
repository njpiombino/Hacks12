package com.goat.demo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.goat.demo.domain.Connection;
import com.goat.demo.domain.Profile;

public interface ConnectionRepository extends JpaRepository<Connection, Long> {

	@Query("""
			select c from Connection c
			join fetch c.requester join fetch c.addressee
			where c.requester = :me or c.addressee = :me
			order by c.createdAt desc
			""")
	List<Connection> findAllInvolving(@Param("me") Profile me);

	@Query("""
			select c from Connection c
			where (c.requester = :a and c.addressee = :b) or (c.requester = :b and c.addressee = :a)
			""")
	Optional<Connection> findBetween(@Param("a") Profile a, @Param("b") Profile b);

	@Modifying
	@Query("delete from Connection c where c.requester = :profile or c.addressee = :profile")
	int deleteInvolving(@Param("profile") Profile profile);

}
