package com.unbosque.afd.desktop.controlador.comandos;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class ComandoInstantanea<T> implements Comando {

    private final String descripcion;
    private final Supplier<T> captor;
    private final Consumer<T> restaurador;
    private final Runnable mutacion;

    private T antes;
    private T despues;

    public ComandoInstantanea(String descripcion,
                              Supplier<T> captor,
                              Consumer<T> restaurador,
                              Runnable mutacion) {
        this.descripcion = Objects.requireNonNull(descripcion, "La descripcion no puede ser nula");
        this.captor = Objects.requireNonNull(captor, "El captor no puede ser nulo");
        this.restaurador = Objects.requireNonNull(restaurador, "El restaurador no puede ser nulo");
        this.mutacion = Objects.requireNonNull(mutacion, "La mutacion no puede ser nula");
    }

    @Override
    public void ejecutar() {
        if (despues == null) {
            antes = captor.get();
            mutacion.run();
            despues = captor.get();
        } else {
            restaurador.accept(despues);
        }
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
