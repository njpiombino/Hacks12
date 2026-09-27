package com.goat.demo;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
				{"title":"Coffee","startsAt":"2030-01-02T15:00:00Z","endsAt":"2030-01-02T15:30:00Z","attendeeIds":["%s"]}"""
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
				{"title":"Coffee","startsAt":"2030-01-02T15:00:00Z","endsAt":"2030-01-02T15:30:00Z","attendeeIds":["%s"]}"""
			.formatted(tom))).andExpect(jsonPath("$.attendees[0].person.name").value("Tom Whitaker"));

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
				{"title":"Lunch","startsAt":"2030-01-02T15:00:00Z","endsAt":"2030-01-02T16:00:00Z","attendeeIds":["%s"]}"""
			.formatted(dave))).andExpect(status().isBadRequest());

		String body = mvc.perform(as("auth0|dave", get("/api/connections")))
			.andReturn()
			.getResponse()
			.getContentAsString();

		mvc.perform(as("auth0|dave", post("/api/connections/" + incomingFrom(body, "Carol") + "/accept")))
			.andExpect(jsonPath("$.connected", hasSize(1)))
			.andExpect(jsonPath("$.connected[0].person.name").value("Carol"));

		String lunch = mvc.perform(as("auth0|carol", post("/api/meetings")).content("""
				{"title":"Lunch","startsAt":"2030-01-02T15:00:00Z","endsAt":"2030-01-02T16:00:00Z","attendeeIds":["%s"]}"""
			.formatted(dave)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.attendees[0].status").value("PENDING"))
			.andReturn()
			.getResponse()
			.getContentAsString();
		Object lunchId = JsonPath.read(lunch, "$.id");

		// Dave has to answer; Carol can't answer for him.
		mvc.perform(as("auth0|dave", get("/api/meetings/invitations")))
			.andExpect(jsonPath("$", hasSize(1)))
			.andExpect(jsonPath("$[0].organizer.name").value("Carol"));
		mvc.perform(as("auth0|carol", post("/api/meetings/" + lunchId + "/accept"))).andExpect(status().isForbidden());
		mvc.perform(as("auth0|dave", get("/api/notifications")))
			.andExpect(jsonPath("$", hasSize(1)))
			.andExpect(jsonPath("$[0].type").value("INVITED"))
			.andExpect(jsonPath("$[0].actor.name").value("Carol"))
			.andExpect(jsonPath("$[0].read").value(false));

		mvc.perform(as("auth0|dave", post("/api/meetings/" + lunchId + "/accept")))
			.andExpect(jsonPath("$.attendees[0].status").value("ACCEPTED"));
		mvc.perform(as("auth0|dave", get("/api/meetings/invitations"))).andExpect(jsonPath("$", hasSize(0)));
		mvc.perform(as("auth0|carol", get("/api/notifications"))).andExpect(jsonPath("$[0].type").value("ACCEPTED"));

		// Moving the meeting asks Dave again, and tells him what changed.
		mvc.perform(as("auth0|carol", put("/api/meetings/" + lunchId)).content("""
				{"title":"Lunch","startsAt":"2030-01-03T15:00:00Z","endsAt":"2030-01-03T16:00:00Z","attendeeIds":["%s"]}"""
			.formatted(dave))).andExpect(jsonPath("$.attendees[0].status").value("PENDING"));
		mvc.perform(as("auth0|dave", get("/api/notifications")))
			.andExpect(jsonPath("$[0].type").value("UPDATED"))
			.andExpect(jsonPath("$[0].changes", hasSize(1)))
			.andExpect(jsonPath("$[0].changes[0]").value("TIME"))
			.andExpect(jsonPath("$[0].startsAt").value("2030-01-03T15:00:00Z"));

		mvc.perform(as("auth0|carol", put("/api/meetings/" + lunchId)).content("""
				{"title":"Brunch","startsAt":"2030-01-03T15:00:00Z","endsAt":"2030-01-03T16:00:00Z","location":"The Hive","attendeeIds":["%s"]}"""
			.formatted(dave)));
		mvc.perform(as("auth0|dave", get("/api/notifications")))
			.andExpect(jsonPath("$[0].changes", hasSize(2)))
			.andExpect(jsonPath("$[0].changes[0]").value("PLACE"))
			.andExpect(jsonPath("$[0].changes[1]").value("TITLE"))
			.andExpect(jsonPath("$[0].title").value("Brunch"))
			.andExpect(jsonPath("$[0].previousTitle").value("Lunch"))
			.andExpect(jsonPath("$[0].location").value("The Hive"));

		// Reading them clears the unread state.
		mvc.perform(as("auth0|dave", post("/api/notifications/read"))).andExpect(status().isOk());
		mvc.perform(as("auth0|dave", get("/api/notifications")))
			.andExpect(jsonPath("$[?(@.read == false)]", hasSize(0)));

		// Declining takes it off both calendars, and Carol hears about it.
		mvc.perform(as("auth0|dave", post("/api/meetings/" + lunchId + "/decline"))).andExpect(status().isOk());
		for (String who : List.of("auth0|carol", "auth0|dave")) {
			mvc.perform(as(who, get("/api/meetings").param("from", "2030-01-01T00:00:00Z")
				.param("to", "2030-02-01T00:00:00Z"))).andExpect(jsonPath("$", hasSize(0)));
		}
		mvc.perform(as("auth0|carol", get("/api/notifications")))
			.andExpect(jsonPath("$[0].type").value("DECLINED"))
			.andExpect(jsonPath("$[0].title").value("Brunch"));

		// The organizer cancelling tells the invitee.
		String dinner = mvc.perform(as("auth0|carol", post("/api/meetings")).content("""
				{"title":"Dinner","startsAt":"2030-01-05T23:00:00Z","endsAt":"2030-01-06T01:00:00Z","attendeeIds":["%s"]}"""
			.formatted(dave))).andReturn().getResponse().getContentAsString();
		mvc.perform(as("auth0|carol", delete("/api/meetings/" + JsonPath.read(dinner, "$.id"))))
			.andExpect(status().isOk());
		mvc.perform(as("auth0|dave", get("/api/notifications")))
			.andExpect(jsonPath("$[0].type").value("CANCELLED"))
			.andExpect(jsonPath("$[0].title").value("Dinner"))
			.andExpect(jsonPath("$[1].type").value("INVITED"));

		// Nobody else can see or touch them.
		mvc.perform(as("auth0|carol", get("/api/notifications")))
			.andExpect(jsonPath("$[?(@.title == 'Dinner')]", hasSize(0)));
	}

	@Test
	void groupMeetings() throws Exception {
		UUID erin = signUp("auth0|erin", "Erin");
		UUID frank = signUp("auth0|frank", "Frank");
		UUID gina = signUp("auth0|gina", "Gina");
		connect("auth0|erin", "Frank", frank);
		connect("auth0|erin", "Gina", gina);

		mvc.perform(as("auth0|erin", post("/api/meetings")).content("""
				{"title":"Standup","startsAt":"2030-03-02T15:00:00Z","endsAt":"2030-03-02T15:30:00Z","attendeeIds":["%s"]}"""
			.formatted(erin))).andExpect(status().isBadRequest());

		String standup = mvc.perform(as("auth0|erin", post("/api/meetings")).content("""
				{"title":"Standup","startsAt":"2030-03-02T15:00:00Z","endsAt":"2030-03-02T15:30:00Z","attendeeIds":["%s","%s"]}"""
			.formatted(frank, gina)))
			.andExpect(jsonPath("$.attendees", hasSize(2)))
			.andExpect(jsonPath("$.attendees[?(@.status == 'PENDING')]", hasSize(2)))
			.andExpect(jsonPath("$.myStatus").value(nullValue()))
			.andReturn()
			.getResponse()
			.getContentAsString();
		Object id = JsonPath.read(standup, "$.id");

		// Each invitee sees the whole group and answers for themselves.
		mvc.perform(as("auth0|gina", get("/api/notifications"))).andExpect(jsonPath("$[0].type").value("INVITED"));
		mvc.perform(as("auth0|frank", post("/api/meetings/" + id + "/accept")))
			.andExpect(jsonPath("$.organizer.name").value("Erin"))
			.andExpect(jsonPath("$.myStatus").value("ACCEPTED"))
			.andExpect(jsonPath("$.attendees[?(@.person.name == 'Gina')].status").value("PENDING"));

		// One of several declining just drops them; it stays on everyone else's calendar.
		mvc.perform(as("auth0|gina", post("/api/meetings/" + id + "/decline"))).andExpect(status().isOk());
		mvc.perform(as("auth0|erin", get("/api/notifications"))).andExpect(jsonPath("$[0].type").value("DROPPED_OUT"));
		mvc.perform(as("auth0|frank", get("/api/meetings").param("from", "2030-03-01T00:00:00Z")
			.param("to", "2030-04-01T00:00:00Z")))
			.andExpect(jsonPath("$", hasSize(1)))
			.andExpect(jsonPath("$[0].attendees", hasSize(1)));
		mvc.perform(as("auth0|gina", get("/api/meetings").param("from", "2030-03-01T00:00:00Z")
			.param("to", "2030-04-01T00:00:00Z"))).andExpect(jsonPath("$", hasSize(0)));

		// Renaming it and re-inviting Gina: Frank hears about the rename and keeps his yes, Gina is invited again.
		mvc.perform(as("auth0|erin", put("/api/meetings/" + id)).content("""
				{"title":"Planning","startsAt":"2030-03-02T15:00:00Z","endsAt":"2030-03-02T15:30:00Z","attendeeIds":["%s","%s"]}"""
			.formatted(frank, gina)))
			.andExpect(jsonPath("$.attendees[?(@.person.name == 'Frank')].status").value("ACCEPTED"))
			.andExpect(jsonPath("$.attendees[?(@.person.name == 'Gina')].status").value("PENDING"));
		mvc.perform(as("auth0|frank", get("/api/notifications")))
			.andExpect(jsonPath("$[0].type").value("UPDATED"))
			.andExpect(jsonPath("$[0].changes[0]").value("TITLE"));
		mvc.perform(as("auth0|gina", get("/api/notifications"))).andExpect(jsonPath("$[0].type").value("INVITED"));

		// Taking Gina off tells her it's cancelled for her.
		mvc.perform(as("auth0|erin", put("/api/meetings/" + id)).content("""
				{"title":"Planning","startsAt":"2030-03-02T15:00:00Z","endsAt":"2030-03-02T15:30:00Z","attendeeIds":["%s"]}"""
			.formatted(frank))).andExpect(jsonPath("$.attendees", hasSize(1)));
		mvc.perform(as("auth0|gina", get("/api/notifications"))).andExpect(jsonPath("$[0].type").value("CANCELLED"));

		// When the last attendee declines, the meeting is cancelled for the organizer too.
		mvc.perform(as("auth0|frank", post("/api/meetings/" + id + "/decline"))).andExpect(status().isOk());
		mvc.perform(as("auth0|erin", get("/api/notifications"))).andExpect(jsonPath("$[0].type").value("DECLINED"));
		mvc.perform(as("auth0|erin", get("/api/meetings").param("from", "2030-03-01T00:00:00Z")
			.param("to", "2030-04-01T00:00:00Z"))).andExpect(jsonPath("$", hasSize(0)));
	}

	@Test
	void deletingAnAccountCancelsMeetingsAndRemovesEverythingElse() throws Exception {
		UUID helen = signUp("auth0|helen", "Helen");
		UUID ivan = signUp("auth0|ivan", "Ivan");
		connect("auth0|helen", "Ivan", ivan);
		mvc.perform(as("auth0|helen", post("/api/people/" + ivan + "/notes")).content("{\"body\":\"Met at the career fair\"}"));
		mvc.perform(as("auth0|ivan", post("/api/people/" + helen + "/notes")).content("{\"body\":\"Knows Kafka\"}"));
		mvc.perform(as("auth0|helen", post("/api/meetings")).content("""
				{"title":"Coffee","startsAt":"2030-05-02T15:00:00Z","endsAt":"2030-05-02T15:30:00Z","attendeeIds":["%s"]}"""
			.formatted(ivan))).andExpect(status().isOk());
		mvc.perform(as("auth0|helen", post("/api/meetings")).content("""
				{"title":"Old chat","startsAt":"2020-05-02T15:00:00Z","endsAt":"2020-05-02T15:30:00Z","attendeeIds":["%s"]}"""
			.formatted(ivan))).andExpect(status().isOk());
		mvc.perform(as("auth0|ivan", post("/api/meetings")).content("""
				{"title":"Lunch","startsAt":"2030-05-03T15:00:00Z","endsAt":"2030-05-03T16:00:00Z","attendeeIds":["%s"]}"""
			.formatted(helen))).andExpect(status().isOk());

		mvc.perform(as("auth0|helen", delete("/api/me"))).andExpect(status().isNoContent());
		assertTrue(profiles.findById(helen).isEmpty());

		// Ivan hears that her meeting is cancelled and that she declined his, which cancels it (she was the only
		// one invited). The past meeting goes quietly. The notifications still say it was Helen.
		mvc.perform(as("auth0|ivan", get("/api/notifications")))
			.andExpect(jsonPath("$[?(@.type == 'CANCELLED' && @.title == 'Coffee')]", hasSize(1)))
			.andExpect(jsonPath("$[?(@.type == 'DECLINED' && @.title == 'Lunch')]", hasSize(1)))
			.andExpect(jsonPath("$[?(@.type == 'CANCELLED' && @.title == 'Old chat')]", hasSize(0)))
			.andExpect(jsonPath("$[0].actor.name").value("Helen"))
			.andExpect(jsonPath("$[0].actor.id").value(nullValue()));
		mvc.perform(as("auth0|ivan", get("/api/meetings").param("from", "2020-01-01T00:00:00Z")
			.param("to", "2031-01-01T00:00:00Z"))).andExpect(jsonPath("$", hasSize(0)));

		// Her connection and the notes about her are gone too.
		mvc.perform(as("auth0|ivan", get("/api/connections")))
			.andExpect(jsonPath("$.connected[?(@.person.name == 'Helen')]", hasSize(0)));
		mvc.perform(as("auth0|ivan", get("/api/notes/recent"))).andExpect(jsonPath("$", hasSize(0)));

		// Signing in again starts over with an empty profile.
		mvc.perform(as("auth0|helen", post("/api/me/sync")).content("{\"name\":\"Helen\"}"))
			.andExpect(jsonPath("$.id").value(not(helen.toString())));
		mvc.perform(as("auth0|helen", get("/api/notes/recent"))).andExpect(jsonPath("$", hasSize(0)));
		mvc.perform(as("auth0|helen", get("/api/notifications"))).andExpect(jsonPath("$", hasSize(0)));
	}

	private UUID signUp(String sub, String name) throws Exception {
		mvc.perform(as(sub, post("/api/me/sync")).content("{\"name\":\"" + name + "\"}"));
		return profiles.findByAuth0Id(sub).orElseThrow().getId();
	}

	/** Has {@code from} send a request that the other person accepts. */
	private void connect(String from, String toName, UUID to) throws Exception {
		String fromName = profiles.findByAuth0Id(from).orElseThrow().getName();
		String toSub = profiles.findById(to).orElseThrow().getAuth0Id();
		mvc.perform(as(from, post("/api/connections")).content("{\"profileId\":\"" + to + "\"}"))
			.andExpect(status().isOk());
		String body = mvc.perform(as(toSub, get("/api/connections"))).andReturn().getResponse().getContentAsString();
		mvc.perform(as(toSub, post("/api/connections/" + incomingFrom(body, fromName) + "/accept")))
			.andExpect(jsonPath("$.connected[?(@.person.name == '" + fromName + "')]", hasSize(1)));
	}

	private static Object incomingFrom(String connectionsJson, String name) {
		List<Object> ids = JsonPath.read(connectionsJson, "$.incoming[?(@.person.name == '" + name + "')].id");
		return ids.getFirst();
	}

}
