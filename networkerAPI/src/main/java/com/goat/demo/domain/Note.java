package com.goat.demo.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** A private note one person keeps about another. Only the author can ever see it. */
@Entity
@Table(name = "notes")
public class Note {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	private Profile author;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	private Profile subject;

	@Column(nullable = false, columnDefinition = "text")
	private String body;

	@Column(nullable = false)
	private Instant createdAt = Instant.now();

	@Column(nullable = false)
	private Instant updatedAt = Instant.now();

	protected Note() {
	}

	public Note(Profile author, Profile subject, String body) {
		this.author = author;
		this.subject = subject;
		this.body = body;
	}

	public Long getId() {
		return id;
	}

	public Profile getAuthor() {
		return author;
	}

	public Profile getSubject() {
		return subject;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
		this.updatedAt = Instant.now();
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

}
