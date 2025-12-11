package interfaces;

import entidades.Jugador;

public interface Modificable {

    /**
     * Aplica la modificación de estadísticas sobre el jugador.
     */
    void aplicarModificacion(Jugador jugador);

    /**
     * Revierte la modificación (si corresponde).
     * Útil para ítems equipables que dejan de estar activos.
     */
    void revertirModificacion(Jugador jugador);
}
