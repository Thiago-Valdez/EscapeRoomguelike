package mapa;

import java.util.*;

public class DisposicionMapa {

    public static class Colocacion {
        public final Habitacion habitacion;
        public final int gx, gy;

        public Colocacion(Habitacion h, int gx, int gy) {
            this.habitacion = h;
            this.gx = gx;
            this.gy = gy;
        }
    }

    private final Map<String, Colocacion> mapa = new HashMap<>();

    private String key(int gx, int gy) {
        return gx + "," + gy;
    }

    public void agregar(Colocacion c) {
        mapa.put(key(c.gx, c.gy), c);
    }

    public boolean ocupada(int gx, int gy) {
        return mapa.containsKey(key(gx, gy));
    }

    public Collection<Colocacion> todas() {
        return mapa.values();
    }

    // ---------- 👇 MÉTODO QUE NECESITÁS PARA JUEGOPRINCIPAL ----------
    public Colocacion buscar(int gx, int gy) {
        return mapa.get(key(gx, gy));
    }
}
