package com.unbosque.afd.desktop.controlador.comandos;

import java.util.Objects;
import java.util.function.Consumer;

public final class ComandoDirecto<T> implements Comando {

    private final String descripcion;
    private final T antes;
    private final T despues;
    private final Consumer<T> restaurador;

    public ComandoDirecto(String descripcion, T antes, T despues, Consumer<T> restaurador) {
        this.descripcion = Objects.requireNonNull(descripcion, "La descripcion no puede ser nula");
        this.antes = Objects.requireNonNull(antes, "El estado anterior no puede ser nulo");
        this.despues = Objects.requireNonNull(despues, "El estado posterior no puede ser nulo");
        this.restaurador = Objects.requireNonNull(restaurador, "El restaurador no puede ser nulo");
    }

    @Override
    public void ejecutar() {
        restaurador.accept(despues);
    }

    @Override
    public void deshacer() {
        restaurador.accept(antes);
    }

    @Override
    public String descripcion() {
        return descripcion;
    }
}
