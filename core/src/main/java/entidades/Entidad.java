package entidades;

import com.badlogic.gdx.physics.box2d.Body;
import interfaces.Actualizable;

/**
 * Entidad abstracta del juego.
 * Cualquier cosa "viva" del juego (Jugador, Enemigo, NPC, etc.)
 * debería extender de esta clase.
 */
public abstract class Entidad implements Actualizable {

    protected String nombre;
    protected Body cuerpoFisico; // puede ser null si todavía no tiene Body

    public Entidad(String nombre) {
        this.nombre = nombre;
    }

    // --- Nombre visible ---
    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    // --- Cuerpo físico (Box2D) ---
    public Body getCuerpoFisico() {
        return cuerpoFisico;
    }

    public void setCuerpoFisico(Body cuerpoFisico) {
        this.cuerpoFisico = cuerpoFisico;
    }

    // Posición basada en el Body
    public float getX() {
        return (cuerpoFisico != null) ? cuerpoFisico.getPosition().x : 0f;
    }

    public float getY() {
        return (cuerpoFisico != null) ? cuerpoFisico.getPosition().y : 0f;
    }

    public void setPosicion(float x, float y) {
        if (cuerpoFisico != null) {
            cuerpoFisico.setTransform(x, y, cuerpoFisico.getAngle());
        }
    }

    /**
     * Toda entidad tiene una lógica de actualización por frame.
     * Se concreta en las subclases (Jugador, Enemigo, etc.).
     */
    @Override
    public abstract void actualizar(float delta);
}
