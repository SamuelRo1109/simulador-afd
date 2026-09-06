package com.unbosque.afd.desktop.controlador.comandos;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

public final class PilaComandos {

    public static final int PROFUNDIDAD = 50;

    private final Deque<Comando> hechos = new ArrayDeque<>();
    private final Deque<Comando> deshechos = new ArrayDeque<>();

    public void ejecutar(Comando comando) {
        Objects.requireNonNull(comando, "El comando no puede ser nulo");
        comando.ejecutar();
        hechos.push(comando);
        while (hechos.size() > PROFUNDIDAD) {
            hechos.removeLast();
        }
        deshechos.clear();
    }

    public boolean puedeDeshacer() {
        return !hechos.isEmpty();
    }

    public boolean puedeRehacer() {
        return !deshechos.isEmpty();
    }

    public String deshacer() {
        if (hechos.isEmpty()) {
            return null;
        }
        Comando comando = hechos.pop();
        comando.deshacer();
        deshechos.push(comando);
        return comando.descripcion();
    }

    public String rehacer() {
        if (deshechos.isEmpty()) {
            return null;
        }
        Comando comando = deshechos.pop();
        comando.ejecutar();
        hechos.push(comando);
        return comando.descripcion();
    }

    public void limpiar() {
        hechos.clear();
        deshechos.clear();
    }

    public int profundidadDeshacer() {
        return hechos.size();
    }

    public int profundidadRehacer() {
        return deshechos.size();
    }
}
