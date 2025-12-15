package juego.sistemas;

import java.util.List;
import java.util.function.BiConsumer;

import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.physics.box2d.World;

import control.puzzle.ControlPuzzlePorSala;
import control.salas.GestorSalas;
import entidades.GestorDeEntidades;
import entidades.enemigos.EnemigosDesdeTiled;
import mapa.minimapa.DisposicionMapa;
import mapa.model.Habitacion;
import mapa.puertas.EventoPuerta;

/**
 * Maneja el cambio de sala por puertas y el cooldown anti "ping-pong".
 * Mantiene el comportamiento existente y deja a Partida como orquestador.
 */
public final class SistemaTransicionSala {

    private int framesBloqueoPuertas = 0;

    public boolean bloqueoActivo() {
        return framesBloqueoPuertas > 0;
    }

    public void tickCooldown() {
        if (framesBloqueoPuertas > 0) framesBloqueoPuertas--;
    }

    /**
     * Procesa el primer evento de puerta pendiente (si existe), aplicando cooldown.
     * @return salaActual actualizada (o la misma si no hubo transición)
     */
    public Habitacion procesarPuertasPendientes(
            Habitacion salaActual,
            List<EventoPuerta> puertasPendientes,
            ControlPuzzlePorSala controlPuzzle,
            GestorSalas gestorSalas,
            DisposicionMapa disposicion,
            BiConsumer<Habitacion, Habitacion> notificarCambioSala,
            TiledMap mapaTiled,
            World world,
            GestorDeEntidades gestorEntidades,
            SistemaSpritesEntidades sprites
    ) {

        // puertas cerradas por puzzle/combat/etc
        if (controlPuzzle != null && controlPuzzle.estaBloqueada(salaActual)) {
            puertasPendientes.clear();
            return salaActual;
        }

        if (puertasPendientes.isEmpty()) return salaActual;

        if (framesBloqueoPuertas > 0) {
            puertasPendientes.clear();
            return salaActual;
        }

        EventoPuerta ev = puertasPendientes.get(0);
        Habitacion nueva = gestorSalas.irASalaVecinaPorPuerta(salaActual, ev.puerta(), ev.jugadorId());

        if (nueva != null) {
            Habitacion anterior = salaActual;

            // cambio de sala: limpiamos instantáneo (no anim) para evitar cuerpos vivos fuera de la sala
            gestorEntidades.eliminarEnemigosDeSala(anterior);

            // limpiamos tracking de muerte de esa sala
            if (sprites != null) sprites.limpiarSpritesDeEntidadesMuertas();

            salaActual = nueva;
            disposicion.descubrir(salaActual);

            if (notificarCambioSala != null) {
                notificarCambioSala.accept(anterior, salaActual);
            }

            if (controlPuzzle != null) controlPuzzle.alEntrarASala(salaActual);

            EnemigosDesdeTiled.crearEnemigosDesdeMapa(mapaTiled, salaActual, world, gestorEntidades);
            if (sprites != null) {
                sprites.registrarSpritesDeEnemigosVivos();
                sprites.limpiarSpritesDeEntidadesMuertas();
            }
        }

        framesBloqueoPuertas = 15;
        puertasPendientes.clear();
        return salaActual;
    }
}
