package interfaces;

import entidades.Jugador;

public interface Equipable {

    /**
     * Se llama cuando el jugador equipa el ítem.
     */
    void equipar(Jugador jugador);

    /**
     * Se llama cuando el jugador se quita el ítem.
     */
    void desequipar(Jugador jugador);

    /**
     * Slot donde se equipa. Ejemplos:
     *  - "cabeza"
     *  - "cuerpo"
     *  - "pies"
     *  - "pasiva"
     */
    String getSlot();
}
