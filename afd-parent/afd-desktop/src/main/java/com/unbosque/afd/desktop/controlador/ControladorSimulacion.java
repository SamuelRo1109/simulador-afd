package com.unbosque.afd.desktop.controlador;

import com.unbosque.afd.core.logica.SimuladorAFD;
import com.unbosque.afd.core.modelo.AutomataFinitoDeterminista;
import com.unbosque.afd.core.modelo.Estado;
import com.unbosque.afd.core.modelo.MotivoRechazo;
import com.unbosque.afd.core.modelo.PasoEjecucion;
import com.unbosque.afd.core.modelo.ResultadoSimulacion;

import javax.swing.Timer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class ControladorSimulacion {

    public static final int VELOCIDAD_POR_DEFECTO = 700;
    public static final int PASO_INICIAL = -1;

    private final ControladorAutomata controlador;
    private final List<Runnable> observadores = new ArrayList<>();
    private final Timer temporizador;

    private ResultadoSimulacion resultado;
    private AutomataFinitoDeterminista automataSimulado;
    private String cadena = "";
    private int paso = PASO_INICIAL;

    public ControladorSimulacion(ControladorAutomata controlador) {
        this.controlador = Objects.requireNonNull(controlador, "El controlador no puede ser nulo");
        this.temporizador = new Timer(VELOCIDAD_POR_DEFECTO, evento -> avanzarAutomaticamente());
        this.temporizador.setRepeats(true);
    }

    public void agregarObservador(Runnable observador) {
        observadores.add(Objects.requireNonNull(observador, "El observador no puede ser nulo"));
    }

    public void simular(String nuevaCadena) {
        Objects.requireNonNull(nuevaCadena, "La cadena no puede ser nula");
        AutomataFinitoDeterminista automata = controlador.automataActual();
        if (automata == null || !controlador.esValido()) {
            return;
        }
        pausar();
        cadena = nuevaCadena;
        automataSimulado = automata;
        resultado = SimuladorAFD.simular(automata, nuevaCadena);
        paso = PASO_INICIAL;
        notificar();
    }

    public boolean hayResultado() {
        return resultado != null;
    }

    public ResultadoSimulacion resultado() {
        return resultado;
    }

    public String cadena() {
        return cadena;
    }

    public int paso() {
        return paso;
    }

    public int totalPasos() {
        return resultado == null ? 0 : resultado.cantidadPasos();
    }

    public PasoEjecucion pasoActual() {
        if (resultado == null || paso < 0 || paso >= resultado.cantidadPasos()) {
            return null;
        }
        return resultado.pasos().get(paso);
    }

    public Estado estadoActual() {
        if (resultado == null) {
            return null;
        }
        PasoEjecucion actual = pasoActual();
        if (actual == null) {
            return automataSimulado == null ? null : automataSimulado.estadoInicial();
        }
        return actual.estadoDestino() == null ? actual.estadoOrigen() : actual.estadoDestino();
    }

    public boolean hayVeredicto() {
        return resultado != null && paso == totalPasos() - 1;
    }

    public boolean puedeAvanzar() {
        return resultado != null && paso < totalPasos() - 1;
    }

    public boolean puedeRetroceder() {
        return resultado != null && paso > PASO_INICIAL;
    }

    public void irAPaso(int nuevoPaso) {
        if (resultado == null) {
            return;
        }
        int destino = Math.max(PASO_INICIAL, Math.min(nuevoPaso, totalPasos() - 1));
        if (destino == paso) {
            return;
        }
        paso = destino;
        notificar();
    }

    public void siguiente() {
        irAPaso(paso + 1);
    }

    public void anterior() {
        irAPaso(paso - 1);
    }

    public void inicio() {
        irAPaso(PASO_INICIAL);
    }

    public void fin() {
        irAPaso(totalPasos() - 1);
    }

    public void reiniciar() {
        pausar();
        inicio();
    }

    public void reproducir() {
        if (resultado == null || !puedeAvanzar()) {
            return;
        }
        temporizador.start();
        notificar();
    }

    public void pausar() {
        if (temporizador.isRunning()) {
            temporizador.stop();
            notificar();
        }
    }

    public void alternarReproduccion() {
        if (reproduciendo()) {
            pausar();
        } else {
            if (!puedeAvanzar()) {
                inicio();
            }
            reproducir();
        }
    }

    public boolean reproduciendo() {
        return temporizador.isRunning();
    }

    public void establecerVelocidad(int milisegundos) {
        temporizador.setDelay(Math.max(50, milisegundos));
        temporizador.setInitialDelay(Math.max(50, milisegundos));
    }

    public int velocidad() {
        return temporizador.getDelay();
    }

    public void cancelar() {
        boolean habiaAlgo = resultado != null || temporizador.isRunning() || paso != PASO_INICIAL;
        temporizador.stop();
        resultado = null;
        automataSimulado = null;
        paso = PASO_INICIAL;
        if (habiaAlgo) {
            notificar();
        }
    }

    public String descripcionPaso() {
        if (resultado == null) {
            return "";
        }
        if (totalPasos() == 0) {
            return "Cadena vacía (λ) — no se lee ningún símbolo";
        }
        PasoEjecucion actual = pasoActual();
        if (actual == null) {
            String inicial = automataSimulado != null && automataSimulado.tieneEstadoInicial()
                    ? automataSimulado.estadoInicial().nombre()
                    : "?";
            return "Listo — posado en " + inicial + ", sin leer nada";
        }
        String encabezado = "Paso " + (paso + 1) + " de " + totalPasos();
        if (actual.estadoDestino() == null) {
            return encabezado + " — leo '" + actual.simbolo() + "' en "
                    + actual.estadoOrigen().nombre() + ", no hay a dónde ir";
        }
        return encabezado + " — leo '" + actual.simbolo() + "' en " + actual.estadoOrigen().nombre()
                + ", voy a " + actual.estadoDestino().nombre();
    }

    public String mensajeVeredicto() {
        if (resultado == null) {
            return "";
        }
        if (resultado.aceptada()) {
            return "Cadena ACEPTADA — terminó en " + nombre(resultado.estadoFinal())
                    + ", que es de aceptación";
        }
        return "Cadena RECHAZADA — " + explicacionRechazo();
    }

    public String explicacionRechazo() {
        if (resultado == null) {
            return "";
        }
        MotivoRechazo motivo = resultado.motivoRechazo();
        PasoEjecucion ultimo = resultado.pasos().isEmpty()
                ? null
                : resultado.pasos().get(resultado.pasos().size() - 1);
        return switch (motivo) {
            case ESTADO_NO_ACEPTACION -> "terminó en " + nombre(resultado.estadoFinal())
                    + ", que no es de aceptación";
            case SIMBOLO_FUERA_ALFABETO -> ultimo == null
                    ? "hay un símbolo que no pertenece a Σ"
                    : "el símbolo '" + ultimo.simbolo() + "' no pertenece a Σ";
            case TRANSICION_NO_DEFINIDA -> ultimo == null
                    ? "el autómata no tiene estado inicial"
                    : "δ(" + ultimo.estadoOrigen().nombre() + ", " + ultimo.simbolo() + ") no está definida";
            case NINGUNO -> "sin motivo registrado";
        };
    }

    public String traza() {
        if (resultado == null) {
            return "";
        }
        StringBuilder registro = new StringBuilder();
        registro.append("Autómata: ").append(controlador.modelo().nombre()).append('\n');
        registro.append("Cadena: ").append(cadena.isEmpty() ? "λ (vacía)" : '"' + cadena + '"').append('\n');
        registro.append('\n');

        if (resultado.pasos().isEmpty()) {
            registro.append("No hay pasos: la cadena vacía no consume símbolos.\n");
        }
        for (PasoEjecucion ejecucion : resultado.pasos()) {
            registro.append("Paso ").append(ejecucion.indice() + 1).append(": ")
                    .append(ejecucion.estadoOrigen().nombre())
                    .append(" --").append(ejecucion.simbolo()).append("--> ")
                    .append(ejecucion.estadoDestino() == null ? "(indefinido)" : ejecucion.estadoDestino().nombre())
                    .append('\n');
        }

        registro.append('\n').append("Recorrido: ").append(recorrido()).append('\n');
        registro.append("Conclusión: ").append(mensajeVeredicto()).append('\n');
        return registro.toString();
    }

    public String recorrido() {
        if (resultado == null) {
            return "";
        }
        if (resultado.pasos().isEmpty()) {
            return nombre(automataSimulado == null ? null : automataSimulado.estadoInicial());
        }
        StringBuilder cadenaRecorrido = new StringBuilder(resultado.pasos().get(0).estadoOrigen().nombre());
        for (PasoEjecucion ejecucion : resultado.pasos()) {
            cadenaRecorrido.append(" --").append(ejecucion.simbolo()).append("--> ")
                    .append(ejecucion.estadoDestino() == null
                            ? "(indefinido)" : ejecucion.estadoDestino().nombre());
        }
        return cadenaRecorrido.toString();
    }

    private void avanzarAutomaticamente() {
        if (!puedeAvanzar()) {
            pausar();
            return;
        }
        siguiente();
        if (!puedeAvanzar()) {
            pausar();
        }
    }

    private static String nombre(Estado estado) {
        return estado == null ? "?" : estado.nombre();
    }

    private void notificar() {
        for (Runnable observador : new ArrayList<>(observadores)) {
            observador.run();
        }
    }
}
