package edu.esi.dls.esiusuarios.auxiliares;

import edu.esi.dls.esiusuarios.services.EmailService;
import edu.esi.dls.esiusuarios.services.EmailServiceFalso;

public class Manager {
    private static Manager yo;
    private EmailService emailService;
    
    private Manager() {
        this.emailService = new EmailServiceFalso();
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
}
