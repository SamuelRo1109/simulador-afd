package com.unbosque.afd.core.logica;

import com.unbosque.afd.core.modelo.AutomataFinitoDeterminista;
import com.unbosque.afd.core.modelo.MotivoRechazo;
import com.unbosque.afd.core.modelo.PasoEjecucion;
import com.unbosque.afd.core.modelo.ResultadoSimulacion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimuladorAFDTest {

    private static AutomataFinitoDeterminista deltaIncompleta() {
        return AutomataFinitoDeterminista.constructor()
                .nombre("Delta incompleta")
                .alfabeto("0", "1")
                .agregarEstado("q0", true, true)
                .agregarEstado("q1", false, false)
                .agregarTransicion("q0", '0', "q1")
                .agregarTransicion("q1", '0', "q1")
                .agregarTransicion("q1", '1', "q0")
                .construir();
    }

    @ParameterizedTest
    @DisplayName("La traza tiene exactamente cadena.length() pasos cuando la simulacion no falla")
    @ValueSource(strings = {"", "0", "01", "1010", "000111", "1001001011"})
    void trazaTieneUnPasoPorSimbolo(String cadena) {
        for (AutomataFinitoDeterminista automata : ConstructorEjemplos.todos()) {
            ResultadoSimulacion resultado = SimuladorAFD.simular(automata, cadena);

            assertEquals(cadena.length(), resultado.pasos().size(),
                    () -> "Traza incorrecta para " + automata.nombre() + " con \"" + cadena + "\"");
            assertTrue(resultado.pasos().stream().noneMatch(PasoEjecucion::fallido),
                    () -> "Ningun paso deberia fallar en " + automata.nombre());
        }
    }

    @Test
    @DisplayName("La traza es coherente: indices, encadenamiento de estados y particion de la cadena")
    void trazaCoherente() {
        AutomataFinitoDeterminista automata = ConstructorEjemplos.terminaEnCero();
        String cadena = "10110";

        ResultadoSimulacion resultado = SimuladorAFD.simular(automata, cadena);

        assertEquals(5, resultado.pasos().size());
        for (int indice = 0; indice < resultado.pasos().size(); indice++) {
            PasoEjecucion paso = resultado.pasos().get(indice);
            assertEquals(indice, paso.indice());
            assertEquals(cadena.charAt(indice), paso.simbolo());
            assertEquals(cadena.substring(0, indice + 1), paso.cadenaLeida());
            assertEquals(cadena.substring(indice + 1), paso.cadenaRestante());
            assertEquals(cadena, paso.cadenaLeida() + paso.cadenaRestante());
            assertEquals(indice == cadena.length() - 1, paso.esUltimo());
            if (indice > 0) {
                assertEquals(resultado.pasos().get(indice - 1).estadoDestino(), paso.estadoOrigen());
            }
        }
        assertEquals(automata.estadoInicial(), resultado.pasos().get(0).estadoOrigen());
        assertEquals(resultado.pasos().get(4).estadoDestino(), resultado.estadoFinal());
        assertTrue(resultado.aceptada());
    }

    @Test
    @DisplayName("La cadena vacia no genera pasos y se decide en el estado inicial")
    void cadenaVaciaLambda() {
        ResultadoSimulacion aceptada = SimuladorAFD.simular(ConstructorEjemplos.cantidadParDeCeros(), "");
        assertTrue(aceptada.aceptada());
        assertTrue(aceptada.pasos().isEmpty());
        assertEquals("qPar", aceptada.estadoFinal().nombre());
        assertEquals(MotivoRechazo.NINGUNO, aceptada.motivoRechazo());

        ResultadoSimulacion rechazada = SimuladorAFD.simular(ConstructorEjemplos.terminaEnCero(), "");
        assertFalse(rechazada.aceptada());
        assertTrue(rechazada.pasos().isEmpty());
        assertEquals(MotivoRechazo.ESTADO_NO_ACEPTACION, rechazada.motivoRechazo());
    }

    @Test
    @DisplayName("Un simbolo fuera del alfabeto corta la simulacion conservando los pasos recorridos")
    void simboloFueraDelAlfabeto() {
        ResultadoSimulacion resultado = SimuladorAFD.simular(ConstructorEjemplos.terminaEnCero(), "10a0");

        assertFalse(resultado.aceptada());
        assertEquals(MotivoRechazo.SIMBOLO_FUERA_ALFABETO, resultado.motivoRechazo());
        assertEquals(3, resultado.pasos().size());

        PasoEjecucion ultimo = resultado.pasos().get(2);
        assertEquals('a', ultimo.simbolo());
        assertNull(ultimo.estadoDestino());
        assertTrue(ultimo.fallido());
        assertTrue(ultimo.esUltimo());
        assertEquals("0", ultimo.cadenaRestante());
        assertSame(resultado.estadoFinal(), ultimo.estadoOrigen());
    }

    @Test
    @DisplayName("Una transicion no definida corta la simulacion conservando los pasos recorridos")
    void transicionNoDefinida() {
        ResultadoSimulacion resultado = SimuladorAFD.simular(deltaIncompleta(), "011");

        assertFalse(resultado.aceptada());
        assertEquals(MotivoRechazo.TRANSICION_NO_DEFINIDA, resultado.motivoRechazo());
        assertEquals(3, resultado.pasos().size());
        assertTrue(resultado.pasos().get(2).fallido());

        ResultadoSimulacion fallaDeInmediato = SimuladorAFD.simular(deltaIncompleta(), "1");
        assertEquals(MotivoRechazo.TRANSICION_NO_DEFINIDA, fallaDeInmediato.motivoRechazo());
        assertEquals(1, fallaDeInmediato.pasos().size());
        assertNull(fallaDeInmediato.pasos().get(0).estadoDestino());
    }

    @Test
    @DisplayName("La traza devuelta es inmutable y no admite argumentos nulos")
    void trazaInmutableYArgumentosObligatorios() {
        ResultadoSimulacion resultado = SimuladorAFD.simular(ConstructorEjemplos.cantidadParDeUnos(), "101");

        assertThrows(UnsupportedOperationException.class, () -> resultado.pasos().clear());
        assertThrows(NullPointerException.class,
                () -> SimuladorAFD.simular(ConstructorEjemplos.cantidadParDeUnos(), null));
        assertThrows(NullPointerException.class, () -> SimuladorAFD.simular(null, "101"));
    }

    @Test
    @DisplayName("El atajo acepta() coincide con el resultado completo")
    void atajoAcepta() {
        AutomataFinitoDeterminista automata = ConstructorEjemplos.contieneSubcadena00();
        assertTrue(SimuladorAFD.acepta(automata, "1001"));
        assertFalse(SimuladorAFD.acepta(automata, "0101"));
    }
}
