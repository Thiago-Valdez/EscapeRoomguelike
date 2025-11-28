package entidades;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.physics.box2d.Body;

/**
 * Se encarga de leer el input y mover al jugador usando su Body de Box2D.
 */
public class ControlJugador {

    private Jugador jugador;
    private Body cuerpo;

    public ControlJugador(Jugador jugador, Body cuerpo) {
        this.jugador = jugador;
        this.cuerpo = cuerpo;
    }

    public void setJugador(Jugador jugador) {
        this.jugador = jugador;
    }

    public void setCuerpo(Body cuerpo) {
        this.cuerpo = cuerpo;
    }

    /**
     * Llamar una vez por frame desde JuegoPrincipal.render().
     */
    public void actualizarMovimiento() {
        if (jugador == null || cuerpo == null) return;

        float vx = 0f;
        float vy = 0f;

        if (Gdx.input.isKeyPressed(Input.Keys.W)) vy += 1f;
        if (Gdx.input.isKeyPressed(Input.Keys.S)) vy -= 1f;
        if (Gdx.input.isKeyPressed(Input.Keys.A)) vx -= 1f;
        if (Gdx.input.isKeyPressed(Input.Keys.D)) vx += 1f;

        if (vx == 0 && vy == 0) {
            cuerpo.setLinearVelocity(0, 0);
            return;
        }

        // Velocidad tomada del jugador (afectada por ítems)
        float speed = jugador.getVelocidad();

        float len = (float) Math.sqrt(vx * vx + vy * vy);
        vx /= len;
        vy /= len;

        cuerpo.setLinearVelocity(vx * speed, vy * speed);
    }
}
