package mapa;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Genera un camino de N habitaciones.
 * Reglas:
 *  - Siempre comienza en INICIO_1.
 *  - No repite TipoSala.
 *  - La nueva habitación debe tener puerta compatible con la anterior
 *    y colocarse contiguamente en la grilla (N/S/E/O).
 *  - Determinístico por seed.
 */
public final class GeneradorMapa {

    public static class Configuracion {
        public int longitud = 5;                  // cantidad total de habitaciones
        public long semilla = System.currentTimeMillis();
        public boolean permitirRetrocesos = false; // para futuras expansiones
    }

    private final Configuracion cfg;
    private final Random rng;

    public GeneradorMapa(Configuracion cfg) {
        this.cfg = cfg;
        this.rng = new Random(cfg.semilla);
    }

    public DisposicionMapa generar() {
        if (cfg.longitud < 1) throw new IllegalArgumentException("longitud debe ser >= 1");

        DisposicionMapa disp = new DisposicionMapa();

        // 1) Colocar inicio en (0,0)
        Habitacion inicio = Habitacion.INICIO_1;
        DisposicionMapa.Colocacion actual = new DisposicionMapa.Colocacion(inicio, 0, 0);
        disp.agregar(actual);

        // 2) Llevar tipos usados
        EnumSet<TipoSala> tiposUsados = EnumSet.of(inicio.tipo);

        // 3) Pool de candidatas (todas menos INICIO)
        List<Habitacion> pool = Arrays.stream(Habitacion.values())
            .filter(h -> h != Habitacion.INICIO_1)
            .collect(Collectors.toCollection(ArrayList::new));

        int presupuestoIntentos = 500; // evita loops infinitos
        while (disp.todas().size() < cfg.longitud && presupuestoIntentos-- > 0) {

            Direccion salida = elegirPuertaAleatoria(actual.habitacion, rng).orElse(null);
            if (salida == null) {
                Optional<DisposicionMapa.Colocacion> alt = elegirColocacionConPuerta(disp, rng);
                if (alt.isPresent()) {
                    actual = alt.get();
                    continue;
                } else {
                    throw new IllegalStateException("No es posible expandir el mapa con las restricciones actuales.");
                }
            }

            int nx = actual.gx + dx(salida);
            int ny = actual.gy + dy(salida);
            if (disp.ocupada(nx, ny)) {
                continue; // probá otra puerta en el próximo ciclo
            }

            Direccion necesita = salida.opuesta();
            List<Habitacion> candidatas = pool.stream()
                .filter(h -> !tiposUsados.contains(h.tipo))
                .filter(h -> h.tienePuerta(necesita))
                .collect(Collectors.toList());

            if (candidatas.isEmpty()) {
                continue; // no había match para esa dirección, probamos otra luego
            }

            Habitacion elegida = candidatas.get(rng.nextInt(candidatas.size()));
            DisposicionMapa.Colocacion nueva = new DisposicionMapa.Colocacion(elegida, nx, ny);
            disp.agregar(nueva);
            tiposUsados.add(elegida.tipo);
            actual = nueva;
        }

        if (disp.todas().size() < cfg.longitud) {
            throw new IllegalStateException("No se pudo construir un camino de " + cfg.longitud + " habitaciones.");
        }

        return disp;
    }

    // ===== Helpers =====
    private static Optional<Direccion> elegirPuertaAleatoria(Habitacion h, Random rng) {
        List<Direccion> dirs = new ArrayList<>(h.puertas.keySet());
        if (dirs.isEmpty()) return Optional.empty();
        return Optional.of(dirs.get(rng.nextInt(dirs.size())));
    }

    private static Optional<DisposicionMapa.Colocacion> elegirColocacionConPuerta(DisposicionMapa d, Random rng) {
        List<DisposicionMapa.Colocacion> xs = new ArrayList<>(d.todas());
        Collections.shuffle(xs, rng);
        return xs.stream().filter(c -> !c.habitacion.puertas.isEmpty()).findFirst();
    }

    private static int dx(Direccion d) {
        return switch (d) {
            case ESTE -> 1;
            case OESTE -> -1;
            default -> 0;
        };
    }

    private static int dy(Direccion d) {
        return switch (d) {
            case NORTE -> 1;
            case SUR -> -1;
            default -> 0;
        };
    }
}
