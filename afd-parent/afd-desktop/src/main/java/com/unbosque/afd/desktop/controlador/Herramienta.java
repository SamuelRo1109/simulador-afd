package com.unbosque.afd.desktop.controlador;

public enum Herramienta {

    SELECCIONAR("Seleccionar", 'V', "Seleccionar, mover y editar"),
    ESTADO("Estado", 'E', "Clic en el lienzo inserta un estado"),
    ACEPTACION("Aceptación", 'A', "Clic inserta o conmuta un estado de aceptación"),
    INICIAL("Inicial", 'I', "Clic en un estado lo marca como inicial"),
    TRANSICION("Transición", 'T', "Arrastra de un estado a otro"),
    BORRAR("Borrar", 'D', "Clic elimina el elemento bajo el cursor"),
    MANO("Mano", 'H', "Paneo explícito del lienzo");

    private final String etiqueta;
    private final char atajo;
    private final String descripcion;

    Herramienta(String etiqueta, char atajo, String descripcion) {
        this.etiqueta = etiqueta;
        this.atajo = atajo;
        this.descripcion = descripcion;
    }

    public String etiqueta() {
        return etiqueta;
    }

    public char atajo() {
        return atajo;
    }

    public String descripcion() {
        return descripcion;
    }

    public boolean insertaEstados() {
        return this == ESTADO || this == ACEPTACION;
    }
}
