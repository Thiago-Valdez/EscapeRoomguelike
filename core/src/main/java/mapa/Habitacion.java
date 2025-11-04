package mapa;

import com.badlogic.gdx.math.Rectangle;
import java.util.EnumMap;
import java.util.Map;

/**
 * Cada constante representa una habitación dibujada dentro de la imagen gigante.
 * srcX/srcY/width/height son coordenadas del rectángulo dentro de esa imagen.
 */
public enum Habitacion {
    // === EJEMPLOS: reemplaza srcX/srcY/width/height y puertas por tus datos reales ===
    INICIO_1(
        1, "Inicio 1", TipoSala.INICIO,
        0, 0, 512, 512,
        Map.of(
            Direccion.ESTE,  new EspecificacionPuerta(Direccion.ESTE,  512, 256),
            Direccion.SUR,   new EspecificacionPuerta(Direccion.SUR,   256, 0)
        )
    ),

    ACERTIJO_A(
        2, "Acertijo A", TipoSala.ACERTIJO,
        512, 0, 512, 512,
        Map.of(
            Direccion.OESTE, new EspecificacionPuerta(Direccion.OESTE, 0,   256),
            Direccion.SUR,   new EspecificacionPuerta(Direccion.SUR,   256, 0)
        )
    ),

    COMBATE_A(
        3, "Combate A", TipoSala.COMBATE,
        1024, 0, 512, 512,
        Map.of(
            Direccion.OESTE, new EspecificacionPuerta(Direccion.OESTE, 0,   256),
            Direccion.NORTE, new EspecificacionPuerta(Direccion.NORTE, 256, 512)
        )
    ),

    BOTIN_A(
        4, "Botín A", TipoSala.BOTIN,
        1536, 0, 512, 512,
        Map.of(
            Direccion.NORTE, new EspecificacionPuerta(Direccion.NORTE, 256, 512),
            Direccion.ESTE,  new EspecificacionPuerta(Direccion.ESTE,  512, 256)
        )
    ),

    JEFE_A(
        5, "Jefe A", TipoSala.JEFE,
        2048, 0, 512, 512,
        Map.of(
            Direccion.OESTE, new EspecificacionPuerta(Direccion.OESTE, 0,   256),
            Direccion.NORTE, new EspecificacionPuerta(Direccion.NORTE, 256, 512)
        )
    );

    public final int id;
    public final String nombreVisible;
    public final TipoSala tipo;
    public final int srcX, srcY, ancho, alto;
    public final EnumMap<Direccion, EspecificacionPuerta> puertas;

    Habitacion(int id, String nombreVisible, TipoSala tipo,
               int srcX, int srcY, int ancho, int alto,
               Map<Direccion, EspecificacionPuerta> puertas) {
        this.id = id;
        this.nombreVisible = nombreVisible;
        this.tipo = tipo;
        this.srcX = srcX;
        this.srcY = srcY;
        this.ancho = ancho;
        this.alto = alto;
        this.puertas = new EnumMap<>(Direccion.class);
        this.puertas.putAll(puertas);
    }

    public boolean tienePuerta(Direccion d) {
        return puertas.containsKey(d);
    }

    public Rectangle rectFuente() {
        return new Rectangle(srcX, srcY, ancho, alto);
    }
}
