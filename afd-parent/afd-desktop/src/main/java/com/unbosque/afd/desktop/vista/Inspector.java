package com.unbosque.afd.desktop.vista;

import com.unbosque.afd.core.modelo.ErrorValidacion;
import com.unbosque.afd.core.modelo.ResultadoValidacion;
import com.unbosque.afd.core.modelo.Transicion;
import com.unbosque.afd.desktop.componentes.BarraDesplazamiento;
import com.unbosque.afd.desktop.componentes.BotonAccion;
import com.unbosque.afd.desktop.componentes.CampoTexto;
import com.unbosque.afd.desktop.componentes.CasillaVerificacion;
import com.unbosque.afd.desktop.componentes.ChipSimbolo;
import com.unbosque.afd.desktop.componentes.PanelTarjeta;
import com.unbosque.afd.desktop.componentes.TarjetaHallazgo;
import com.unbosque.afd.desktop.controlador.ControladorAutomata;
import com.unbosque.afd.desktop.controlador.Seleccion;
import com.unbosque.afd.desktop.render.Medidas;
import com.unbosque.afd.desktop.render.Tema;
import com.unbosque.afd.desktop.render.TipografiaApp;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.List;
import java.util.Objects;

public class Inspector extends JPanel implements Tema.Sensible {

    public static final int PESTANA_CONTEXTO = 0;
    public static final int PESTANA_VALIDACION = 1;

    private static final int HOLGURA_TEXTO = 12;

    private final ControladorAutomata controlador;
    private final TirillaPestanas tirilla;
    private final JPanel tarjetas = new JPanel(new CardLayout());
    private final JPanel contexto = new JPanel();
    private final JPanel validacion = new JPanel();
    private final BotonAccion botonTrampa =
            new BotonAccion("Completar con estado trampa", BotonAccion.Estilo.SECUNDARIO);

    public Inspector(ControladorAutomata controlador) {
        this.controlador = Objects.requireNonNull(controlador, "El controlador no puede ser nulo");

        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(Medidas.ANCHO_INSPECTOR, 0));
        setMinimumSize(new Dimension(Medidas.ANCHO_INSPECTOR, 0));
        setOpaque(false);

        tirilla = new TirillaPestanas(List.of("CONTEXTO", "VALIDACIÓN"), this::mostrarPestana);

        contexto.setLayout(new BoxLayout(contexto, BoxLayout.Y_AXIS));
        contexto.setOpaque(false);
        contexto.setBorder(BorderFactory.createEmptyBorder(
                Medidas.paso(3), Medidas.paso(3), Medidas.paso(3), Medidas.paso(3)));

        validacion.setLayout(new BoxLayout(validacion, BoxLayout.Y_AXIS));
        validacion.setOpaque(false);
        validacion.setBorder(BorderFactory.createEmptyBorder(
                Medidas.paso(3), Medidas.paso(3), Medidas.paso(3), Medidas.paso(3)));

        tarjetas.setOpaque(false);
        tarjetas.add(envolver(contexto), "contexto");
        tarjetas.add(envolver(validacion), "validacion");

        JPanel tira = new JPanel(new BorderLayout());
        tira.setOpaque(false);
        tira.add(tirilla, BorderLayout.NORTH);

        add(tira, BorderLayout.WEST);
        add(tarjetas, BorderLayout.CENTER);

        botonTrampa.addActionListener(evento -> controlador.completarConEstadoTrampa());

        controlador.agregarObservador(this::refrescar);
        refrescar();
    }

    private static JScrollPane envolver(JComponent contenido) {
        return BarraDesplazamiento.envolver(contenido);
    }

    public void mostrarPestana(int indice) {
        ((CardLayout) tarjetas.getLayout()).show(tarjetas,
                indice == PESTANA_VALIDACION ? "validacion" : "contexto");
        tirilla.seleccionar(indice);
    }

    public void abrirValidacion() {
        mostrarPestana(PESTANA_VALIDACION);
    }

    @Override
    public void aplicarTema() {
        refrescar();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            Tema tema = Tema.actual();
            g2.setColor(tema.panelFondo());
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.setColor(tema.panelBorde());
            g2.drawLine(0, 0, 0, getHeight());
        } finally {
            g2.dispose();
        }
    }

    public void refrescar() {
        reconstruirContexto();
        reconstruirValidacion();
        revalidate();
        repaint();
    }

    private void reconstruirContexto() {
        contexto.removeAll();
        Seleccion seleccion = controlador.seleccion();
        switch (seleccion.tipo()) {
            case ESTADO -> construirPanelEstado(seleccion.estado());
            case ARISTA -> construirPanelArista(seleccion.origen(), seleccion.destino());
            default -> construirResumen();
        }
        contexto.add(Box.createVerticalGlue());
    }

    private void construirResumen() {
        PanelTarjeta tarjeta = new PanelTarjeta("La quíntupla");
        tarjeta.setLayout(new BoxLayout(tarjeta, BoxLayout.Y_AXIS));

        tarjeta.add(lineaMono("M = (Q, Σ, δ, q₀, F)", Tema.ACTIVO));
        tarjeta.add(Box.createVerticalStrut(Medidas.paso(2)));
        tarjeta.add(lineaDato("Q", plural(controlador.modelo().estados().size(), "estado", "estados")));
        tarjeta.add(lineaDato("Σ", plural(controlador.modelo().simbolos().size(), "símbolo", "símbolos")));
        tarjeta.add(lineaDato("δ", plural(contarTransiciones(),
                "transición definida", "transiciones definidas")));
        tarjeta.add(lineaDato("q₀", controlador.modelo().estadoInicial() == null
                ? "sin definir" : controlador.modelo().estadoInicial()));
        tarjeta.add(lineaDato("F", plural(contarAceptacion(),
                "estado de aceptación", "estados de aceptación")));
        agregar(tarjeta);

        PanelTarjeta alfabeto = new PanelTarjeta("Alfabeto Σ");
        alfabeto.setLayout(new BoxLayout(alfabeto, BoxLayout.Y_AXIS));

        JPanel chips = new JPanel(new FlowLayout(FlowLayout.LEFT, Medidas.paso(2), Medidas.PASO));
        chips.setOpaque(false);
        chips.setAlignmentX(LEFT_ALIGNMENT);
        if (controlador.modelo().simbolos().isEmpty()) {
            chips.add(textoTenue("Σ está vacío"));
        } else {
            for (char simbolo : controlador.modelo().simbolos()) {
                chips.add(new ChipSimbolo(simbolo, controlador::quitarSimbolo));
            }
        }
        alfabeto.add(chips);
        alfabeto.add(Box.createVerticalStrut(Medidas.paso(1)));

        CampoTexto campo = new CampoTexto("símbolo", 4);
        campo.setMaximumSize(new Dimension(104, 32));
        BotonAccion agregar = new BotonAccion("Agregar", BotonAccion.Estilo.SECUNDARIO);
        Runnable accion = () -> {
            if (controlador.agregarSimbolo(campo.getText())) {
                campo.setText("");
            } else {
                java.awt.Toolkit.getDefaultToolkit().beep();
            }
        };
        campo.addActionListener(evento -> accion.run());
        agregar.addActionListener(evento -> accion.run());

        JPanel entrada = new JPanel();
        entrada.setOpaque(false);
        entrada.setLayout(new BoxLayout(entrada, BoxLayout.X_AXIS));
        entrada.setAlignmentX(LEFT_ALIGNMENT);
        entrada.add(campo);
        entrada.add(Box.createHorizontalStrut(Medidas.paso(2)));
        entrada.add(agregar);
        entrada.add(Box.createHorizontalGlue());
        alfabeto.add(entrada);
        agregar(alfabeto);

        PanelTarjeta ayuda = new PanelTarjeta("Cómo dibujar");
        ayuda.setLayout(new BoxLayout(ayuda, BoxLayout.Y_AXIS));
        ayuda.add(textoTenue("E   inserta un estado"));
        ayuda.add(textoTenue("A   conmuta la aceptación"));
        ayuda.add(textoTenue("I    marca el inicial"));
        ayuda.add(textoTenue("T   arrastra una transición"));
        ayuda.add(textoTenue("D   borra bajo el cursor"));
        agregar(ayuda);
    }

    private void construirPanelEstado(String estado) {
        PanelTarjeta tarjeta = new PanelTarjeta("Estado seleccionado");
        tarjeta.setLayout(new BoxLayout(tarjeta, BoxLayout.Y_AXIS));

        CampoTexto nombre = new CampoTexto("nombre", 10);
        nombre.setText(estado);
        nombre.setAlignmentX(LEFT_ALIGNMENT);
        nombre.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        nombre.addActionListener(evento -> {
            if (!controlador.renombrarEstado(estado, nombre.getText())) {
                nombre.setText(estado);
                java.awt.Toolkit.getDefaultToolkit().beep();
            }
        });
        tarjeta.add(nombre);
        tarjeta.add(Box.createVerticalStrut(Medidas.paso(2)));

        CasillaVerificacion aceptacion = new CasillaVerificacion("Es de aceptación",
                controlador.modelo().esAceptacion(estado),
                marcada -> controlador.establecerAceptacion(estado, marcada));
        aceptacion.establecerAcento(Tema.ACEPTACION);
        aceptacion.setAlignmentX(LEFT_ALIGNMENT);
        tarjeta.add(aceptacion);
        tarjeta.add(Box.createVerticalStrut(Medidas.paso(2)));

        BotonAccion inicial = new BotonAccion(
                controlador.modelo().esInicial(estado) ? "Ya es el estado inicial" : "Marcar como inicial",
                BotonAccion.Estilo.SECUNDARIO);
        inicial.setEnabled(!controlador.modelo().esInicial(estado));
        inicial.setAlignmentX(LEFT_ALIGNMENT);
        inicial.addActionListener(evento -> controlador.marcarInicial(estado));
        tarjeta.add(inicial);
        agregar(tarjeta);

        PanelTarjeta salientes = new PanelTarjeta("Transiciones salientes");
        salientes.setLayout(new BoxLayout(salientes, BoxLayout.Y_AXIS));
        agregarTransiciones(salientes, controlador.modelo().salientesDe(estado));
        agregar(salientes);

        PanelTarjeta entrantes = new PanelTarjeta("Transiciones entrantes");
        entrantes.setLayout(new BoxLayout(entrantes, BoxLayout.Y_AXIS));
        agregarTransiciones(entrantes, controlador.modelo().entrantesA(estado));
        agregar(entrantes);

        BotonAccion eliminar = new BotonAccion("Eliminar el estado", BotonAccion.Estilo.SECUNDARIO);
        eliminar.setAlignmentX(LEFT_ALIGNMENT);
        eliminar.addActionListener(evento -> controlador.eliminarEstado(estado));
        contexto.add(eliminar);
        contexto.add(Box.createVerticalStrut(Medidas.paso(2)));
    }

    private void agregarTransiciones(JComponent destino, List<Transicion> transiciones) {
        if (transiciones.isEmpty()) {
            destino.add(textoTenue("Ninguna"));
            return;
        }
        for (Transicion transicion : transiciones) {
            JLabel linea = new JLabel(transicion.estadoOrigen().nombre()
                    + " --" + transicion.simbolo() + "--> " + transicion.estadoDestino().nombre());
            linea.setFont(TipografiaApp.MONO);
            linea.setForeground(Tema.actual().textoPrimario());
            linea.setAlignmentX(LEFT_ALIGNMENT);
            destino.add(linea);
        }
    }

    private void construirPanelArista(String origen, String destino) {
        PanelTarjeta tarjeta = new PanelTarjeta("Transición seleccionada");
        tarjeta.setLayout(new BoxLayout(tarjeta, BoxLayout.Y_AXIS));

        tarjeta.add(lineaMono(origen + "  →  " + destino, Tema.ACTIVO));
        tarjeta.add(Box.createVerticalStrut(Medidas.paso(1)));
        tarjeta.add(lineaDato("Origen", origen));
        tarjeta.add(lineaDato("Destino", destino));
        agregar(tarjeta);

        PanelTarjeta simbolos = new PanelTarjeta("Símbolos que la disparan");
        simbolos.setLayout(new BoxLayout(simbolos, BoxLayout.Y_AXIS));

        if (controlador.modelo().simbolos().isEmpty()) {
            simbolos.add(textoTenue("Σ está vacío"));
        }
        for (char simbolo : controlador.modelo().simbolos()) {
            String actual = controlador.modelo().destino(origen, simbolo);
            boolean marcado = destino.equals(actual);
            boolean ocupado = actual != null && !marcado;

            CasillaVerificacion casilla = new CasillaVerificacion(
                    "δ(" + origen + ", " + simbolo + ")" + (ocupado ? "  ya va a " + actual : ""),
                    marcado,
                    marcada -> alternarSimbolo(origen, destino, simbolo, marcada));
            casilla.setAlignmentX(LEFT_ALIGNMENT);
            if (ocupado) {
                casilla.establecerAcento(Tema.ADVERTENCIA);
                casilla.setToolTipText("Marcarlo reemplazaría el destino actual " + actual);
            }
            simbolos.add(casilla);
        }
        agregar(simbolos);

        BotonAccion eliminar = new BotonAccion("Eliminar la transición", BotonAccion.Estilo.SECUNDARIO);
        eliminar.setAlignmentX(LEFT_ALIGNMENT);
        eliminar.addActionListener(evento -> controlador.eliminarArista(origen, destino));
        contexto.add(eliminar);
        contexto.add(Box.createVerticalStrut(Medidas.paso(2)));
    }

    private void alternarSimbolo(String origen, String destino, char simbolo, boolean marcada) {
        if (marcada) {
            controlador.establecerTransicion(origen, simbolo, destino);
        } else if (destino.equals(controlador.modelo().destino(origen, simbolo))) {
            controlador.establecerTransicion(origen, simbolo, null);
        }
    }

    private void reconstruirValidacion() {
        validacion.removeAll();
        ResultadoValidacion resultado = controlador.validacionActual();

        PanelTarjeta resumen = new PanelTarjeta("Estado del autómata");
        resumen.setLayout(new BoxLayout(resumen, BoxLayout.Y_AXIS));
        int errores = resultado.errores().size();
        int advertencias = resultado.advertencias().size();

        if (controlador.modelo().estados().isEmpty()) {
            resumen.add(lineaMono("Lienzo vacío", Tema.actual().textoSecundario()));
            resumen.add(textoTenue("Inserta estados con la herramienta E"));
        } else if (errores == 0 && advertencias == 0) {
            resumen.add(lineaMono("AFD válido", Tema.ACEPTADA));
            resumen.add(textoTenue("La quíntupla está completa y δ es total"));
        } else {
            resumen.add(lineaMono(errores + " error(es), " + advertencias + " advertencia(s)",
                    errores > 0 ? Tema.RECHAZADA : Tema.ADVERTENCIA));
            resumen.add(textoTenue("Cada tarjeta selecciona el elemento afectado"));
        }
        agregar(validacion, resumen);

        for (ErrorValidacion hallazgo : resultado.errores()) {
            agregar(validacion, tarjetaDe(hallazgo));
        }
        for (ErrorValidacion hallazgo : resultado.advertencias()) {
            agregar(validacion, tarjetaDe(hallazgo));
        }

        botonTrampa.setEnabled(controlador.puedeCompletarConEstadoTrampa());
        botonTrampa.setAlignmentX(LEFT_ALIGNMENT);
        botonTrampa.setToolTipText(controlador.puedeCompletarConEstadoTrampa()
                ? "Cierra las transiciones faltantes con un estado absorbente"
                : "Se habilita cuando δ no es total");
        validacion.add(botonTrampa);
        validacion.add(Box.createVerticalGlue());
    }

    private TarjetaHallazgo tarjetaDe(ErrorValidacion hallazgo) {
        String culpable = controlador.estadoCulpable(hallazgo).orElse(null);
        return new TarjetaHallazgo(hallazgo, culpable != null,
                culpable == null ? null : () -> controlador.seleccionar(Seleccion.deEstado(culpable)));
    }

    private static String plural(int cantidad, String singular, String plural) {
        return cantidad + " " + (cantidad == 1 ? singular : plural);
    }

    private int contarTransiciones() {
        int total = 0;
        for (String estado : controlador.modelo().estados()) {
            total += controlador.modelo().salientesDe(estado).size();
        }
        return total;
    }

    private int contarAceptacion() {
        int total = 0;
        for (String estado : controlador.modelo().estados()) {
            if (controlador.modelo().esAceptacion(estado)) {
                total++;
            }
        }
        return total;
    }

    private void agregar(JComponent tarjeta) {
        agregar(contexto, tarjeta);
    }

    private static void agregar(JPanel destino, JComponent tarjeta) {
        tarjeta.setAlignmentX(LEFT_ALIGNMENT);
        tarjeta.setMaximumSize(new Dimension(Integer.MAX_VALUE, tarjeta.getPreferredSize().height));
        destino.add(tarjeta);
        destino.add(Box.createVerticalStrut(Medidas.paso(2)));
    }

    private static JLabel lineaMono(String texto, Color color) {
        JLabel etiqueta = new JLabel(texto);
        etiqueta.setFont(TipografiaApp.MONO_FUERTE);
        etiqueta.setForeground(color);
        etiqueta.setAlignmentX(LEFT_ALIGNMENT);
        return etiqueta;
    }

    private static Component lineaDato(String clave, String valor) {
        JPanel fila = new JPanel();
        fila.setOpaque(false);
        fila.setLayout(new BoxLayout(fila, BoxLayout.X_AXIS));
        fila.setAlignmentX(LEFT_ALIGNMENT);
        fila.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));

        JLabel etiquetaClave = new JLabel(clave);
        etiquetaClave.setFont(TipografiaApp.MONO_FUERTE);
        etiquetaClave.setForeground(Tema.actual().textoSecundario());
        etiquetaClave.setPreferredSize(new Dimension(28, 18));

        JLabel etiquetaValor = new JLabel(valor);
        etiquetaValor.setFont(TipografiaApp.CUERPO);
        etiquetaValor.setForeground(Tema.actual().textoPrimario());

        fila.add(etiquetaClave);
        fila.add(conHolgura(etiquetaValor));
        fila.add(Box.createHorizontalGlue());
        return fila;
    }

    private static JLabel textoTenue(String texto) {
        JLabel etiqueta = new JLabel(texto);
        etiqueta.setFont(TipografiaApp.ETIQUETA);
        etiqueta.setForeground(Tema.actual().textoSecundario());
        etiqueta.setAlignmentX(LEFT_ALIGNMENT);
        return conHolgura(etiqueta);
    }

    private static JLabel conHolgura(JLabel etiqueta) {
        etiqueta.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, HOLGURA_TEXTO));
        return etiqueta;
    }
}
