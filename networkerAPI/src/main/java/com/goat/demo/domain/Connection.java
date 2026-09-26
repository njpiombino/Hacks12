package com.goat.demo.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "connections", uniqueConstraints = @UniqueConstraint(columnNames = { "requester_id", "addressee_id" }))
public class Connection {

	public enum Status {
		PENDING, ACCEPTED
	}

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	private Profile requester;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	private Profile addressee;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Status status = Status.PENDING;

	@Column(nullable = false)
	private Instant createdAt = Instant.now();

	private Instant acceptedAt;

	protected Connection() {
	}

	public Connection(Profile requester, Profile addressee) {
		this.requester = requester;
		this.addressee = addressee;
	}

	public void accept() {
		status = Status.ACCEPTED;
		acceptedAt = Instant.now();
	}

	public boolean involves(Profile profile) {
		return requester.getId().equals(profile.getId()) || addressee.getId().equals(profile.getId());
	}

	public Profile other(Profile me) {
		return requester.getId().equals(me.getId()) ? addressee : requester;
	}

	public Long getId() {
		return id;
	}

	public Profile getRequester() {
		return requester;
	}

	public Profile getAddressee() {
		return addressee;
	}

	public Status getStatus() {
		return status;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getAcceptedAt() {
		return acceptedAt;
	}

}
