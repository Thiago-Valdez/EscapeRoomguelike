package juego.contactos;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;

import entidades.Entidad;
import entidades.GestorDeEntidades;
import entidades.datos.*;
import entidades.enemigos.*;
import entidades.eventos.*;
import entidades.items.*;
import entidades.personajes.*;
import entidades.sprites.*;
import juego.Partida;
import mapa.botones.*;
import mapa.generacion.*;
import mapa.minimapa.*;
import mapa.model.*;
import mapa.puertas.*;

/**
 * ContactListener dedicado al gameplay.
 *
 * Importante: NO modificamos Box2D dentro del callback.
 * Solo encolamos eventos y el update de Partida los procesa.
 */
public final class EnrutadorContactosPartida implements ContactListener {

    private final Partida partida;

    public EnrutadorContactosPartida(Partida partida) {
        this.partida = partida;
    }

    @Override
    public void beginContact(Contact contact) {
        Fixture a = contact.getFixtureA();
        Fixture b = contact.getFixtureB();

        // Puertas
        encolarContactoPuerta(a, b);
        encolarContactoPuerta(b, a);

        // Pickups
        encolarPickup(a, b);
        encolarPickup(b, a);

        // Botones (DOWN)
        encolarBoton(a, b, true);
        encolarBoton(b, a, true);

        detectarDanioJugadorEnemigo(a, b);
    }

    @Override
    public void endContact(Contact contact) {
        Fixture a = contact.getFixtureA();
        Fixture b = contact.getFixtureB();

        // Botones (UP)
        encolarBoton(a, b, false);
        encolarBoton(b, a, false);
    }

    @Override public void preSolve(Contact contact, Manifold oldManifold) {}
    @Override public void postSolve(Contact contact, ContactImpulse impulse) {}

    private void encolarContactoPuerta(Fixture puertaFx, Fixture otroFx) {
        if (puertaFx == null || otroFx == null) return;
        if (partida.getSistemaTransicionSala().bloqueoActivo()) return;

        Object ud = puertaFx.getUserData();
        if (!(ud instanceof DatosPuerta puerta)) return;

        int jugadorId = getJugadorId(otroFx);
        if (jugadorId == -1) return;

        Habitacion salaActual = partida.getSalaActual();
        if (salaActual == puerta.origen() || salaActual == puerta.destino()) {
            partida.getPuertasPendientes().add(new EventoPuerta(puerta, jugadorId));
        }
    }

    private void encolarPickup(Fixture jugadorFx, Fixture otroFx) {
        if (jugadorFx == null || otroFx == null) return;
        int jugadorId = getJugadorId(jugadorFx);
        if (jugadorId == -1) return;

        Object ud = otroFx.getUserData();
        if (ud instanceof entidades.items.Item item) {
            partida.getItemsPendientes().add(new EventoPickup(item, jugadorId));
        }
    }

    private void encolarBoton(Fixture jugadorFx, Fixture otroFx, boolean down) {
        if (jugadorFx == null || otroFx == null) return;
        int jugadorId = getJugadorId(jugadorFx);
        if (jugadorId == -1) return;

        Object ud = otroFx.getUserData();
        if (ud instanceof DatosBoton db) {
            partida.getEventosBoton().add(new EventoBoton(db, jugadorId, down));
        }
    }

    private int getJugadorId(Fixture fx) {
        if (fx == null) return -1;
        Body b = fx.getBody();
        if (b == null) return -1;
        Object ud = b.getUserData();
        if (ud instanceof Jugador j) return j.getId();
        return -1;
    }

    private void detectarDanioJugadorEnemigo(Fixture a, Fixture b) {
        if (a == null || b == null) return;

        Object ua = a.getBody() != null ? a.getBody().getUserData() : null;
        Object ub = b.getBody() != null ? b.getBody().getUserData() : null;

        if (ua instanceof Jugador j && ub instanceof Enemigo e) {
            Vector2 pe = e.getCuerpoFisico().getPosition();
            partida.encolarDanioJugador(j.getId(), pe.x, pe.y);
            return;
        }
        if (ua instanceof Enemigo e && ub instanceof Jugador j) {
            Vector2 pe = e.getCuerpoFisico().getPosition();
            partida.encolarDanioJugador(j.getId(), pe.x, pe.y);
        }
    }
}
