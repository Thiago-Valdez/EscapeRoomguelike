package entidades;

import com.badlogic.gdx.physics.box2d.*;
import java.util.*;

public class Jugador {

    private final String nombre;
    private final int id;

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

    public Jugador(int id, String nombre,
                   Genero generoInicial,
                   Estilo estiloInicial) {
        this.id = id;
        this.nombre = nombre;
        this.genero = (generoInicial != null) ? generoInicial : Genero.MASCULINO;
        this.estilo = (estiloInicial != null) ? estiloInicial : Estilo.CLASICO;

        this.vidaMaxima = 3;
        this.vida = 3;
        this.velocidad = 200f;

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
        this.cuerpoFisico.setUserData(this); // ✅ fuente de verdad
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
        this.velocidad = 200f;

        if (vida > vidaMaxima) vida = vidaMaxima;

        // aplicar todos los ítems pasivos
        for (Item item : objetos) {
            item.aplicarModificacion(this);
        }
    }

    public int getId() {
        return id;
    }

}
