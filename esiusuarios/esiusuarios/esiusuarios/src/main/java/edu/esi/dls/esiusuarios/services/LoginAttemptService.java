package edu.esi.dls.esiusuarios.services;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

@Service
public class LoginAttemptService {

    private static final int MAX_INTENTOS = 5;
    private static final long BLOQUEO_MILLIS = 2 * 60 * 1000L;

    private final Map<String, long[]> intentosFallidos = new ConcurrentHashMap<>();

    public void registrarFallo(String ip) {
        long ahora = Instant.now().toEpochMilli();
        intentosFallidos.compute(ip, (key, valor) -> {
            if (valor == null) {
                return new long[]{1, ahora};
            }
            long timestampBloqueo = valor[1];
            if (ahora - timestampBloqueo > BLOQUEO_MILLIS) {
                return new long[]{1, ahora};
            }
            return new long[]{valor[0] + 1, timestampBloqueo};
        });
    }

    public void registrarExito(String ip) {
        intentosFallidos.remove(ip);
    }

    public boolean estaBloqueada(String ip) {
        long[] valor = intentosFallidos.get(ip);
        if (valor == null) {
            return false;
        }
        long ahora = Instant.now().toEpochMilli();
        if (ahora - valor[1] > BLOQUEO_MILLIS) {
            intentosFallidos.remove(ip);
            return false;
        }
        return valor[0] >= MAX_INTENTOS;
    }

    public long minutosRestantes(String ip) {
        long[] valor = intentosFallidos.get(ip);
        if (valor == null) {
            return 0;
        }
        long ahora = Instant.now().toEpochMilli();
        long restantes = BLOQUEO_MILLIS - (ahora - valor[1]);
        return Math.max(0, restantes / 60000);
    }
}
