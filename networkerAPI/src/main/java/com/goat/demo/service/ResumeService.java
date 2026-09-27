package com.goat.demo.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.goat.demo.domain.Profile;
import com.goat.demo.domain.Resume;
import com.goat.demo.repository.ProfileRepository;
import com.goat.demo.repository.ResumeRepository;

/** One PDF resume per person, visible to them and their connections. */
@Service
@Transactional
public class ResumeService {

	public static final int MAX_BYTES = 5 * 1024 * 1024;

	private static final byte[] PDF_MAGIC = "%PDF-".getBytes(StandardCharsets.US_ASCII);

	private final ResumeRepository resumes;

	private final ProfileRepository profileRepository;

	private final ProfileService profiles;

	private final ConnectionService connections;

	public ResumeService(ResumeRepository resumes, ProfileRepository profileRepository, ProfileService profiles,
			ConnectionService connections) {
		this.resumes = resumes;
		this.profileRepository = profileRepository;
		this.profiles = profiles;
		this.connections = connections;
	}

	public record File(String fileName, byte[] data) {
	}

	public Profile upload(Profile me, MultipartFile file) throws IOException {
		byte[] data = file.getBytes();
		if (data.length == 0) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "That file is empty");
		}
		if (data.length > MAX_BYTES) {
			throw new ResponseStatusException(HttpStatus.CONTENT_TOO_LARGE, "Resumes can be up to 5 MB");
		}
		// Check the file itself, not the name or the browser's say-so.
		if (data.length < PDF_MAGIC.length || !Arrays.equals(data, 0, PDF_MAGIC.length, PDF_MAGIC, 0, PDF_MAGIC.length)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Resumes need to be PDFs");
		}

		Resume resume = resumes.findById(me.getId()).orElseGet(() -> new Resume(me.getId(), data));
		resume.setData(data);
		resumes.save(resume);
		me.setResume(cleanName(file.getOriginalFilename()), data.length);
		return profileRepository.save(me);
	}

	public Profile delete(Profile me) {
		resumes.deleteById(me.getId());
		me.clearResume();
		return profileRepository.save(me);
	}

	/** The resume of {@code ownerId}, if {@code me} is allowed to see it: it's theirs, or they're connected. */
	@Transactional(readOnly = true)
	public File download(Profile me, UUID ownerId) {
		Profile owner = profiles.get(ownerId);
		boolean allowed = owner.getId().equals(me.getId()) || connections.areConnected(me, owner);
		if (!allowed || !owner.hasResume()) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No resume to show");
		}
		Resume resume = resumes.findById(owner.getId())
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No resume to show"));
		return new File(owner.getResumeFileName(), resume.getData());
	}

	/** Just the file's own name, never a path, ending in .pdf. */
	private static String cleanName(String original) {
		String name = original == null ? "" : original.replaceAll(".*[/\\\\]", "").strip();
		if (name.isEmpty()) {
			name = "resume.pdf";
		}
		if (!name.toLowerCase().endsWith(".pdf")) {
			name += ".pdf";
		}
		return name.length() > 200 ? name.substring(name.length() - 200) : name;
	}

}
