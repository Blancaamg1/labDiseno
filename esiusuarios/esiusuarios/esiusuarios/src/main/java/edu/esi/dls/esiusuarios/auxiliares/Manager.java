package edu.esi.dls.esiusuarios.auxiliares;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import edu.esi.dls.esiusuarios.services.EmailService;
import edu.esi.dls.esiusuarios.services.EmailServiceBrevo;

public class Manager {
	private static Manager yo;
	private EmailService emailService;

	private Manager() {
		this.emailService = new EmailServiceBrevo();
		yo = this;
	}

	public synchronized static Manager getInstance() {
		if (yo == null) {
			new Manager();
		}
		return yo;
	}

	public EmailService getEmailService() {
		return this.emailService;
	}

	public String readFile(String resourceName) throws IOException {
		InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourceName);
		if (inputStream == null) {
			throw new IOException("No existe el recurso: " + resourceName);
		}
		try (inputStream) {
			return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
		}
	}
}
