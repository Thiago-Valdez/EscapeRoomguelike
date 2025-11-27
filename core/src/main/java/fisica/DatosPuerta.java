package fisica;

import mapa.Direccion;

public class DatosPuerta {
    public final String nombreHabitacion; // solo para logs
    public final Direccion direccion;     // hacia dónde sale la puerta (desde la sala actual)
    public final int gx;                  // celda X de la sala actual
    public final int gy;                  // celda Y de la sala actual

    public DatosPuerta(String nombreHabitacion, Direccion direccion, int gx, int gy) {
        this.nombreHabitacion = nombreHabitacion;
        this.direccion = direccion;
        this.gx = gx;
        this.gy = gy;
    }

    @Override public String toString() {
        return "Puerta[" + nombreHabitacion + " -> " + direccion + " @(" + gx + "," + gy + ")]";
    }
}
