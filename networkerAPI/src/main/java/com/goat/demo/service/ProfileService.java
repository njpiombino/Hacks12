package com.goat.demo.service;

import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.goat.demo.domain.Profile;
import com.goat.demo.repository.ProfileRepository;
import com.goat.demo.web.Dto;

@Service
public class ProfileService {

	private final ProfileRepository profiles;

	public ProfileService(ProfileRepository profiles) {
		this.profiles = profiles;
	}

	/** Returns the profile for the signed-in user, creating it on their first visit. */
	@Transactional
	public synchronized Profile current(Jwt jwt) {
		return profiles.findByAuth0Id(jwt.getSubject()).orElseGet(() -> profiles.save(new Profile(jwt.getSubject())));
	}

	public Profile get(Long id) {
		return profiles.findById(id)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No such person"));
	}

	@Transactional
	public Profile update(Profile me, Dto.ProfileUpdate update) {
		me.setName(update.name().strip());
		me.setHeadline(blankToNull(update.headline()));
		me.setLocation(blankToNull(update.location()));
		me.setBio(blankToNull(update.bio()));
		me.setPictureUrl(blankToNull(update.pictureUrl()));
		return profiles.save(me);
	}

	/** Fills in any empty fields from the identity provider, without overwriting what the user wrote. */
	@Transactional
	public Profile sync(Profile me, Dto.ProfileSync sync) {
		if (isBlank(me.getName())) {
			me.setName(blankToNull(sync.name()));
		}
		if (isBlank(me.getEmail())) {
			me.setEmail(blankToNull(sync.email()));
		}
		if (isBlank(me.getPictureUrl())) {
			me.setPictureUrl(blankToNull(sync.pictureUrl()));
		}
		return profiles.save(me);
	}

	private static boolean isBlank(String s) {
		return s == null || s.isBlank();
	}

	private static String blankToNull(String s) {
		return isBlank(s) ? null : s.strip();
	}

}
