package com.unbosque.afd.desktop.controlador;

import com.unbosque.afd.core.logica.DistribuidorEstados;
import com.unbosque.afd.core.logica.ValidadorAutomata;
import com.unbosque.afd.core.modelo.AutomataFinitoDeterminista;
import com.unbosque.afd.core.modelo.Estado;
import com.unbosque.afd.core.modelo.Punto;
import com.unbosque.afd.core.modelo.ResultadoValidacion;
import com.unbosque.afd.desktop.render.NodoGrafico;
import com.unbosque.afd.desktop.vista.LienzoAutomata;
import com.unbosque.afd.desktop.vista.PanelAlfabeto;
import com.unbosque.afd.desktop.vista.PanelEstados;
import com.unbosque.afd.desktop.vista.PanelMatrizTransiciones;
import com.unbosque.afd.desktop.vista.PanelValidacion;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class ControladorAutomata {

    private static final double RADIO_DISTRIBUCION = 0.34;

    private final ModeloEdicion modelo = new ModeloEdicion();
    private final Map<String, Punto> posiciones = new LinkedHashMap<>();

    private LienzoAutomata lienzo;
    private PanelAlfabeto panelAlfabeto;
    private PanelEstados panelEstados;
    private PanelMatrizTransiciones panelMatriz;
    private PanelValidacion panelValidacion;

    private AutomataFinitoDeterminista automataActual;
    private ResultadoValidacion validacionActual = ResultadoValidacion.vacio();

    public void registrarVista(LienzoAutomata lienzo,
                               PanelAlfabeto panelAlfabeto,
                               PanelEstados panelEstados,
                               PanelMatrizTransiciones panelMatriz,
                               PanelValidacion panelValidacion) {
        this.lienzo = Objects.requireNonNull(lienzo, "El lienzo no puede ser nulo");
        this.panelAlfabeto = Objects.requireNonNull(panelAlfabeto, "El panel de alfabeto no puede ser nulo");
        this.panelEstados = Objects.requireNonNull(panelEstados, "El panel de estados no puede ser nulo");
        this.panelMatriz = Objects.requireNonNull(panelMatriz, "El panel de matriz no puede ser nulo");
        this.panelValidacion = Objects.requireNonNull(panelValidacion, "El panel de validacion no puede ser nulo");
    }

    public ModeloEdicion modelo() {
        return modelo;
    }

    public AutomataFinitoDeterminista automataActual() {
        return automataActual;
    }

    public ResultadoValidacion validacionActual() {
        return validacionActual;
    }

    public boolean agregarSimbolo(String texto) {
        if (texto == null || texto.isBlank() || texto.trim().length() != 1) {
            return false;
        }
        if (!modelo.agregarSimbolo(texto.trim().charAt(0))) {
            return false;
        }
        sincronizar();
        return true;
    }

    public void quitarSimbolo(char simbolo) {
        modelo.quitarSimbolo(simbolo);
        sincronizar();
    }

    public boolean agregarEstado(String nombre) {
        if (!modelo.agregarEstado(nombre == null ? null : nombre.trim())) {
            return false;
        }
        sincronizar();
        return true;
    }

    public void eliminarEstado(String nombre) {
        modelo.eliminarEstado(nombre);
        posiciones.remove(nombre);
        sincronizar();
    }

    public void establecerInicial(String nombre) {
        modelo.establecerInicial(nombre);
        sincronizar();
    }

    public void establecerAceptacion(String nombre, boolean aceptacion) {
        modelo.establecerAceptacion(nombre, aceptacion);
        sincronizar();
    }

    public void establecerTransicion(String estado, char simbolo, String destino) {
        modelo.establecerTransicion(estado, simbolo, destino);
        sincronizar();
    }

    public void nuevo() {
        modelo.limpiar();
        posiciones.clear();
        sincronizar();
    }

    public void cargarEjemplo(AutomataFinitoDeterminista ejemplo) {
        modelo.cargarDesde(ejemplo);
        posiciones.clear();
        sincronizar();
    }

    public void completarConEstadoTrampa() {
        if (automataActual == null) {
            return;
        }
        modelo.cargarDesde(ValidadorAutomata.completarConEstadoTrampa(automataActual));
        sincronizar();
    }

    public boolean puedeCompletarConEstadoTrampa() {
        return validacionActual.contieneCodigo(ValidadorAutomata.CODIGO_TRANSICION_FALTANTE);
    }

    public void sincronizar() {
        automataActual = modelo.construirAutomata();
        validacionActual = ValidadorAutomata.validar(automataActual);

        if (lienzo != null) {
            lienzo.establecerAutomata(automataActual, posicionesPara(automataActual));
        }
        if (panelAlfabeto != null) {
            panelAlfabeto.refrescar();
        }
        if (panelEstados != null) {
            panelEstados.refrescar();
        }
        if (panelMatriz != null) {
            panelMatriz.refrescar();
        }
        if (panelValidacion != null) {
            panelValidacion.refrescar();
        }
    }

    private List<Punto> posicionesPara(AutomataFinitoDeterminista automata) {
        for (NodoGrafico nodo : lienzo.nodos()) {
            posiciones.put(nodo.nombre(), new Punto(nodo.x(), nodo.y()));
        }

        List<Estado> estados = List.copyOf(automata.estados());
        List<Punto> reserva = distribucionDeReserva(estados.size());

        List<Punto> resultado = new ArrayList<>(estados.size());
        for (int indice = 0; indice < estados.size(); indice++) {
            String nombre = estados.get(indice).nombre();
            Punto punto = posiciones.computeIfAbsent(nombre, ignorado -> reserva.get(resultado.size()));
            resultado.add(punto);
        }
        return resultado;
    }

    private List<Punto> distribucionDeReserva(int cantidad) {
        double ancho = lienzo.getWidth() > 0 ? lienzo.getWidth() : lienzo.getPreferredSize().width;
        double alto = lienzo.getHeight() > 0 ? lienzo.getHeight() : lienzo.getPreferredSize().height;
        return DistribuidorEstados.circular(cantidad, ancho / 2, alto / 2,
                Math.min(ancho, alto) * RADIO_DISTRIBUCION);
    }
}
