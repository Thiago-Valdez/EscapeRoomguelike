package control;

import java.util.HashMap;
import java.util.Map;

import mapa.Habitacion;
import mapa.TipoSala;

/**
 * Controla el “puzzle de botones” por sala:
 * - Aplica a ACERTIJO y COMBATE (puertas cerradas hasta cumplir condición)
 * - Estado persistente: si la sala ya fue resuelta, queda desbloqueada para siempre
 * - “Mantenido”: mientras NO esté resuelta, si un jugador suelta su botón, vuelve a bloquearse
 */
public class ControlPuzzlePorSala {

    private static final int MAX_JUGADORES = 2;

    private static class Estado {
        boolean resuelta = false;   // ✅ persistente
        boolean locked = true;      // puertas cerradas
        boolean[] pressed = new boolean[MAX_JUGADORES + 1]; // indices 1..2
    }

    private final Map<Habitacion, Estado> estados = new HashMap<>();

    private boolean aplica(Habitacion sala) {
        if (sala == null) return false;
        return sala.tipo == TipoSala.ACERTIJO || sala.tipo == TipoSala.COMBATE;
    }

    private boolean jugadorValido(int jugadorId) {
        return jugadorId >= 1 && jugadorId <= MAX_JUGADORES;
    }

    /** Llamar al entrar a una sala (o al cambiar salaActual) */
    public void alEntrarASala(Habitacion sala) {
        if (!aplica(sala)) return;

        Estado e = estados.computeIfAbsent(sala, k -> new Estado());

        // ✅ Si ya fue resuelta, no se vuelve a bloquear nunca
        if (e.resuelta) {
            e.locked = false;
            return;
        }

        // ✅ Si NO fue resuelta, arrancamos bloqueada.
        // Nota: dejamos pressed[] como esté o lo reseteamos:
        // - Si tus spawns siempre ocurren lejos de botones, reseteo es más simple y seguro.
        e.locked = true;
        for (int i = 1; i <= MAX_JUGADORES; i++) {
            e.pressed[i] = false;
        }
    }

    public boolean estaBloqueada(Habitacion sala) {
        if (!aplica(sala)) return false;
        Estado e = estados.get(sala);
        // si nunca se inicializó, por seguridad: bloqueada
        return e == null || e.locked;
    }

    /** Se llama al detectar BEGIN contact del jugador con SU botón */
    public boolean botonDown(Habitacion sala, int botonDeJugador) {
        if (!aplica(sala)) return false;
        if (!jugadorValido(botonDeJugador)) return false;

        Estado e = estados.computeIfAbsent(sala, k -> new Estado());
        if (e.resuelta) return false;

        e.pressed[botonDeJugador] = true;

        if (e.pressed[1] && e.pressed[2]) {
            e.locked = false;
            e.resuelta = true; // ✅ queda resuelta permanentemente
            return true;
        }

        return false;
    }

    /** Se llama al detectar END contact del jugador con SU botón */
    public void botonUp(Habitacion sala, int botonDeJugador) {
        if (!aplica(sala)) return;
        if (!jugadorValido(botonDeJugador)) return;

        Estado e = estados.computeIfAbsent(sala, k -> new Estado());
        if (e.resuelta) return; // si ya fue resuelta, no importa

        e.pressed[botonDeJugador] = false;

        // ✅ “mantenido”: si suelta alguno antes de resolver, sigue bloqueado
        // (locked ya es true mientras no se resuelva, pero lo dejamos explícito)
        e.locked = true;
    }

    /** Para futuro: desbloquear COMBATE por enemigos muertos, etc. */
    public void marcarResuelta(Habitacion sala) {
        if (!aplica(sala)) return;
        Estado e = estados.computeIfAbsent(sala, k -> new Estado());
        e.resuelta = true;
        e.locked = false;
        for (int i = 1; i <= MAX_JUGADORES; i++) e.pressed[i] = false;
    }

    public boolean estaResuelta(Habitacion sala) {
        if (!aplica(sala)) return true;
        Estado e = estados.get(sala);
        return e != null && e.resuelta;
    }
}
