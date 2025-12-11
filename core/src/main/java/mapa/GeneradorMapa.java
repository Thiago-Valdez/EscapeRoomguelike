package mapa;

import com.badlogic.gdx.Gdx;

import java.util.*;

/**
 * Genera un camino de habitaciones usando el GrafoPuertas y ciertas
 * restricciones por nivel.
 */
public class GeneradorMapa {

    // --------- Config pública que usás desde JuegoPrincipal ---------

    public static class Configuracion {
        public int nivel = 1;
        public long semilla = System.currentTimeMillis();
    }

    // Config interna por nivel
    private static class NivelCfg {
        final int minSalas;
        final int maxSalas;
        final int minAcertijos;
        final int minCombates;
        final boolean requiereBotin;
        final boolean terminaEnJefe;

        NivelCfg(int minSalas, int maxSalas,
                 int minAcertijos, int minCombates,
                 boolean requiereBotin,
                 boolean terminaEnJefe) {
            this.minSalas = minSalas;
            this.maxSalas = maxSalas;
            this.minAcertijos = minAcertijos;
            this.minCombates = minCombates;
            this.requiereBotin = requiereBotin;
            this.terminaEnJefe = terminaEnJefe;
        }
    }

    private final Configuracion cfg;
    private final GrafoPuertas grafo;
    private final Random rng;

    public GeneradorMapa(Configuracion cfg, GrafoPuertas grafo) {
        this.cfg = cfg;
        this.grafo = grafo;
        this.rng = new Random(cfg.semilla);
    }

    // --------- API principal ---------

    public DisposicionMapa generar() {
        NivelCfg nivelCfg = elegirCfgNivel(cfg.nivel);

        Habitacion inicio = Habitacion.INICIO_1;

        // Buscamos TODOS los caminos válidos que terminen en JEFE
        List<List<Habitacion>> candidatos = new ArrayList<>();
        List<Habitacion> path = new ArrayList<>();
        Set<Habitacion> visitados = new HashSet<>();

        path.add(inicio);
        visitados.add(inicio);

        dfsTodos(inicio, nivelCfg, path, visitados, candidatos);

        List<Habitacion> mejor;

        if (candidatos.isEmpty()) {
            // Fallback: camino mínimo Inicio -> Jefe aleatorio
            Habitacion jefeFallback = elegirJefeAleatorio();
            mejor = new ArrayList<>();
            mejor.add(inicio);
            mejor.add(jefeFallback);

            Gdx.app.log("GeneradorMapa",
                "No se pudo generar un camino completo, usando fallback simple.");
            imprimirCamino("CAMINO FALLBACK", mejor);
        } else {
            // Elegimos entre los candidatos el/los más largos
            int maxLen = 0;
            for (List<Habitacion> c : candidatos) {
                maxLen = Math.max(maxLen, c.size());
            }

            List<List<Habitacion>> masLargos = new ArrayList<>();
            for (List<Habitacion> c : candidatos) {
                if (c.size() == maxLen) {
                    masLargos.add(c);
                }
            }

            mejor = masLargos.get(rng.nextInt(masLargos.size()));
            imprimirCamino("CAMINO GENERADO", mejor);
        }

        return new DisposicionMapa();
    }

    // --------- DFS que encuentra TODOS los caminos válidos ---------

    private void dfsTodos(Habitacion actual,
                          NivelCfg nivelCfg,
                          List<Habitacion> path,
                          Set<Habitacion> visitados,
                          List<List<Habitacion>> candidatos) {

        int n = path.size();
        if (n > nivelCfg.maxSalas) return;

        // Si estamos en un Jefe, este camino sólo es válido si cumple restricciones
        if (actual.tipo == TipoSala.JEFE) {
            if (nivelCfg.terminaEnJefe && cumpleRestricciones(path, nivelCfg)) {
                candidatos.add(new ArrayList<>(path));
            }
            // No seguimos más allá del jefe
            return;
        }

        // Armamos vecinos aleatorios para dar variedad
        List<Habitacion> vecinos = new ArrayList<>(grafo.vecinas(actual));
        Collections.shuffle(vecinos, rng);

        for (Habitacion sig : vecinos) {
            if (visitados.contains(sig)) continue;

            path.add(sig);
            visitados.add(sig);

            dfsTodos(sig, nivelCfg, path, visitados, candidatos);

            // backtracking
            path.remove(path.size() - 1);
            visitados.remove(sig);
        }
    }

    private boolean cumpleRestricciones(List<Habitacion> path, NivelCfg nivelCfg) {
        int n = path.size();

        if (n < nivelCfg.minSalas || n > nivelCfg.maxSalas) return false;

        Habitacion ultima = path.get(n - 1);
        if (nivelCfg.terminaEnJefe && ultima.tipo != TipoSala.JEFE) {
            return false;
        }

        int cAcertijo = 0;
        int cCombate = 0;
        int cBotin = 0;

        for (Habitacion h : path) {
            switch (h.tipo) {
                case ACERTIJO -> cAcertijo++;
                case COMBATE -> cCombate++;
                case BOTIN -> cBotin++;
                default -> {}
            }
        }

        if (cAcertijo < nivelCfg.minAcertijos) return false;
        if (cCombate < nivelCfg.minCombates) return false;
        if (nivelCfg.requiereBotin && cBotin == 0) return false;

        return true;
    }

    // --------- Helpers ---------

    private NivelCfg elegirCfgNivel(int nivel) {
        return switch (nivel) {
            case 1 -> new NivelCfg(5, 7, 2, 2, true, true);
            case 2 -> new NivelCfg(7, 9, 3, 3, true, true);
            case 3 -> new NivelCfg(9, 11, 4, 4, true, true);
            default -> new NivelCfg(5, 7, 2, 2, true, true);
        };
    }

    private Habitacion elegirJefeAleatorio() {
        List<Habitacion> jefes = new ArrayList<>();
        for (Habitacion h : Habitacion.values()) {
            if (h.tipo == TipoSala.JEFE) {
                jefes.add(h);
            }
        }
        if (jefes.isEmpty()) {
            throw new IllegalStateException("No hay habitaciones de JEFE definidas.");
        }
        return jefes.get(rng.nextInt(jefes.size()));
    }

    private void imprimirCamino(String titulo, List<Habitacion> camino) {
        System.out.println("== " + titulo + " ==");
        for (Habitacion h : camino) {
            System.out.println(" - " + h.nombreVisible +
                " (" + h.gridX + "," + h.gridY + ")");
        }
    }
}
