package com.goat.demo.web;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.goat.demo.service.ProfileService;
import com.goat.demo.service.ResumeService;
import com.goat.demo.service.Views;
import com.goat.demo.web.Dto.Relation;

@RestController
@RequestMapping("/api")
public class ResumeController {

	private final ProfileService profiles;

	private final ResumeService resumes;

	public ResumeController(ProfileService profiles, ResumeService resumes) {
		this.profiles = profiles;
		this.resumes = resumes;
	}

	@PutMapping(path = "/me/resume", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public Dto.Person upload(@AuthenticationPrincipal Jwt jwt, @RequestParam("file") MultipartFile file)
			throws IOException {
		return Views.person(resumes.upload(profiles.current(jwt), file), Relation.SELF, null);
	}

	@DeleteMapping("/me/resume")
	public Dto.Person delete(@AuthenticationPrincipal Jwt jwt) {
		return Views.person(resumes.delete(profiles.current(jwt)), Relation.SELF, null);
	}

	@GetMapping("/people/{id}/resume")
	public ResponseEntity<byte[]> download(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
		ResumeService.File file = resumes.download(profiles.current(jwt), id);
		return ResponseEntity.ok()
			.contentType(MediaType.APPLICATION_PDF)
			.header(HttpHeaders.CONTENT_DISPOSITION,
					ContentDisposition.inline().filename(file.fileName(), StandardCharsets.UTF_8).build().toString())
			.body(file.data());
	}

}
