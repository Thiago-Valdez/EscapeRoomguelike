package entidades;

import interfaces.Equipable;
import interfaces.Modificable;

/**
 * Representa una instancia de ítem en el juego.
 *
 * - Tiene un tipo (ItemTipo) que define su rareza.
 * - Guarda un nombre visible.
 * - Tiene un efecto que modifica al jugador cuando se aplica.
 *
 * Implementa:
 *  - ModificableStats (aplicarModificacion / revertirModificacion)
 *  - Equipable (equipar / desequipar / getSlot)
 *
 * NOTA: por ahora todos los ítems son pasivos:
 *  - se aplican inmediatamente al recogerlos
 *  - el "slot" es genérico ("pasivo")
 */
public class Item implements Equipable, Modificable {

    @FunctionalInterface
    public interface EfectoItem {
        void aplicar(Jugador jugador);
    }

    private final String nombre;
    private final ItemTipo tipo;
    private final EfectoItem efecto;

    public Item(String nombre, ItemTipo tipo, EfectoItem efecto) {
        this.nombre = nombre;
        this.tipo = tipo;
        this.efecto = efecto;
    }

    // -------------------- Getters básicos --------------------

    public String getNombre() {
        return nombre;
    }

    /** Por si en algún lado usás nombre "visible" distinto. */
    public String getNombreVisible() {
        return nombre;
    }

    /** Descripción simple: por ahora usamos el nombre tal cual. */
    public String getDescripcion() {
        return nombre;
    }

    public ItemTipo getTipo() {
        return tipo;
    }

    public RarezaItem getRareza() {
        return tipo.rareza;
    }

    /**
     * Método que venías usando al recoger el ítem.
     * Por ahora simplemente aplica la modificación al jugador.
     */
    public void aplicar(Jugador jugador) {
        aplicarModificacion(jugador);
    }

    // -------------------- ModificableStats --------------------

    @Override
    public void aplicarModificacion(Jugador jugador) {
        if (efecto != null) {
            efecto.aplicar(jugador);
        }
    }

    @Override
    public void revertirModificacion(Jugador jugador) {
        // Por ahora los ítems son pasivos permanentes:
        // no revertimos efectos al "desequipar".
        // Si más adelante querés revertir, se puede agregar
        // otro EfectoItem inverso o más estado aquí.
    }

    // -------------------- Equipable --------------------

    @Override
    public void equipar(Jugador jugador) {
        // Lógica simple: al equipar, aplicamos modificación.
        aplicarModificacion(jugador);
    }

    @Override
    public void desequipar(Jugador jugador) {
        // Lógica simple: al desequipar, revertimos modificación.
        // (Hoy revertirModificacion() no hace nada, pero queda preparado).
        revertirModificacion(jugador);
    }

    @Override
    public String getSlot() {
        // Por ahora un slot único genérico.
        return "pasivo";
    }

    // -------------------- Para debug / HUD --------------------

    @Override
    public String toString() {
        return nombre + " (" + tipo.name() + ", " + tipo.rareza + ")";
    }
}
