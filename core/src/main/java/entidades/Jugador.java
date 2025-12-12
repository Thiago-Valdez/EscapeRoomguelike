package entidades;

import com.badlogic.gdx.physics.box2d.*;
import java.util.*;

public class Jugador {

    private final String nombre;

    // Estética / apariencia
    private Genero genero;
    private Estilo estilo;

    // Stats
    private int vida;
    private int vidaMaxima;
    private float velocidad;

    // Cuerpo físico en Box2D
    private Body cuerpoFisico;

    // Inventario simple (ítems pasivos)
    private final List<Item> objetos = new ArrayList<>();

    public Jugador(String nombre,
                   Genero generoInicial,
                   Estilo estiloInicial,
                   World world,
                   float x,
                   float y) {

        this.nombre = nombre;
        this.genero = (generoInicial != null) ? generoInicial : Genero.MASCULINO;
        this.estilo = (estiloInicial != null) ? estiloInicial : Estilo.CLASICO;

        this.vidaMaxima = 3;
        this.vida = 3;
        this.velocidad = 100f;

        // ---- Crear cuerpo físico ----
        BodyDef bd = new BodyDef();
        bd.type = BodyDef.BodyType.DynamicBody;
        bd.position.set(x, y);

        cuerpoFisico = world.createBody(bd);
        cuerpoFisico.setUserData(this); // útil para colisiones

        PolygonShape shape = new PolygonShape();
        // Ajustá el tamaño al sprite real
        shape.setAsBox(10f, 10f);

        FixtureDef fd = new FixtureDef();
        fd.shape = shape;
        fd.density = 1f;
        fd.friction = 0.3f;

        Fixture fx = cuerpoFisico.createFixture(fd);
        fx.setUserData("jugador");

        // 🔍 LOG DE CREACIÓN
        com.badlogic.gdx.Gdx.app.log(
            "Jugador",
            "Body creado en (" + x + ", " + y + ") " +
                "worldHash=" + System.identityHashCode(world) +
                " bodyHash=" + System.identityHashCode(cuerpoFisico)
        );

        shape.dispose();
    }

    // ------------------ Nombre ------------------

    public String getNombre() {
        return nombre;
    }

    // ------------------ Estética ------------------

    public Genero getGenero() {
        return genero;
    }

    public void setGenero(Genero genero) {
        if (genero != null) {
            this.genero = genero;
        }
    }

    public Estilo getEstilo() {
        return estilo;
    }

    public void setEstilo(Estilo estilo) {
        if (estilo != null) {
            this.estilo = estilo;
        }
    }

    public String getClaveSpriteBase() {
        return "player_" + genero.getSufijoSprite() + "_" + estilo.getSufijoSprite();
    }

    // ------------------ Stats ------------------

    public int getVida() {
        return vida;
    }

    public void setVida(int vida) {
        if (vida < 0) vida = 0;
        if (vida > vidaMaxima) vida = vidaMaxima;
        this.vida = vida;
    }

    public int getVidaMaxima() {
        return vidaMaxima;
    }

    public void setVidaMaxima(int vidaMaxima) {
        if (vidaMaxima < 1) vidaMaxima = 1;
        this.vidaMaxima = vidaMaxima;
        if (vida > vidaMaxima) {
            vida = vidaMaxima;
        }
    }

    public float getVelocidad() {
        return velocidad;
    }

    public void setVelocidad(float velocidad) {
        if (velocidad < 0f) velocidad = 0f;
        this.velocidad = velocidad;
    }

    // ------------------ Física ------------------

    public Body getCuerpoFisico() {
        return cuerpoFisico;
    }

    public void setCuerpoFisico(Body cuerpoFisico) {
        this.cuerpoFisico = cuerpoFisico;
    }

    // ------------------ Inventario ------------------

    public List<Item> getObjetos() {
        return Collections.unmodifiableList(objetos);
    }

    public void agregarObjeto(Item item) {
        if (item == null) return;
        objetos.add(item);
    }

    public void removerObjeto(Item item) {
        objetos.remove(item);
    }

    public void reaplicarEfectosDeItems() {
        // stats base del jugador
        this.vidaMaxima = 3;
        this.velocidad = 100f;

        if (vida > vidaMaxima) vida = vidaMaxima;

        // aplicar todos los ítems pasivos
        for (Item item : objetos) {
            item.aplicarModificacion(this);
        }
    }

}
