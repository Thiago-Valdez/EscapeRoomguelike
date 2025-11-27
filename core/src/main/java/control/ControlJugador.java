package control;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import fisica.FisicaMundo;

public class ControlJugador {

    private final Body cuerpo;
    private float velocidadPx = 140f; // px/s
    private float velocidadMaxPx = 160f; // clamp opcional

    public ControlJugador(Body cuerpo) {
        this.cuerpo = cuerpo;
    }

    public void update() {
        Vector2 vel = new Vector2(0, 0);
        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) vel.x += 1f;
        if (Gdx.input.isKeyPressed(Input.Keys.LEFT))  vel.x -= 1f;
        if (Gdx.input.isKeyPressed(Input.Keys.UP))    vel.y += 1f;
        if (Gdx.input.isKeyPressed(Input.Keys.DOWN))  vel.y -= 1f;

        if (vel.len2() > 0) vel.nor().scl(velocidadPx / FisicaMundo.PPM);
        cuerpo.setLinearVelocity(vel);

        // clamp de seguridad
        Vector2 v = cuerpo.getLinearVelocity();
        float max = velocidadMaxPx / FisicaMundo.PPM;
        if (v.len() > max) {
            v.nor().scl(max);
            cuerpo.setLinearVelocity(v);
        }
    }

    public Body cuerpo() { return cuerpo; }
    public void setVelocidadPx(float v) { this.velocidadPx = v; }
    public void setVelocidadMaxPx(float v) { this.velocidadMaxPx = v; }
}
