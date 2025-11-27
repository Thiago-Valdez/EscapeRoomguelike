package entidades;

import java.util.ArrayList;
import java.util.List;

/**
 * Representa al jugador en la partida:
 * - atributos visibles (nombre, genero, estilo)
 * - atributos de gameplay (vida, velocidad)
 * - inventario (lista de Items)
 */
public class Jugador {

    private String nombre;
    private int vida;
    private int vidaMaxima;   // 👈 NUEVO
    private float velocidad;
    private Genero genero;
    private Estilo estilo;
    private List<Item> objetos;

    /**
     * Crea un jugador con parámetros iniciales básicos.
     */
    public Jugador(String nombre, Genero genero, Estilo estilo) {
        this.nombre = nombre;
        this.genero = genero;
        this.estilo = estilo;
        this.vidaMaxima = 3;  // base: 3 corazones
        this.vida = vidaMaxima;       // Vida inicial por defecto
        this.velocidad = 120f;  // Velocidad base (ajustable)
        this.objetos = new ArrayList<>();
    }

    // ====================
    // GETTERS / SETTERS
    // ====================
    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public int getVida() {
        return vida;
    }

    public void setVida(int vida) {
        this.vida = vida;
    }

    public void sumarVida(int cantidad) {
        this.vida += cantidad;
        if (this.vida < 0) this.vida = 0;
    }

    public int getVidaMaxima() {    // 👈 NUEVO
        return vidaMaxima;
    }

    public void setVidaMaxima(int vidaMaxima) {   // 👈 NUEVO
        this.vidaMaxima = Math.max(1, vidaMaxima);
        if (vida > this.vidaMaxima) {
            vida = this.vidaMaxima;
        }
    }

    public float getVelocidad() {
        return velocidad;
    }

    public void setVelocidad(float velocidad) {
        this.velocidad = velocidad;
    }

    public Genero getGenero() {
        return genero;
    }

    public void setGenero(Genero genero) {
        this.genero = genero;
    }

    public Estilo getEstilo() {
        return estilo;
    }

    public void setEstilo(Estilo estilo) {
        this.estilo = estilo;
    }

    // ====================
    // INVENTARIO
    // ====================
    public List<Item> getObjetos() {
        return objetos;
    }

    public void agregarObjeto(Item item) {
        if (item != null) {
            objetos.add(item);
        }
    }

    public void quitarObjeto(Item item) {
        objetos.remove(item);
    }
}
