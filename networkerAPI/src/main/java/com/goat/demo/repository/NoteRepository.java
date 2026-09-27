package com.goat.demo.repository;

import java.util.List;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.goat.demo.domain.Note;
import com.goat.demo.domain.Profile;

public interface NoteRepository extends JpaRepository<Note, Long> {

	List<Note> findByAuthorAndSubjectOrderByCreatedAtDesc(Profile author, Profile subject);

	@Query("select n from Note n join fetch n.subject where n.author = :author order by n.updatedAt desc")
	List<Note> findRecent(@Param("author") Profile author, Limit limit);

	/** Notes the profile wrote, and notes other people wrote about them. */
	@Modifying
	@Query("delete from Note n where n.author = :profile or n.subject = :profile")
	int deleteInvolving(@Param("profile") Profile profile);

}
