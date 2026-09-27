package com.goat.demo.domain;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * The PDF behind a profile's resume. Kept out of {@link Profile} so loading people never pulls the file along;
 * the file name, size and upload time live on the profile.
 */
@Entity
@Table(name = "resumes")
public class Resume {

	@Id
	private UUID profileId;

	@Column(nullable = false)
	private byte[] data;

	protected Resume() {
	}

	public Resume(UUID profileId, byte[] data) {
		this.profileId = profileId;
		this.data = data;
	}

	public UUID getProfileId() {
		return profileId;
	}

	public byte[] getData() {
		return data;
	}

	public void setData(byte[] data) {
		this.data = data;
	}

}
