package com.unbosque.afd.desktop.controlador.comandos;

public interface Comando {

    void ejecutar();

    void deshacer();

    String descripcion();
}
