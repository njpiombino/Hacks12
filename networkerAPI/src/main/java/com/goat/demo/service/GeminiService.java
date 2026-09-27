package com.goat.demo.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.goat.demo.domain.Profile;
import com.goat.demo.repository.ProfileRepository;
import com.goat.demo.web.Dto;

/**
 * Talks to Gemini and lets it call a couple of tools — searching this user's network and scheduling a
 * meeting with existing connections — to actually get things done instead of just talking about them.
 */
@Service
public class GeminiService {

	/** Guards against a runaway tool-calling loop; a real conversation never needs this many round trips. */
	private static final int MAX_TOOL_ROUNDS = 5;

	private static final String TOOLS_JSON = """
			[
			  {
			    "functionDeclarations": [
			      {
			        "name": "search_people",
			        "description": "Searches this user's network for people matching a name, headline keyword, or location. Use this to help the user find someone, or to look up who's already a connection before scheduling with them.",
			        "parameters": {
			          "type": "OBJECT",
			          "properties": {
			            "query": { "type": "STRING", "description": "A name, role, or location to search for. An empty string lists everyone." }
			          },
			          "required": ["query"]
			        }
			      },
			      {
			        "name": "schedule_meeting",
			        "description": "Schedules a meeting with one or more of the user's existing connections. Only works for people the user is already connected with — if they aren't connected yet, tell the user to connect first instead of calling this.",
			        "parameters": {
			          "type": "OBJECT",
			          "properties": {
			            "attendeeNames": {
			              "type": "ARRAY",
			              "items": { "type": "STRING" },
			              "description": "First names (or full names) of the connections to invite."
			            },
			            "title": { "type": "STRING", "description": "A short title for the meeting." },
			            "startsAt": { "type": "STRING", "description": "Start time as a UTC ISO-8601 instant, e.g. 2026-10-01T15:00:00Z." },
			            "endsAt": { "type": "STRING", "description": "End time as a UTC ISO-8601 instant." },
			            "location": { "type": "STRING", "description": "Optional location or video call link." },
			            "description": { "type": "STRING", "description": "Optional note about the meeting, e.g. what to say or discuss." }
			          },
			          "required": ["attendeeNames", "title", "startsAt", "endsAt"]
			        }
			      }
			    ]
			  }
			]
			""";

	private final RestClient restClient;

	private final ObjectMapper mapper;

	private final ProfileRepository profileRepository;

	private final ConnectionService connections;

	private final MeetingService meetings;

	private final String apiKey;

	private final String model;

	public GeminiService(ProfileRepository profileRepository, ConnectionService connections,
			MeetingService meetings, @Value("${gemini.api-key:}") String apiKey,
			@Value("${gemini.model:gemini-3.8-flash}") String model) {
		this.restClient = RestClient.builder().baseUrl("https://generativelanguage.googleapis.com").build();
		this.mapper = new ObjectMapper();
		this.profileRepository = profileRepository;
		this.connections = connections;
		this.meetings = meetings;
		this.apiKey = apiKey;
		this.model = model;
	}

	public String chat(Profile me, String message, List<Dto.ChatMessage> history, String timezone) {
		if (apiKey == null || apiKey.isBlank()) {
			throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "The chatbot isn't configured yet.");
		}
		String tz = (timezone == null || timezone.isBlank()) ? "UTC" : timezone;

		ArrayNode contents = mapper.createArrayNode();
		if (history != null) {
			for (Dto.ChatMessage turn : history) {
				contents.add(textTurn(turn.role(), turn.text()));
			}
		}
		contents.add(textTurn("user", message));

		for (int round = 0; round < MAX_TOOL_ROUNDS; round++) {
			JsonNode content = call(contents, tz).path("candidates").path(0).path("content");
			contents.add(content);

			List<JsonNode> functionCalls = new ArrayList<>();
			StringBuilder text = new StringBuilder();
			for (JsonNode part : content.path("parts")) {
				if (part.has("functionCall")) {
					functionCalls.add(part.get("functionCall"));
				}
				else if (part.has("text")) {
					text.append(part.get("text").asText());
				}
			}

			if (functionCalls.isEmpty()) {
				return text.isEmpty() ? "Sorry, I didn't catch that — could you try again?" : text.toString();
			}

			ObjectNode functionResponseTurn = mapper.createObjectNode();
			functionResponseTurn.put("role", "user");
			ArrayNode parts = functionResponseTurn.putArray("parts");
			for (JsonNode functionCall : functionCalls) {
				String name = functionCall.path("name").asText();
				Object result = execute(me, name, functionCall.path("args"));
				ObjectNode responsePart = parts.addObject().putObject("functionResponse");
				responsePart.put("name", name);
				responsePart.set("response", mapper.valueToTree(Map.of("result", result)));
			}
			contents.add(functionResponseTurn);
		}
		return "That request needed too many steps — try asking for something simpler.";
	}

	private Object execute(Profile me, String name, JsonNode args) {
		try {
			return switch (name) {
				case "search_people" -> searchPeople(me, args.path("query").asText(""));
				case "schedule_meeting" -> scheduleMeeting(me, args);
				default -> Map.of("error", "Unknown tool: " + name);
			};
		}
		catch (ResponseStatusException e) {
			return Map.of("error", String.valueOf(e.getReason()));
		}
		catch (Exception e) {
			return Map.of("error", "Something went wrong: " + e.getMessage());
		}
	}

	private List<Map<String, Object>> searchPeople(Profile me, String query) {
		var byPerson = connections.byOtherPerson(me);
		return profileRepository.search(query.strip(), me.getId())
			.stream()
			.filter(p -> p.getName() != null)
			.map(p -> {
				var relation = ConnectionService.relation(me, byPerson.get(p.getId()));
				Map<String, Object> row = new LinkedHashMap<>();
				row.put("name", p.getName());
				row.put("headline", p.getHeadline());
				row.put("location", p.getLocation());
				row.put("relation", relation.name());
				return row;
			})
			.limit(10)
			.toList();
	}

	private Map<String, Object> scheduleMeeting(Profile me, JsonNode args) {
		List<String> names = new ArrayList<>();
		for (JsonNode n : args.path("attendeeNames")) {
			names.add(n.asText());
		}
		if (names.isEmpty()) {
			return Map.of("error", "Tell me who to invite — I can only schedule with existing connections.");
		}

		Instant startsAt;
		Instant endsAt;
		try {
			startsAt = Instant.parse(args.path("startsAt").asText());
			endsAt = Instant.parse(args.path("endsAt").asText());
		}
		catch (Exception e) {
			return Map.of("error", "Times must be UTC ISO-8601 instants, like 2026-10-01T15:00:00Z.");
		}

		var connected = connections.overview(me).connected();
		List<UUID> attendeeIds = new ArrayList<>();
		for (String name : names) {
			var matches = connected.stream()
				.filter(item -> item.person().name() != null
						&& item.person().name().toLowerCase().contains(name.toLowerCase()))
				.toList();
			if (matches.isEmpty()) {
				return Map.of("error", "You're not connected with anyone named \"" + name + "\".");
			}
			if (matches.size() > 1) {
				return Map.of("error", "More than one connection matches \"" + name + "\" — ask which one.");
			}
			attendeeIds.add(matches.get(0).person().id());
		}

		String location = args.has("location") ? args.path("location").asText(null) : null;
		String description = args.has("description") ? args.path("description").asText(null) : null;
		String title = args.path("title").asText("Meeting");
		Dto.MeetingRequest request = new Dto.MeetingRequest(title, startsAt, endsAt, location, description, attendeeIds);
		Dto.MeetingView view = meetings.create(me, request);

		Map<String, Object> result = new LinkedHashMap<>();
		result.put("status", "scheduled");
		result.put("meetingId", view.id());
		result.put("title", view.title());
		result.put("attendees", view.attendees().stream().map(a -> a.person().name()).toList());
		return result;
	}

	private JsonNode call(ArrayNode contents, String timezone) {
		ObjectNode body = mapper.createObjectNode();
		body.set("contents", contents);
		try {
			body.set("tools", mapper.readTree(TOOLS_JSON));
		}
		catch (Exception e) {
			throw new IllegalStateException("Malformed tool schema", e);
		}
		body.putObject("systemInstruction").putArray("parts").addObject().put("text", systemPrompt(timezone));

		String requestJson;
		try {
			requestJson = mapper.writeValueAsString(body);
		}
		catch (Exception e) {
			throw new IllegalStateException("Couldn't build the Gemini request", e);
		}

		// Gemini's free-tier models occasionally return 503/429 under load; these are worth a
		// couple of quick retries rather than failing the whole chat turn outright.
		int[] backoffMillis = { 400, 1200 };
		for (int attempt = 0; ; attempt++) {
			try {
				String responseJson = restClient.post()
					.uri("/v1beta/models/{model}:generateContent?key={key}", model, apiKey)
					.contentType(MediaType.APPLICATION_JSON)
					.body(requestJson)
					.retrieve()
					.body(String.class);
				return mapper.readTree(responseJson);
			}
			catch (RestClientResponseException e) {
				boolean retryable = e.getStatusCode().value() == 503 || e.getStatusCode().value() == 429;
				if (!retryable || attempt >= backoffMillis.length) {
					throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Couldn't reach Gemini: " + e.getMessage());
				}
				try {
					Thread.sleep(backoffMillis[attempt]);
				}
				catch (InterruptedException ie) {
					Thread.currentThread().interrupt();
					throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Couldn't reach Gemini: interrupted");
				}
			}
			catch (Exception e) {
				throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Couldn't reach Gemini: " + e.getMessage());
			}
		}
	}

	private ObjectNode textTurn(String role, String text) {
		ObjectNode turn = mapper.createObjectNode();
		turn.put("role", role);
		turn.putArray("parts").addObject().put("text", text);
		return turn;
	}

	private String systemPrompt(String timezone) {
		return """
				You are Networker's in-app assistant. You help the signed-in user find people in their network \
				and schedule meetings with their existing connections.

				The current UTC date and time is %s. The user's local timezone is %s — when they give a time \
				like "5pm" or "tomorrow at 3", that time is in THEIR timezone, not UTC. Work out the correct \
				moment in their timezone first, then convert it to a UTC ISO-8601 instant before passing \
				startsAt/endsAt to schedule_meeting. For example, 5pm in America/New_York is 21:00 or 22:00 \
				UTC depending on daylight saving time — never pass "5pm" straight through as if it were already UTC.

				Only schedule meetings with people the user is already connected with. If someone isn't a \
				connection yet, tell the user to send a connection request first instead of trying to schedule \
				with them. Keep replies short and conversational.
				""".formatted(Instant.now(), timezone);
	}

}
