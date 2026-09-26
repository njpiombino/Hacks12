package com.goat.demo.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "profiles")
public class Profile {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** The Auth0 subject ("sub" claim) this profile belongs to. */
	@Column(nullable = false, unique = true)
	private String auth0Id;

	private String name;

	private String headline;

	private String location;

	@Column(length = 2000)
	private String bio;

	private String email;

	@Column(length = 1000)
	private String pictureUrl;

	@Column(nullable = false)
	private Instant createdAt = Instant.now();

	protected Profile() {
	}

	public Profile(String auth0Id) {
		this.auth0Id = auth0Id;
	}

	public boolean isDemo() {
		return auth0Id.startsWith("demo|");
	}

	public Long getId() {
		return id;
	}

	public String getAuth0Id() {
		return auth0Id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getHeadline() {
		return headline;
	}

	public void setHeadline(String headline) {
		this.headline = headline;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public String getBio() {
		return bio;
	}

	public void setBio(String bio) {
		this.bio = bio;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPictureUrl() {
		return pictureUrl;
	}

	public void setPictureUrl(String pictureUrl) {
		this.pictureUrl = pictureUrl;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

}
