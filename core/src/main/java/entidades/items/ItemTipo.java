package entidades.items;

import entidades.Item;
import entidades.Jugador;

import java.util.Random;

public enum ItemTipo {

    CORAZON_EXTRA(RarezaItem.RARO, new Item() {
        @Override
        public String getNombre() { return "Corazón Extra"; }

        @Override
        public String getDescripcion() { return "Aumenta la vida máxima en +1 y cura completamente."; }

        @Override
        public void aplicar(Jugador j) {
            j.setVidaMaxima(j.getVidaMaxima() + 1);
            j.setVida(j.getVidaMaxima());
        }
    }),

    BOTA_VELOZ(RarezaItem.COMUN, new Item() {
        @Override
        public String getNombre() { return "Bota Veloz"; }

        @Override
        public String getDescripcion() { return "Aumenta la velocidad en +50."; }

        @Override
        public void aplicar(Jugador j) {
            j.setVelocidad(j.getVelocidad() + 50f);
        }
    }),

    CHOCOLATE_BLANCO(RarezaItem.COMUN, new Item() {
        @Override
        public String getNombre() { return "Chocolate Blanco"; }

        @Override
        public String getDescripcion() { return "Restaura 1 punto de vida."; }

        @Override
        public void aplicar(Jugador j) {
            j.setVida(j.getVida() + 1);
        }
    }),

    RELOJ_ROTO(RarezaItem.RARO, new Item() {
        @Override
        public String getNombre() { return "Reloj Roto"; }

        @Override
        public String getDescripcion() { return "Reduce tu velocidad en 40."; }

        @Override
        public void aplicar(Jugador j) {
            j.setVelocidad(Math.max(10f, j.getVelocidad() - 40f));
        }
    }),

    ARMADURA_LIGERA(RarezaItem.EPICO, new Item() {
        @Override
        public String getNombre() { return "Armadura Ligera"; }

        @Override
        public String getDescripcion() { return "Aumenta la vida máxima en +2 (no cura)."; }

        @Override
        public void aplicar(Jugador j) {
            j.setVidaMaxima(j.getVidaMaxima() + 2);
        }
    });

    // ---------------------------------------------------------
    private final RarezaItem rareza;
    private final Item itemBase;

    ItemTipo(RarezaItem rareza, Item base) {
        this.rareza = rareza;
        this.itemBase = base;
    }

    public RarezaItem getRareza() {
        return rareza;
    }

    // Devuelve una copia nueva del item base (para que no compartan estado)
    public Item crear() {
        return new Item() {
            @Override
            public String getNombre() {
                return itemBase.getNombre();
            }

            @Override
            public String getDescripcion() {
                return itemBase.getDescripcion();
            }

            @Override
            public void aplicar(Jugador j) {
                itemBase.aplicar(j);
            }
        };
    }

    public String nombre() { return itemBase.getNombre(); }

    public String descripcion() { return itemBase.getDescripcion(); }

    // -------- Selección ponderada por rareza --------
    public static ItemTipo aleatorioSegunRareza(Random rng) {
        ItemTipo[] tipos = values();

        int total = 0;
        for (ItemTipo t : tipos) total += t.rareza.getPeso();

        int r = rng.nextInt(total);

        for (ItemTipo t : tipos) {
            r -= t.rareza.getPeso();
            if (r < 0) return t;
        }

        return tipos[0];
    }
}
