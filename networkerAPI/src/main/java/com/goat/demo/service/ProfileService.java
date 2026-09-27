package com.goat.demo.service;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.goat.demo.config.DemoData;
import com.goat.demo.domain.Profile;
import com.goat.demo.repository.ProfileRepository;
import com.goat.demo.web.Dto;

@Service
public class ProfileService {

	private final ProfileRepository profiles;

	private final ObjectProvider<DemoData> demoData;

	public ProfileService(ProfileRepository profiles, ObjectProvider<DemoData> demoData) {
		this.profiles = profiles;
		this.demoData = demoData;
	}

	/** Returns the profile for the signed-in user, creating it on their first visit. */
	@Transactional
	public synchronized Profile current(Jwt jwt) {
		return profiles.findByAuth0Id(jwt.getSubject()).orElseGet(() -> {
			Profile created = profiles.save(new Profile(jwt.getSubject()));
			demoData.ifAvailable(d -> d.welcome(created));
			return created;
		});
	}

	public Profile get(UUID id) {
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
		me.setPortfolioUrl(blankToNull(update.portfolioUrl()));
		me.setInterests(cleanInterests(update.interests()));
		me.setHideLocation(update.hideLocation());
		me.setHideEmail(update.hideEmail());
		return profiles.save(me);
	}

	/** Trims, drops blanks/duplicates, and caps how many interests a profile can have. */
	private static List<String> cleanInterests(List<String> raw) {
		if (raw == null) {
			return List.of();
		}
		return raw.stream()
			.filter(s -> s != null && !s.isBlank())
			.map(s -> s.strip().length() > 40 ? s.strip().substring(0, 40) : s.strip())
			.distinct()
			.limit(15)
			.toList();
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
