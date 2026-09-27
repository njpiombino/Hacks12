package com.goat.demo.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.goat.demo.domain.Profile;

public interface ProfileRepository extends JpaRepository<Profile, UUID> {

	Optional<Profile> findByAuth0Id(String auth0Id);

	@Query("""
			select p from Profile p
			where p.id <> :excludeId
			  and (:q = '' or lower(p.name) like lower(concat('%', :q, '%'))
			              or lower(p.headline) like lower(concat('%', :q, '%'))
			              or lower(p.location) like lower(concat('%', :q, '%')))
			order by p.name
			""")
	List<Profile> search(@Param("q") String q, @Param("excludeId") UUID excludeId);

}
