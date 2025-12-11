package entidades;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;

/**
 * Encargado de leer el input y mover al jugador (su Body de Box2D).
 * El Jugador guarda los stats (velocidad, vida, etc.), y acá solo
 * usamos la velocidad y el Body físico.
 */
public class ControlJugador {

    private final Jugador jugador;
    private Body cuerpo;
    private final Vector2 direccion = new Vector2();

    public ControlJugador(Jugador jugador, Body cuerpo) {
        this.jugador = jugador;
        this.cuerpo = cuerpo;
    }

    /**
     * Llamar cada frame desde GestorDeEntidades.actualizar().
     */
    public void actualizar(float delta) {
        if (cuerpo == null) return;

        leerInput();

        if (direccion.isZero()) {
            cuerpo.setLinearVelocity(0, 0);
            return;
        }

        float vel = jugador.getVelocidad();
        //System.out.println("dir=" + direccion + " vel=" + vel);
        direccion.nor().scl(vel);
        cuerpo.setLinearVelocity(direccion);
    }


    private void leerInput() {
        direccion.set(0, 0);

        // WASD o flechas
        if (Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP)) {
            direccion.y += 1;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
            direccion.y -= 1;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
            direccion.x -= 1;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
            direccion.x += 1;
        }
    }

    public void setCuerpo(Body cuerpo) {
        this.cuerpo = cuerpo;
    }

    public Body getCuerpo() {
        return cuerpo;
    }
}
