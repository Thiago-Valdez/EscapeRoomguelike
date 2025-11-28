package entidades;

import java.util.ArrayList;
import java.util.List;

public class Jugador extends Entidad {

    private int vida;
    private int vidaMaxima;
    private float velocidad;
    private Genero genero;
    private Estilo estilo;
    private List<Item> objetos;

    public Jugador(String nombre, Genero genero, Estilo estilo) {
        super(nombre);          // 👈 ahora el nombre lo maneja Entidad
        this.genero = genero;
        this.estilo = estilo;

        this.vidaMaxima = 3;
        this.vida = vidaMaxima;
        this.velocidad = 120f;
        this.objetos = new ArrayList<>();
    }

    // --- Vida ---
    public int getVida() {
        return vida;
    }

    public int getVidaMaxima() {
        return vidaMaxima;
    }

    public void setVidaMaxima(int vidaMaxima) {
        this.vidaMaxima = Math.max(1, vidaMaxima);
        if (vida > this.vidaMaxima) {
            vida = this.vidaMaxima;
        }
    }

    public void setVida(int vida) {
        this.vida = Math.max(0, Math.min(vida, vidaMaxima));
    }

    public void sumarVida(int cantidad) {
        setVida(this.vida + cantidad);
    }

    // --- Velocidad ---
    public float getVelocidad() {
        return velocidad;
    }

    public void setVelocidad(float velocidad) {
        this.velocidad = velocidad;
    }

    // --- Genero / Estilo ---
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

    // --- Objetos / Inventario ---
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

    // --- Lógica por frame ---
    @Override
    public void actualizar(float delta) {
        // Por ahora vacío.
        // Más adelante podés:
        //  - Actualizar efectos de estado
        //  - Crunch de cooldowns
        //  - Animaciones propias del jugador
    }
}
