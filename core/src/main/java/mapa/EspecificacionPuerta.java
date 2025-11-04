package mapa;

/** Posición de una puerta en coordenadas locales de la habitación, y su dirección. */
public final class EspecificacionPuerta {
    public final Direccion direccion;
    public final int localX;
    public final int localY;

    public EspecificacionPuerta(Direccion direccion, int localX, int localY) {
        this.direccion = direccion;
        this.localX = localX;
        this.localY = localY;
    }
}
