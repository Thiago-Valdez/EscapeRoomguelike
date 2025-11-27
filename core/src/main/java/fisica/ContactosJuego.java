package fisica;

import com.badlogic.gdx.physics.box2d.*;

public class ContactosJuego implements ContactListener {

    @Override
    public void beginContact(Contact contact) {
        Fixture a = contact.getFixtureA();
        Fixture b = contact.getFixtureB();

        // Si alguna es puerta (sensor), loggear y/o disparar evento de cambio de sala
        if (esPuerta(a) && esJugador(b)) {
            logPuerta(a, b);
        } else if (esPuerta(b) && esJugador(a)) {
            logPuerta(b, a);
        }
    }

    @Override
    public void endContact(Contact contact) {
        // Podés loggear salida de puerta si te sirve
    }

    @Override public void preSolve(Contact contact, Manifold oldManifold) { }
    @Override public void postSolve(Contact contact, ContactImpulse impulse) { }

    private boolean esJugador(Fixture f) {
        return (f.getFilterData().categoryBits & BitsColision.CATEGORIA_JUGADOR) != 0;
    }

    private boolean esPuerta(Fixture f) {
        return (f.getFilterData().categoryBits & BitsColision.CATEGORIA_PUERTA) != 0;
    }

    private void logPuerta(Fixture puerta, Fixture jugador) {
        Object user = puerta.getUserData();
        System.out.println("[COLISION] Jugador tocó PUERTA -> " + (user != null ? user : "sin userData"));
    }
}
