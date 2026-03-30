package edu.esi.dls.esiusuarios.services;

public abstract class EmailService {
    public abstract void sendEmail(String destinatario, Object... params );

}