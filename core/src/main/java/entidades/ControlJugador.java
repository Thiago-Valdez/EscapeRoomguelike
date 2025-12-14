package entidades;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;

public class ControlJugador {

    private final Jugador jugador;

    public ControlJugador(Jugador jugador) {
        this.jugador = jugador;
    }

    public void actualizar(float delta) {
        Body cuerpo = jugador.getCuerpoFisico();
        if (cuerpo == null) return;

        float dx = 0;
        float dy = 0;

        if (Gdx.input.isKeyPressed(Input.Keys.W)) dy += 1;
        if (Gdx.input.isKeyPressed(Input.Keys.S)) dy -= 1;
        if (Gdx.input.isKeyPressed(Input.Keys.A)) dx -= 1;
        if (Gdx.input.isKeyPressed(Input.Keys.D)) dx += 1;

        Vector2 dir = new Vector2(dx, dy);

        if (dir.len2() > 0) {
            dir.nor();
            float velocidad = jugador.getVelocidad();

            // opcional: log de verificación
            // Gdx.app.log("VEL", "stat=" + velocidad + " bodyHash=" + System.identityHashCode(cuerpo));

            cuerpo.setLinearVelocity(dir.x * velocidad, dir.y * velocidad);
        } else {
            cuerpo.setLinearVelocity(0, 0);
        }

        // Nuevo: Chequear si se está manteniendo presionado el botón para activar el puzzle
        if (Gdx.input.isKeyPressed(Input.Keys.E)) {
            jugador.activarBoton(true); // Activar el botón si se mantiene presionado
        } else {
            jugador.activarBoton(false); // Desactivar si no se mantiene presionado
        }
    }
}
