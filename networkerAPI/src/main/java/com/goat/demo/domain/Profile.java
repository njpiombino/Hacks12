package com.goat.demo.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

@Entity
@Table(name = "profiles")
public class Profile {

	@Id
	@GeneratedValue
	private UUID id;

	/** The Auth0 subject ("sub" claim) this profile belongs to. */
	@Column(nullable = false, unique = true)
	private String auth0Id;

	private String name;

	private String headline;

	private String location;

	@Column(length = 2000)
	private String bio;

	private String email;

	/** A URL, or a data: URI for an uploaded photo. */
	@Column(columnDefinition = "text")
	private String pictureUrl;

	/** A GitHub or personal portfolio link. */
	@Column(length = 300)
	private String portfolioUrl;

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "profile_interests", joinColumns = @JoinColumn(name = "profile_id"))
	@Column(name = "interest", length = 40)
	private List<String> interests = new ArrayList<>();

	/** When true, location is hidden from everyone but yourself. */
	@Column(nullable = false)
	private boolean hideLocation = false;

	/** When true, email is hidden even from accepted connections. */
	@Column(nullable = false)
	private boolean hideEmail = false;

	/** Details of the uploaded resume; the file itself lives in {@link Resume}. Null when there isn't one. */
	private String resumeFileName;

	private Integer resumeSize;

	private Instant resumeUploadedAt;

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

	public UUID getId() {
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

	public String getPortfolioUrl() {
		return portfolioUrl;
	}

	public void setPortfolioUrl(String portfolioUrl) {
		this.portfolioUrl = portfolioUrl;
	}

	public List<String> getInterests() {
		return interests;
	}

	public void setInterests(List<String> interests) {
		this.interests = interests;
	}

	public boolean isHideLocation() {
		return hideLocation;
	}

	public void setHideLocation(boolean hideLocation) {
		this.hideLocation = hideLocation;
	}

	public boolean isHideEmail() {
		return hideEmail;
	}

	public void setHideEmail(boolean hideEmail) {
		this.hideEmail = hideEmail;
	}

	public boolean hasResume() {
		return resumeFileName != null;
	}

	public String getResumeFileName() {
		return resumeFileName;
	}

	public Integer getResumeSize() {
		return resumeSize;
	}

	public Instant getResumeUploadedAt() {
		return resumeUploadedAt;
	}

	public void setResume(String fileName, int size) {
		this.resumeFileName = fileName;
		this.resumeSize = size;
		this.resumeUploadedAt = Instant.now();
	}

	public void clearResume() {
		this.resumeFileName = null;
		this.resumeSize = null;
		this.resumeUploadedAt = null;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

}
