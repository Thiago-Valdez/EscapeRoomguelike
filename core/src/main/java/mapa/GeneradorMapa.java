package mapa;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Genera un mapa como un "camino" de habitaciones conectadas por sus puertas.
 *
 * Reglas:
 *  - La primera sala SIEMPRE es INICIO_1.
 *  - La última sala SIEMPRE es de tipo JEFE.
 *  - Solo se coloca 1 sala BOTIN_* en todo el camino.
 *  - Las salas ACERTIJO_* y COMBATE_* pueden repetirse libremente.
 *  - La cantidad total de salas depende del nivel:
 *      nivel 1 -> entre 5 y 8
 *      nivel 2 -> entre 7 y 11
 *      nivel 3 -> entre 9 y 13
 *      etc. (se suman 2 al mínimo y máximo por nivel)
 */
public final class GeneradorMapa {

    // ---------- Configuración de entrada ----------
    public static class Configuracion {
        /** Nivel del juego (1,2,3,...) usado para calcular el rango de salas. */
        public int nivel = 1;
        /** Semilla para el RNG (si querés reproducibilidad). */
        public long semilla = System.currentTimeMillis();
    }

    private final Configuracion cfg;
    private final Random rng;

    // Listas de habitaciones por tipo (tomadas del enum Habitacion)
    private final List<Habitacion> acertijos;
    private final List<Habitacion> combates;
    private final List<Habitacion> botines;
    private final List<Habitacion> jefes;

    public GeneradorMapa(Configuracion cfg) {
        this.cfg = cfg;
        this.rng = new Random(cfg.semilla);

        // Preparamos las listas de habitaciones por tipo
        acertijos = filtrarPorTipo(TipoSala.ACERTIJO);
        combates  = filtrarPorTipo(TipoSala.COMBATE);
        botines   = filtrarPorTipo(TipoSala.BOTIN);
        jefes     = filtrarPorTipo(TipoSala.JEFE);

        if (botines.isEmpty()) {
            System.out.println("[GeneradorMapa] ADVERTENCIA: no hay habitaciones de BOTIN definidas.");
        }
        if (jefes.isEmpty()) {
            throw new IllegalStateException("Debe haber al menos una habitación de tipo JEFE en el enum Habitacion.");
        }
    }

    private List<Habitacion> filtrarPorTipo(TipoSala tipo) {
        return Arrays.stream(Habitacion.values())
            .filter(h -> h.tipo == tipo)
            .collect(Collectors.toList());
    }

    /**
     * Calcula el rango [min,max] de salas según el nivel.
     * nivel 1 -> [5,8]
     * nivel 2 -> [7,11]
     * nivel 3 -> [9,13]
     * ...
     */
    private int[] rangoSalasParaNivel(int nivel) {
        int min = 5 + (nivel - 1) * 2;
        int max = 8 + (nivel - 1) * 2;
        // Aseguramos al menos espacio para INICIO + algo + JEFE
        if (min < 3) min = 3;
        if (max < min) max = min;
        return new int[]{min, max};
    }

    // -------------------------------------------------------------
    // MÉTODO PRINCIPAL: genera la DisposicionMapa
    // -------------------------------------------------------------
    public DisposicionMapa generar() {
        int[] rango = rangoSalasParaNivel(cfg.nivel);
        int minSalas = rango[0];
        int maxSalas = rango[1];

        int objetivo = randomEntero(minSalas, maxSalas);

        System.out.println("[GeneradorMapa] Nivel " + cfg.nivel +
            " -> rango de salas = [" + minSalas + "," + maxSalas + "], elegido = " + objetivo);

        DisposicionMapa disp = new DisposicionMapa();

        // --- 1) Colocar sala de inicio en (0,0) ---
        Habitacion inicio = Habitacion.INICIO_1;
        DisposicionMapa.Colocacion actual = new DisposicionMapa.Colocacion(inicio, 0, 0);
        disp.agregar(actual);

        // Queremos al menos 3 salas: INICIO + ... + JEFE
        if (objetivo < 3) objetivo = 3;

        // --- 2) Decidir en qué posición irá el BOTÍN (si cabe) ---
        // Elegimos un índice entre 2 y objetivo-1 para la sala de BOTÍN (si hay espacio).
        int indiceBotin = -1;
        if (objetivo > 3 && !botines.isEmpty()) {
            indiceBotin = randomEntero(2, objetivo - 1); // la numeración de pasos arranca en 1 (INICIO)
        }

        boolean jefeColocado = false;
        boolean botinColocado = false;

        // --- 3) Colocar el resto de salas una por una ---
        // Paso 1: INICIO (ya colocado)
        // Paso i (2..objetivo): tipo según reglas (BOTIN, JEFE, ACERTIJO/COMBATE)
        for (int paso = 2; paso <= objetivo; paso++) {

            TipoSala tipoDeseado;
            if (paso == objetivo) {
                tipoDeseado = TipoSala.JEFE; // última sala: JEFE
            } else if (!botinColocado && paso == indiceBotin) {
                tipoDeseado = TipoSala.BOTIN; // posición reservada para BOTIN
            } else {
                // Sala intermedia: puede ser ACERTIJO o COMBATE
                tipoDeseado = rng.nextBoolean() ? TipoSala.ACERTIJO : TipoSala.COMBATE;
            }

            boolean colocado = colocarSiguienteSala(disp, tipoDeseado);
            if (!colocado) {
                // Si no pudimos colocar exactamente ese tipo, intentamos degradar la regla:
                // 1) Si era BOTIN, lo saltamos y seguimos con ACERTIJO/COMBATE.
                // 2) Si era JEFE, lo intentamos abajo de otra forma.
                if (tipoDeseado == TipoSala.BOTIN) {
                    System.out.println("[GeneradorMapa] No se pudo colocar la sala BOTIN en el paso " + paso + ". Se continúa sin BOTIN.");
                    botinColocado = false;
                    continue;
                } else if (tipoDeseado == TipoSala.JEFE) {
                    // Intentamos colocar JEFE pegado a cualquier sala existente
                    System.out.println("[GeneradorMapa] No se pudo colocar JEFE como último paso. Intentando ubicarlo en cualquier posición válida...");
                    boolean jefeEnOtroLado = colocarJefeEnCualquierLado(disp);
                    if (!jefeEnOtroLado) {
                        throw new IllegalStateException("No se pudo colocar ninguna sala JEFE conectada al mapa.");
                    } else {
                        jefeColocado = true;
                    }
                    break;
                } else {
                    // ACERTIJO/COMBATE que no se pudo colocar (muy raro con las salas que definiste)
                    System.out.println("[GeneradorMapa] ADVERTENCIA: no se pudo colocar sala de tipo " + tipoDeseado + " en el paso " + paso);
                }
            } else {
                if (tipoDeseado == TipoSala.BOTIN) botinColocado = true;
                if (tipoDeseado == TipoSala.JEFE) jefeColocado = true;
            }
        }

        // Si aún no se colocó JEFE por alguna razón, intentamos forzarlo
        if (!jefeColocado) {
            System.out.println("[GeneradorMapa] ADVERTENCIA: no se colocó JEFE durante el bucle principal. Intentando forzarlo...");
            boolean ok = colocarJefeEnCualquierLado(disp);
            if (!ok) {
                throw new IllegalStateException("No se pudo colocar ninguna sala JEFE conectada al mapa (forzado).");
            }
        }

        // Debug sencillo: lista de habitaciones
        System.out.println("[GeneradorMapa] Mapa generado con " + disp.todas().size() + " salas:");
        for (DisposicionMapa.Colocacion c : disp.todas()) {
            System.out.println("  - " + c.habitacion.nombreVisible + " (" + c.habitacion.tipo +
                ") en [" + c.gx + "," + c.gy + "]");
        }

        return disp;
    }

    // -------------------------------------------------------------
    // Lógica para colocar una nueva sala de un tipo deseado
    // -------------------------------------------------------------
    private boolean colocarSiguienteSala(DisposicionMapa disp, TipoSala tipoDeseado) {
        int intentos = 200; // alto para ser robustos

        while (intentos-- > 0) {
            // Elegimos una sala ya colocada como "base" desde donde expandir
            List<DisposicionMapa.Colocacion> existentes = new ArrayList<>(disp.todas());
            if (existentes.isEmpty()) return false;
            DisposicionMapa.Colocacion base = existentes.get(rng.nextInt(existentes.size()));

            // Mezclamos las direcciones de las puertas de la sala base
            List<Direccion> dirs = new ArrayList<>(base.habitacion.puertas.keySet());
            if (dirs.isEmpty()) {
                continue;
            }
            Collections.shuffle(dirs, rng);

            for (Direccion salida : dirs) {
                int nx = base.gx + dx(salida);
                int ny = base.gy + dy(salida);
                if (disp.ocupada(nx, ny)) {
                    continue; // ya hay una sala ahí
                }

                Direccion entrada = salida.opuesta();

                // Buscamos una habitación del tipo adecuado y con puerta en la dirección "entrada"
                List<Habitacion> candidatas = candidatasPorTipoYEntrada(tipoDeseado, entrada);
                if (candidatas.isEmpty()) {
                    continue; // probar otra dirección
                }

                Habitacion elegida = candidatas.get(rng.nextInt(candidatas.size()));
                DisposicionMapa.Colocacion nueva = new DisposicionMapa.Colocacion(elegida, nx, ny);
                disp.agregar(nueva);
                return true;
            }
        }

        return false; // no se pudo colocar nada
    }

    private List<Habitacion> candidatasPorTipoYEntrada(TipoSala tipo, Direccion entrada) {
        List<Habitacion> base;
        switch (tipo) {
            case ACERTIJO -> base = acertijos;
            case COMBATE  -> base = combates;
            case BOTIN    -> base = botines;
            case JEFE     -> base = jefes;
            default -> base = List.of();
        }
        if (base.isEmpty()) return List.of();
        List<Habitacion> out = new ArrayList<>();
        for (Habitacion h : base) {
            if (h.tienePuerta(entrada)) {
                out.add(h);
            }
        }
        return out;
    }

    /**
     * Intenta colocar una sala JEFE pegada a cualquier sala ya existente.
     * Si tiene éxito, la agrega al mapa y devuelve true.
     */
    private boolean colocarJefeEnCualquierLado(DisposicionMapa disp) {
        int intentos = 400;

        while (intentos-- > 0) {
            List<DisposicionMapa.Colocacion> existentes = new ArrayList<>(disp.todas());
            if (existentes.isEmpty()) return false;

            DisposicionMapa.Colocacion base = existentes.get(rng.nextInt(existentes.size()));

            List<Direccion> dirs = new ArrayList<>(base.habitacion.puertas.keySet());
            if (dirs.isEmpty()) {
                continue;
            }
            Collections.shuffle(dirs, rng);

            for (Direccion salida : dirs) {
                int nx = base.gx + dx(salida);
                int ny = base.gy + dy(salida);
                if (disp.ocupada(nx, ny)) {
                    continue;
                }
                Direccion entrada = salida.opuesta();
                List<Habitacion> candidatas = candidatasPorTipoYEntrada(TipoSala.JEFE, entrada);
                if (candidatas.isEmpty()) {
                    continue;
                }
                Habitacion jefe = candidatas.get(rng.nextInt(candidatas.size()));
                DisposicionMapa.Colocacion nueva = new DisposicionMapa.Colocacion(jefe, nx, ny);
                disp.agregar(nueva);
                return true;
            }
        }

        return false;
    }

    // -------------------------------------------------------------
    // Helpers varios
    // -------------------------------------------------------------
    private int randomEntero(int minIncl, int maxIncl) {
        if (maxIncl < minIncl) return minIncl;
        if (maxIncl == minIncl) return minIncl;
        return minIncl + rng.nextInt(maxIncl - minIncl + 1);
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
            case SUR   -> -1;
            default -> 0;
        };
    }
}
