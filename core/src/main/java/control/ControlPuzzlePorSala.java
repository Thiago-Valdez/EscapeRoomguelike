package control;

import java.util.HashMap;
import java.util.Map;

import mapa.Habitacion;
import mapa.TipoSala;

public class ControlPuzzlePorSala {

    private static class Estado {
        boolean resuelta = false; // ✅ persistente
        boolean locked = true;
        boolean p1 = false;
        boolean p2 = false;
    }

    private final Map<Habitacion, Estado> estados = new HashMap<>();

    private boolean aplica(Habitacion sala) {
        if (sala == null) return false;
        // ✅ ahora aplica también a COMBATE
        return sala.tipo == TipoSala.ACERTIJO || sala.tipo == TipoSala.COMBATE;
    }

    /** Llamar al entrar a una sala */
    public void alEntrarASala(Habitacion sala) {
        if (!aplica(sala)) return;

        Estado e = estados.computeIfAbsent(sala, k -> new Estado());

        // ✅ si ya está resuelta, NO se vuelve a bloquear
        if (e.resuelta) {
            e.locked = false;
            return;
        }

        // ✅ solo resetea si NO estaba resuelta
        e.locked = true;
        e.p1 = false;
        e.p2 = false;
    }

    public boolean estaBloqueada(Habitacion sala) {
        if (!aplica(sala)) return false;

        Estado e = estados.get(sala);
        // si nunca se inicializó, la tratamos como bloqueada (por seguridad)
        return e == null || e.locked;
    }

    public boolean botonDown(Habitacion sala, int botonDeJugador) {
        if (!aplica(sala)) return false;

        Estado e = estados.computeIfAbsent(sala, k -> new Estado());
        if (e.resuelta) return false;

        if (botonDeJugador == 1) e.p1 = true;
        if (botonDeJugador == 2) e.p2 = true;

        if (e.p1 && e.p2) {
            e.locked = false;
            e.resuelta = true; // ✅ queda resuelta
            return true;
        }
        return false;
    }

    public void botonUp(Habitacion sala, int botonDeJugador) {
        if (!aplica(sala)) return;

        Estado e = estados.computeIfAbsent(sala, k -> new Estado());
        if (e.resuelta) return; // si ya está resuelta, no importa

        if (botonDeJugador == 1) e.p1 = false;
        if (botonDeJugador == 2) e.p2 = false;
    }


    /** Por si después querés desbloquear COMBATE por enemigos muertos */
    public void marcarResuelta(Habitacion sala) {
        if (!aplica(sala)) return;
        Estado e = estados.computeIfAbsent(sala, k -> new Estado());
        e.resuelta = true;
        e.locked = false;
    }

    public boolean estaResuelta(Habitacion sala) {
        if (!aplica(sala)) return true;
        Estado e = estados.get(sala);
        return e != null && e.resuelta;
    }
}
