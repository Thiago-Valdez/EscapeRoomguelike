package juego;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.ContactListener;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.Manifold;
import entidades.Enemigo;
import entidades.Jugador;

/**
 * ContactListener dedicado al gameplay.
 *
 * Importante: NO modificamos Box2D dentro del callback.
 * Solo encolamos eventos y Partida los procesa en el update.
 */
public final class ContactosPartida implements ContactListener {

    private final Partida partida;

    public ContactosPartida(Partida partida) {
        this.partida = partida;
    }

    @Override
    public void beginContact(Contact contact) {
        Fixture a = contact.getFixtureA();
        Fixture b = contact.getFixtureB();

        // Puertas
        partida.encolarContactoPuerta(a, b);
        partida.encolarContactoPuerta(b, a);

        // Pickups
        partida.encolarPickup(a, b);
        partida.encolarPickup(b, a);

        // Botones (DOWN)
        partida.encolarBoton(a, b, true);
        partida.encolarBoton(b, a, true);

        detectarDanioJugadorEnemigo(a, b);

    }

    @Override
    public void endContact(Contact contact) {
        Fixture a = contact.getFixtureA();
        Fixture b = contact.getFixtureB();

        // Botones (UP)
        partida.encolarBoton(a, b, false);
        partida.encolarBoton(b, a, false);
    }

    @Override public void preSolve(Contact contact, Manifold oldManifold) {}
    @Override public void postSolve(Contact contact, ContactImpulse impulse) {}

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
