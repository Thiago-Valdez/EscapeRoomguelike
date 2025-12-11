package mapa;

import java.util.*;

/**
 * Grafo de “compatibilidad de puertas” entre habitaciones.
 *
 * Dos habitaciones A y B están conectadas si:
 *  - A tiene una puerta en una Direccion d
 *  - B tiene una puerta en la direccion opuesta d.opuesta()
 *
 * En esta versión NO obligamos a que sean vecinas en la grilla 5x5.
 * Eso permite, por ejemplo, conectar el NORTE de Inicio con
 * cualquier sala que tenga puerta SUR.
 */
public class GrafoPuertas {

    private final List<Habitacion> habitaciones;
    private final Random rng;

    /**
     * Para cada habitación origen, guarda a qué habitación lleva cada
     * dirección concreta (NORTE, SUR, ESTE, OESTE).
     *
     * conexiones.get(origen).get(dir)  -> destino (o null si no hay)
     */
    private final Map<Habitacion, EnumMap<Direccion, Habitacion>> conexiones =
        new EnumMap<>(Habitacion.class);

    public GrafoPuertas(List<Habitacion> habitaciones, Random rng) {
        this.habitaciones = new ArrayList<>(habitaciones);
        this.rng = rng;
        construirConexiones();
    }

    /**
     * Construye las conexiones lógicas:
     * para cada puerta (origen, dir), elige una sala destino
     * que tenga la puerta opuesta.
     */
    private void construirConexiones() {
        for (Habitacion origen : habitaciones) {

            EnumMap<Direccion, Habitacion> mapaDirs =
                new EnumMap<>(Direccion.class);

            for (Direccion dir : origen.puertas.keySet()) {
                Direccion opuesta = dir.opuesta();

                // Candidatos: TODAS las salas (distintas de origen)
                // que tengan una puerta en la dirección opuesta.
                List<Habitacion> candidatos = new ArrayList<>();
                for (Habitacion candidata : habitaciones) {
                    if (candidata == origen) continue;
                    if (candidata.tienePuerta(opuesta)) {
                        candidatos.add(candidata);
                    }
                }

                if (candidatos.isEmpty()) {
                    // No hay nadie compatible con esta puerta
                    continue;
                }

                Habitacion destino = candidatos.get(
                    rng.nextInt(candidatos.size())
                );

                mapaDirs.put(dir, destino);
            }

            conexiones.put(origen, mapaDirs);
        }

        // Debug opcional del grafo
        System.out.println("== GRAFO DE PUERTAS ==");
        for (var e : conexiones.entrySet()) {
            Habitacion h = e.getKey();
            EnumMap<Direccion, Habitacion> dirs = e.getValue();

            System.out.print(" " + h.nombreVisible + " ->");
            for (var de : dirs.entrySet()) {
                Direccion d = de.getKey();
                Habitacion dst = de.getValue();
                System.out.print(" [" + d + "→" + dst.nombreVisible + "]");
            }
            System.out.println();
        }
    }

    /**
     * Devuelve la habitación destino a la que lleva la puerta `dir`
     * desde la habitación `origen`, o null si no hay conexión.
     */
    public Habitacion destinoDe(Habitacion origen, Direccion dir) {
        EnumMap<Direccion, Habitacion> mapaDirs = conexiones.get(origen);
        if (mapaDirs == null) return null;
        return mapaDirs.get(dir);
    }

    /**
     * Vecinas lógicas de una habitación (unión de todos los destinos
     * de sus direcciones). Útil si necesitás “salas adyacentes” en
     * el sentido lógico del grafo.
     */
    public List<Habitacion> vecinas(Habitacion h) {
        EnumMap<Direccion, Habitacion> mapaDirs = conexiones.get(h);
        if (mapaDirs == null || mapaDirs.isEmpty()) {
            return Collections.emptyList();
        }

        // Evitar duplicados
        LinkedHashSet<Habitacion> set = new LinkedHashSet<>(mapaDirs.values());
        return new ArrayList<>(set);
    }

    /**
     * Devuelve una vecina aleatoria que cumpla un filtro, o null si no hay.
     */
    public Habitacion vecinaAleatoria(Habitacion h,
                                      java.util.function.Predicate<Habitacion> filtro) {
        List<Habitacion> candidatas = new ArrayList<>();
        for (Habitacion x : vecinas(h)) {
            if (filtro == null || filtro.test(x)) {
                candidatas.add(x);
            }
        }
        if (candidatas.isEmpty()) return null;
        return candidatas.get(rng.nextInt(candidatas.size()));
    }
}
