package com.unbosque.afd.core.logica;

import com.unbosque.afd.core.modelo.AutomataFinitoDeterminista;
import com.unbosque.afd.core.modelo.ErrorValidacion;
import com.unbosque.afd.core.modelo.Estado;
import com.unbosque.afd.core.modelo.MotivoRechazo;
import com.unbosque.afd.core.modelo.ResultadoValidacion;
import com.unbosque.afd.core.modelo.Severidad;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidadorAutomataTest {

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

    private static boolean tieneCodigo(ResultadoValidacion resultado, String codigo, Severidad severidad) {
        return resultado.hallazgos().stream()
                .anyMatch(hallazgo -> hallazgo.codigo().equals(codigo) && hallazgo.severidad() == severidad);
    }

    @Test
    @DisplayName("Una delta incompleta invalida el AFD y completarConEstadoTrampa lo vuelve válido")
    void completarConEstadoTrampaVuelveValidoElAutomata() {
        AutomataFinitoDeterminista original = deltaIncompleta();

        ResultadoValidacion antes = ValidadorAutomata.validar(original);
        assertFalse(antes.esValido());
        assertTrue(tieneCodigo(antes, ValidadorAutomata.CODIGO_TRANSICION_FALTANTE, Severidad.ERROR));
        assertEquals(1, antes.errores().size());

        AutomataFinitoDeterminista completado = ValidadorAutomata.completarConEstadoTrampa(original);
        ResultadoValidacion despues = ValidadorAutomata.validar(completado);

        assertTrue(despues.esValido(), () -> "Hallazgos inesperados: " + despues.hallazgos());
        assertTrue(despues.hallazgos().isEmpty());
        assertEquals(3, completado.estados().size());

        Estado trampa = completado.buscarEstado(ValidadorAutomata.NOMBRE_ESTADO_TRAMPA).orElseThrow();
        assertFalse(trampa.esAceptacion());
        assertFalse(trampa.esInicial());
        assertEquals(trampa, completado.transitar(trampa, '0').orElseThrow());
        assertEquals(trampa, completado.transitar(trampa, '1').orElseThrow());
        assertEquals(trampa, completado.transitar(completado.buscarEstado("q0").orElseThrow(), '1').orElseThrow());
    }

    @Test
    @DisplayName("completarConEstadoTrampa no muta el autómata original")
    void completarNoMutaElOriginal() {
        AutomataFinitoDeterminista original = deltaIncompleta();
        int transicionesAntes = original.funcionTransicion().cantidad();

        AutomataFinitoDeterminista completado = ValidadorAutomata.completarConEstadoTrampa(original);

        assertNotNull(completado);
        assertEquals(2, original.estados().size());
        assertEquals(transicionesAntes, original.funcionTransicion().cantidad());
        assertTrue(original.buscarEstado(ValidadorAutomata.NOMBRE_ESTADO_TRAMPA).isEmpty());
        assertFalse(ValidadorAutomata.validar(original).esValido());
        assertTrue(original.transitar(original.buscarEstado("q0").orElseThrow(), '1').isEmpty());

        assertEquals(MotivoRechazo.TRANSICION_NO_DEFINIDA, SimuladorAFD.simular(original, "1").motivoRechazo());
        assertEquals(MotivoRechazo.ESTADO_NO_ACEPTACION, SimuladorAFD.simular(completado, "1").motivoRechazo());
    }

    @Test
    @DisplayName("Un AFD ya total no recibe estado trampa")
    void automataTotalNoRecibeTrampa() {
        AutomataFinitoDeterminista completado =
                ValidadorAutomata.completarConEstadoTrampa(ConstructorEjemplos.cantidadParDeCeros());

        assertEquals(2, completado.estados().size());
        assertTrue(completado.buscarEstado(ValidadorAutomata.NOMBRE_ESTADO_TRAMPA).isEmpty());
        assertTrue(ValidadorAutomata.validar(completado).esValido());
    }

    @Test
    @DisplayName("Un estado inalcanzable se reporta como ADVERTENCIA sin invalidar el AFD")
    void estadoInalcanzableEsAdvertencia() {
        AutomataFinitoDeterminista automata = AutomataFinitoDeterminista.constructor()
                .nombre("Con estado aislado")
                .alfabeto("0", "1")
                .agregarEstado("q0", true, true)
                .agregarEstado("qAislado", false, false)
                .agregarTransicion("q0", '0', "q0")
                .agregarTransicion("q0", '1', "q0")
                .agregarTransicion("qAislado", '0', "qAislado")
                .agregarTransicion("qAislado", '1', "qAislado")
                .construir();

        ResultadoValidacion resultado = ValidadorAutomata.validar(automata);

        assertTrue(resultado.esValido());
        assertTrue(resultado.errores().isEmpty());
        assertEquals(1, resultado.advertencias().size());
        ErrorValidacion advertencia = resultado.advertencias().get(0);
        assertEquals(Severidad.ADVERTENCIA, advertencia.severidad());
        assertEquals(ValidadorAutomata.CODIGO_ESTADO_INALCANZABLE, advertencia.codigo());
        assertTrue(advertencia.mensaje().contains("qAislado"));
        assertFalse(ValidadorAutomata.alcanzables(automata).contains(automata.buscarEstado("qAislado").orElseThrow()));
    }

    @Test
    @DisplayName("Un estado de aceptación inalcanzable genera las dos advertencias")
    void estadoDeAceptacionInalcanzable() {
        AutomataFinitoDeterminista automata = AutomataFinitoDeterminista.constructor()
                .alfabeto("0", "1")
                .agregarEstado("q0", true, false)
                .agregarEstado("qFinal", false, true)
                .agregarTransicion("q0", '0', "q0")
                .agregarTransicion("q0", '1', "q0")
                .agregarTransicion("qFinal", '0', "qFinal")
                .agregarTransicion("qFinal", '1', "qFinal")
                .construir();

        ResultadoValidacion resultado = ValidadorAutomata.validar(automata);

        assertTrue(resultado.esValido());
        assertTrue(tieneCodigo(resultado, ValidadorAutomata.CODIGO_ESTADO_INALCANZABLE, Severidad.ADVERTENCIA));
        assertTrue(tieneCodigo(resultado, ValidadorAutomata.CODIGO_ACEPTACION_INALCANZABLE, Severidad.ADVERTENCIA));
    }

    @Test
    @DisplayName("Se exige exactamente un estado inicial")
    void estadoInicialUnico() {
        AutomataFinitoDeterminista sinInicial = AutomataFinitoDeterminista.constructor()
                .alfabeto("0")
                .agregarEstado("q0", false, true)
                .agregarTransicion("q0", '0', "q0")
                .construir();
        ResultadoValidacion resultadoSinInicial = ValidadorAutomata.validar(sinInicial);
        assertFalse(resultadoSinInicial.esValido());
        assertTrue(tieneCodigo(resultadoSinInicial, ValidadorAutomata.CODIGO_SIN_ESTADO_INICIAL, Severidad.ERROR));

        AutomataFinitoDeterminista dosIniciales = AutomataFinitoDeterminista.constructor()
                .alfabeto("0")
                .agregarEstado("q0", true, true)
                .agregarEstado("q1", true, false)
                .agregarTransicion("q0", '0', "q1")
                .agregarTransicion("q1", '0', "q0")
                .construir();
        ResultadoValidacion resultadoDosIniciales = ValidadorAutomata.validar(dosIniciales);
        assertFalse(resultadoDosIniciales.esValido());
        assertTrue(tieneCodigo(resultadoDosIniciales,
                ValidadorAutomata.CODIGO_VARIOS_ESTADOS_INICIALES, Severidad.ERROR));
    }

    @Test
    @DisplayName("Se detectan alfabeto vacío, no determinismo, destinos inexistentes y nombres inválidos")
    void erroresEstructurales() {
        AutomataFinitoDeterminista automata = AutomataFinitoDeterminista.constructor()
                .agregarEstado("q0", true, true)
                .agregarEstado("q0", false, false)
                .agregarEstado("", false, false)
                .agregarTransicion("q0", '0', "q0")
                .agregarTransicion("q0", '0', "qFantasma")
                .construir();

        ResultadoValidacion resultado = ValidadorAutomata.validar(automata);

        assertFalse(resultado.esValido());
        assertTrue(tieneCodigo(resultado, ValidadorAutomata.CODIGO_ALFABETO_VACIO, Severidad.ERROR));
        assertTrue(tieneCodigo(resultado, ValidadorAutomata.CODIGO_NO_DETERMINISTA, Severidad.ERROR));
        assertTrue(tieneCodigo(resultado, ValidadorAutomata.CODIGO_DESTINO_INEXISTENTE, Severidad.ERROR));
        assertTrue(tieneCodigo(resultado, ValidadorAutomata.CODIGO_NOMBRE_DUPLICADO, Severidad.ERROR));
        assertTrue(tieneCodigo(resultado, ValidadorAutomata.CODIGO_NOMBRE_VACIO, Severidad.ERROR));
    }
}
