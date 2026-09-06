package com.unbosque.afd.core.logica;

import com.unbosque.afd.core.modelo.Alfabeto;
import com.unbosque.afd.core.modelo.AutomataFinitoDeterminista;

import java.util.List;

public final class ConstructorEjemplos {

    private ConstructorEjemplos() {
    }

    public static Alfabeto alfabetoBinario() {
        return Alfabeto.de("0", "1");
    }

    public static AutomataFinitoDeterminista cantidadParDeCeros() {
        return AutomataFinitoDeterminista.constructor()
                .nombre("Cantidad par de ceros")
                .alfabeto(alfabetoBinario())
                .agregarEstado("qPar", true, true)
                .agregarEstado("qImpar", false, false)
                .agregarTransicion("qPar", '0', "qImpar")
                .agregarTransicion("qPar", '1', "qPar")
                .agregarTransicion("qImpar", '0', "qPar")
                .agregarTransicion("qImpar", '1', "qImpar")
                .construir();
    }

    public static AutomataFinitoDeterminista contieneSubcadena00() {
        return AutomataFinitoDeterminista.constructor()
                .nombre("Contiene la subcadena 00")
                .alfabeto(alfabetoBinario())
                .agregarEstado("qSinCero", true, false)
                .agregarEstado("qUnCero", false, false)
                .agregarEstado("qEncontrado", false, true)
                .agregarTransicion("qSinCero", '0', "qUnCero")
                .agregarTransicion("qSinCero", '1', "qSinCero")
                .agregarTransicion("qUnCero", '0', "qEncontrado")
                .agregarTransicion("qUnCero", '1', "qSinCero")
                .agregarTransicion("qEncontrado", '0', "qEncontrado")
                .agregarTransicion("qEncontrado", '1', "qEncontrado")
                .construir();
    }

    public static AutomataFinitoDeterminista cantidadParDeUnos() {
        return AutomataFinitoDeterminista.constructor()
                .nombre("Cantidad par de unos")
                .alfabeto(alfabetoBinario())
                .agregarEstado("pPar", true, true)
                .agregarEstado("pImpar", false, false)
                .agregarTransicion("pPar", '0', "pPar")
                .agregarTransicion("pPar", '1', "pImpar")
                .agregarTransicion("pImpar", '0', "pImpar")
                .agregarTransicion("pImpar", '1', "pPar")
                .construir();
    }

    public static AutomataFinitoDeterminista terminaEnCero() {
        return AutomataFinitoDeterminista.constructor()
                .nombre("Termina en cero")
                .alfabeto(alfabetoBinario())
                .agregarEstado("qNoTerminaEnCero", true, false)
                .agregarEstado("qTerminaEnCero", false, true)
                .agregarTransicion("qNoTerminaEnCero", '0', "qTerminaEnCero")
                .agregarTransicion("qNoTerminaEnCero", '1', "qNoTerminaEnCero")
                .agregarTransicion("qTerminaEnCero", '0', "qTerminaEnCero")
                .agregarTransicion("qTerminaEnCero", '1', "qNoTerminaEnCero")
                .construir();
    }

    public static List<AutomataFinitoDeterminista> todos() {
        return List.of(cantidadParDeCeros(), contieneSubcadena00(), cantidadParDeUnos(), terminaEnCero());
    }
}
