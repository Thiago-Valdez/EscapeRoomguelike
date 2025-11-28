package entidades;

public interface Item {

    /**
     * Nombre legible del item, se usa para mostrarlo en el HUD,
     * logs, depuración, etc.
     */
    String getNombre();

    /**
     * Descripción corta del efecto del item.
     * Ideal para pantallas de detalle o tooltips en el futuro.
     */
    String getDescripcion();

    /**
     * Aplica el efecto del item sobre el jugador.
     * Puede modificar vida, velocidad, vida máxima, etc.
     */
    void aplicar(Jugador jugador);
}
