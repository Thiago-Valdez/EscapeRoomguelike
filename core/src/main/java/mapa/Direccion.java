package mapa;

public enum Direccion {
    NORTE, SUR, ESTE, OESTE;

    public Direccion opuesta() {
        return switch (this) {
            case NORTE -> SUR;
            case SUR   -> NORTE;
            case ESTE  -> OESTE;
            case OESTE -> ESTE;
        };
    }
}
