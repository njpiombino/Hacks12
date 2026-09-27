package com.goat.demo;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.goat.demo.repository.ProfileRepository;
import com.jayway.jsonpath.JsonPath;

@SpringBootTest(properties = { "spring.datasource.url=jdbc:h2:mem:flow;MODE=PostgreSQL",
		"spring.datasource.username=sa", "spring.datasource.password=" })
@AutoConfigureMockMvc
class ApiFlowTests {

	@Autowired
	MockMvc mvc;

	@Autowired
	ProfileRepository profiles;

	private static MockHttpServletRequestBuilder as(String sub, MockHttpServletRequestBuilder request) {
		return request.with(jwt().jwt(j -> j.subject(sub))).contentType(MediaType.APPLICATION_JSON);
	}

	@Test
	void rejectsAnonymousRequests() throws Exception {
		mvc.perform(get("/api/me")).andExpect(status().isUnauthorized());
	}

	@Test
	void connectNoteAndMeet() throws Exception {
		mvc.perform(as("auth0|alice", post("/api/me/sync")).content("""
				{"name":"Alice Park","email":"alice@example.com"}"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.name").value("Alice Park"))
			.andExpect(jsonPath("$.relation").value("SELF"));

		UUID maya = profiles.findByAuth0Id("demo|1").orElseThrow().getId();
		UUID tom = profiles.findByAuth0Id("demo|4").orElseThrow().getId();

		mvc.perform(as("auth0|alice", get("/api/people").param("q", "maya")))
			.andExpect(jsonPath("$", hasSize(1)))
			.andExpect(jsonPath("$[0].relation").value("NONE"))
			.andExpect(jsonPath("$[0].email").value(nullValue()));

		// Requests wait for the other person, even sample people.
		mvc.perform(as("auth0|alice", post("/api/connections")).content("{\"profileId\":\"" + maya + "\"}"))
			.andExpect(jsonPath("$.outgoing", hasSize(1)))
			.andExpect(jsonPath("$.connected", hasSize(0)));

		// Notes work on anyone, connected or not.
		mvc.perform(as("auth0|alice", post("/api/people/" + maya + "/notes")).content("""
				{"body":"Training for a half marathon"}"""))
			.andExpect(status().isOk());

		// Meetings don't.
		mvc.perform(as("auth0|alice", post("/api/meetings")).content("""
				{"title":"Coffee","startsAt":"2030-01-02T15:00:00Z","endsAt":"2030-01-02T15:30:00Z","attendeeId":"%s"}"""
			.formatted(maya)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.detail").value("You can only invite your connections"));

		// Two sample people reach out to newcomers; accepting makes a connection.
		String body = mvc.perform(as("auth0|alice", get("/api/connections")))
			.andExpect(jsonPath("$.incoming", hasSize(2)))
			.andReturn()
			.getResponse()
			.getContentAsString();
		mvc.perform(as("auth0|alice", post("/api/connections/" + incomingFrom(body, "Tom Whitaker") + "/accept")))
			.andExpect(jsonPath("$.connected", hasSize(1)))
			.andExpect(jsonPath("$.connected[0].person.email").value("tom.whitaker@example.com"));

		// Find people leaves out connections but keeps pending requests.
		mvc.perform(as("auth0|alice", get("/api/people")))
			.andExpect(jsonPath("$[?(@.name == 'Tom Whitaker')]", hasSize(0)))
			.andExpect(jsonPath("$[?(@.name == 'Maya Okafor')].relation").value("OUTGOING"))
			.andExpect(jsonPath("$[?(@.name == 'Hana Kim')].relation").value("INCOMING"));

		mvc.perform(as("auth0|alice", post("/api/meetings")).content("""
				{"title":"Coffee","startsAt":"2030-01-02T15:00:00Z","endsAt":"2030-01-02T15:30:00Z","attendeeId":"%s"}"""
			.formatted(tom))).andExpect(jsonPath("$.with.name").value("Tom Whitaker"));

		mvc.perform(as("auth0|alice", get("/api/meetings").param("from", "2030-01-01T00:00:00Z")
			.param("to", "2030-02-01T00:00:00Z"))).andExpect(jsonPath("$", hasSize(1)));

		// Notes are private to their author.
		mvc.perform(as("auth0|bob", get("/api/people/" + maya + "/notes"))).andExpect(jsonPath("$", hasSize(0)));
		mvc.perform(as("auth0|alice", get("/api/notes/recent"))).andExpect(jsonPath("$", hasSize(1)));
	}

	@Test
	void peopleCanRequestAndAccept() throws Exception {
		mvc.perform(as("auth0|carol", post("/api/me/sync")).content("{\"name\":\"Carol\"}"));
		mvc.perform(as("auth0|dave", post("/api/me/sync")).content("{\"name\":\"Dave\"}"));
		UUID dave = profiles.findByAuth0Id("auth0|dave").orElseThrow().getId();

		mvc.perform(as("auth0|carol", post("/api/connections")).content("{\"profileId\":\"" + dave + "\"}"))
			.andExpect(jsonPath("$.outgoing", hasSize(1)));

		// Not connected until Dave says yes.
		mvc.perform(as("auth0|carol", post("/api/meetings")).content("""
				{"title":"Lunch","startsAt":"2030-01-02T15:00:00Z","endsAt":"2030-01-02T16:00:00Z","attendeeId":"%s"}"""
			.formatted(dave))).andExpect(status().isBadRequest());

		String body = mvc.perform(as("auth0|dave", get("/api/connections")))
			.andReturn()
			.getResponse()
			.getContentAsString();

		mvc.perform(as("auth0|dave", post("/api/connections/" + incomingFrom(body, "Carol") + "/accept")))
			.andExpect(jsonPath("$.connected", hasSize(1)))
			.andExpect(jsonPath("$.connected[0].person.name").value("Carol"));

		mvc.perform(as("auth0|carol", post("/api/meetings")).content("""
				{"title":"Lunch","startsAt":"2030-01-02T15:00:00Z","endsAt":"2030-01-02T16:00:00Z","attendeeId":"%s"}"""
			.formatted(dave))).andExpect(status().isOk());
	}

	private static Object incomingFrom(String connectionsJson, String name) {
		List<Object> ids = JsonPath.read(connectionsJson, "$.incoming[?(@.person.name == '" + name + "')].id");
		return ids.getFirst();
	}

}
