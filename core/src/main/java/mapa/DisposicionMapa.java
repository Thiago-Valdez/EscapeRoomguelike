package mapa;

import java.util.*;

/** Resultado de la generación: lista de colocaciones en una grilla lógica. */
public final class DisposicionMapa {

    /** Una habitación colocada en la grilla (gx, gy). */
    public static final class Colocacion {
        public final Habitacion habitacion;
        public final int gx;
        public final int gy;

        public Colocacion(Habitacion habitacion, int gx, int gy) {
            this.habitacion = habitacion;
            this.gx = gx;
            this.gy = gy;
        }

        @Override public String toString() {
            return habitacion.nombreVisible + " @(" + gx + "," + gy + ")";
        }
    }

    private final List<Colocacion> colocaciones = new ArrayList<>();
    private final Map<String, Colocacion> porCelda = new HashMap<>();

    public void agregar(Colocacion c) {
        colocaciones.add(c);
        porCelda.put(clave(c.gx, c.gy), c);
    }

    public boolean ocupada(int gx, int gy) {
        return porCelda.containsKey(clave(gx, gy));
    }

    public Colocacion en(int gx, int gy) {
        return porCelda.get(clave(gx, gy));
    }

    /** Lista inmutable de colocaciones en orden de generación. */
    public List<Colocacion> todas() {
        return Collections.unmodifiableList(colocaciones);
    }

    /** Devuelve un **array** de Habitacion en orden de generación (para debug). */
    public Habitacion[] habitacionesComoArray() {
        Habitacion[] arr = new Habitacion[colocaciones.size()];
        for (int i = 0; i < colocaciones.size(); i++) {
            arr[i] = colocaciones.get(i).habitacion;
        }
        return arr;
    }

    private static String clave(int gx, int gy) { return gx + ":" + gy; }
}
