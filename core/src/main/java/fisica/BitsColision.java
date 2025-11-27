package fisica;

/** Categorías y máscaras de colisión (filtros) */
public class BitsColision {
    public static final short CATEGORIA_JUGADOR = 0x0001;
    public static final short CATEGORIA_PARED   = 0x0002;
    public static final short CATEGORIA_PUERTA  = 0x0004; // sensor

    // Máscaras: con qué colisiona cada categoría
    public static final short MASCARA_JUGADOR = (short)(CATEGORIA_PARED | CATEGORIA_PUERTA);
    public static final short MASCARA_PARED   = (short)(CATEGORIA_JUGADOR);
    public static final short MASCARA_PUERTA  = (short)(CATEGORIA_JUGADOR);
}
