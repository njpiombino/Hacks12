package com.goat.demo.service;

import java.util.List;

import org.springframework.data.domain.Limit;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.goat.demo.domain.Note;
import com.goat.demo.domain.Profile;
import com.goat.demo.repository.NoteRepository;
import com.goat.demo.web.Dto;

@Service
@Transactional
public class NoteService {

	private final NoteRepository notes;

	private final ProfileService profiles;

	public NoteService(NoteRepository notes, ProfileService profiles) {
		this.notes = notes;
		this.profiles = profiles;
	}

	@Transactional(readOnly = true)
	public List<Dto.NoteView> about(Profile me, Long subjectId) {
		return notes.findByAuthorAndSubjectOrderByCreatedAtDesc(me, profiles.get(subjectId))
			.stream()
			.map(Views::note)
			.toList();
	}

	@Transactional(readOnly = true)
	public List<Dto.NoteView> recent(Profile me, int limit) {
		return notes.findRecent(me, Limit.of(limit)).stream().map(Views::note).toList();
	}

	public Dto.NoteView create(Profile me, Long subjectId, Dto.NoteRequest request) {
		Profile subject = profiles.get(subjectId);
		if (subject.getId().equals(me.getId())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Notes are for other people");
		}
		return Views.note(notes.save(new Note(me, subject, request.body().strip())));
	}

	public Dto.NoteView update(Profile me, Long noteId, Dto.NoteRequest request) {
		Note note = find(me, noteId);
		note.setBody(request.body().strip());
		return Views.note(note);
	}

	public void delete(Profile me, Long noteId) {
		notes.delete(find(me, noteId));
	}

	private Note find(Profile me, Long id) {
		return notes.findById(id)
			.filter(n -> n.getAuthor().getId().equals(me.getId()))
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No such note"));
	}

}
