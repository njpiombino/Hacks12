package com.goat.demo.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.stereotype.Component;

import com.goat.demo.domain.Connection;
import com.goat.demo.domain.Profile;
import com.goat.demo.repository.ConnectionRepository;
import com.goat.demo.repository.ProfileRepository;

/**
 * Seeds a few sample people so a fresh install isn't empty. A couple of them send each newcomer a
 * connection request, so there's someone to accept and meet with. Requests sent to them stay pending.
 */
@Component
@ConditionalOnBooleanProperty("app.demo-data")
public class DemoData implements ApplicationRunner {

	/** Sample people who reach out to every new user. */
	private static final String[] WELCOMERS = { "demo|4", "demo|5" };

	private record Sample(String name, String headline, String location, String bio) {
	}

	private static final Sample[] SAMPLES = {
			new Sample("Maya Okafor", "Product designer at a small climate startup", "Richmond, VA",
					"Sketches everything on paper first. Always up for talking about design systems and good coffee."),
			new Sample("Daniel Reyes", "Backend engineer, Java & Postgres", "Washington, DC",
					"Ten years building payment systems. Mentors bootcamp grads on weekends."),
			new Sample("Priya Shah", "Venture associate focused on edtech", "New York, NY",
					"Former teacher turned investor. Loves meeting founders who have taught before."),
			new Sample("Tom Whitaker", "CS professor & research lab lead", "Williamsburg, VA",
					"Distributed systems researcher. Runs the department's undergraduate research program."),
			new Sample("Hana Kim", "Recruiter for early-career engineering", "Remote",
					"Helps new grads find their first role. Happy to review résumés."), };

	private final ProfileRepository profiles;

	private final ConnectionRepository connections;

	public DemoData(ProfileRepository profiles, ConnectionRepository connections) {
		this.profiles = profiles;
		this.connections = connections;
	}

	/** Has the welcoming sample people send a connection request to someone who just signed up. */
	public void welcome(Profile newcomer) {
		for (String auth0Id : WELCOMERS) {
			profiles.findByAuth0Id(auth0Id).ifPresent(p -> connections.save(new Connection(p, newcomer)));
		}
	}

	@Override
	public void run(ApplicationArguments args) {
		for (int i = 0; i < SAMPLES.length; i++) {
			String auth0Id = "demo|" + (i + 1);
			if (profiles.findByAuth0Id(auth0Id).isPresent()) {
				continue;
			}
			Sample s = SAMPLES[i];
			Profile p = new Profile(auth0Id);
			p.setName(s.name());
			p.setHeadline(s.headline());
			p.setLocation(s.location());
			p.setBio(s.bio());
			p.setEmail(s.name().toLowerCase().replace(' ', '.') + "@example.com");
			profiles.save(p);
		}
	}

}
