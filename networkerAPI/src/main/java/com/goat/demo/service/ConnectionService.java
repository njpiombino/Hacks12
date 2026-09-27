package com.goat.demo.service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.goat.demo.domain.Connection;
import com.goat.demo.domain.Profile;
import com.goat.demo.repository.ConnectionRepository;
import com.goat.demo.web.Dto;
import com.goat.demo.web.Dto.Relation;

@Service
public class ConnectionService {

	private final ConnectionRepository connections;

	public ConnectionService(ConnectionRepository connections) {
		this.connections = connections;
	}

	@Transactional(readOnly = true)
	public Dto.Connections overview(Profile me) {
		List<Connection> all = connections.findAllInvolving(me);
		var connected = all.stream()
			.filter(c -> c.getStatus() == Connection.Status.ACCEPTED)
			.map(c -> item(me, c, c.getAcceptedAt()))
			.toList();
		var incoming = all.stream()
			.filter(c -> c.getStatus() == Connection.Status.PENDING && c.getAddressee().getId().equals(me.getId()))
			.map(c -> item(me, c, c.getCreatedAt()))
			.toList();
		var outgoing = all.stream()
			.filter(c -> c.getStatus() == Connection.Status.PENDING && c.getRequester().getId().equals(me.getId()))
			.map(c -> item(me, c, c.getCreatedAt()))
			.toList();
		return new Dto.Connections(connected, incoming, outgoing);
	}

	/** Maps every person the user has any connection with to that connection, for relation lookups. */
	@Transactional(readOnly = true)
	public Map<UUID, Connection> byOtherPerson(Profile me) {
		Map<UUID, Connection> map = new HashMap<>();
		for (Connection c : connections.findAllInvolving(me)) {
			map.put(c.other(me).getId(), c);
		}
		return map;
	}

	@Transactional(readOnly = true)
	public boolean areConnected(Profile a, Profile b) {
		return connections.findBetween(a, b).map(c -> c.getStatus() == Connection.Status.ACCEPTED).orElse(false);
	}

	@Transactional
	public Connection request(Profile me, Profile other) {
		if (me.getId().equals(other.getId())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You can't connect with yourself");
		}
		var existing = connections.findBetween(me, other);
		if (existing.isPresent()) {
			Connection c = existing.get();
			// They already asked us: treat our request as saying yes.
			if (c.getStatus() == Connection.Status.PENDING && c.getAddressee().getId().equals(me.getId())) {
				c.accept();
			}
			return c;
		}
		return connections.save(new Connection(me, other));
	}

	@Transactional
	public Connection accept(Profile me, Long connectionId) {
		Connection c = find(me, connectionId);
		if (!c.getAddressee().getId().equals(me.getId())) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the invited person can accept");
		}
		c.accept();
		return c;
	}

	/** Declines, withdraws, or removes a connection, depending on its state. */
	@Transactional
	public void remove(Profile me, Long connectionId) {
		connections.delete(find(me, connectionId));
	}

	public static Relation relation(Profile me, Connection c) {
		if (c == null) {
			return Relation.NONE;
		}
		if (c.getStatus() == Connection.Status.ACCEPTED) {
			return Relation.CONNECTED;
		}
		return c.getRequester().getId().equals(me.getId()) ? Relation.OUTGOING : Relation.INCOMING;
	}

	private Connection find(Profile me, Long id) {
		return connections.findById(id)
			.filter(c -> c.involves(me))
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No such connection"));
	}

	private Dto.ConnectionItem item(Profile me, Connection c, Instant since) {
		return new Dto.ConnectionItem(c.getId(), Views.person(c.other(me), relation(me, c), c.getId()), since);
	}

}
