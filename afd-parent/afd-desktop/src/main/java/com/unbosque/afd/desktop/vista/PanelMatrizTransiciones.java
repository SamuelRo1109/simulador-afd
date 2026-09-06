package com.unbosque.afd.desktop.vista;

import com.unbosque.afd.desktop.componentes.BarraDesplazamiento;
import com.unbosque.afd.desktop.controlador.ControladorAutomata;
import com.unbosque.afd.desktop.controlador.ControladorSimulacion;
import com.unbosque.afd.desktop.render.Medidas;
import com.unbosque.afd.desktop.render.Tema;
import com.unbosque.afd.desktop.render.TipografiaApp;

import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PanelMatrizTransiciones extends JPanel implements Tema.Sensible {

    public static final String SIN_DEFINIR = "—";

    private static final int ANCHO_COLUMNA = 84;
    private static final int ANCHO_ENCABEZADO = 120;
    private static final int ALTO_FILA = 28;

    private final ControladorAutomata controlador;
    private final ModeloTablaMatriz modeloTabla;
    private final JTable tabla;
    private final JLabel resumen = new JLabel(" ");
    private final JScrollPane desplazable;

    private ControladorSimulacion simulacion;
    private String firmaEstructura = "";

    public PanelMatrizTransiciones(ControladorAutomata controlador) {
        this.controlador = Objects.requireNonNull(controlador, "El controlador no puede ser nulo");
        this.modeloTabla = new ModeloTablaMatriz();
        this.tabla = new JTable(modeloTabla);

        setLayout(new BorderLayout(0, Medidas.paso(1)));
        setOpaque(false);

        configurarTabla();
        desplazable = new JScrollPane(tabla);
        desplazable.setBorder(null);
        desplazable.setOpaque(false);
        desplazable.getViewport().setOpaque(false);
        BarraDesplazamiento.aplicar(desplazable);

        resumen.setFont(TipografiaApp.ETIQUETA);
        resumen.setBorder(BorderFactory.createEmptyBorder(0, Medidas.paso(1), 0, 0));

        add(desplazable, BorderLayout.CENTER);
        add(resumen, BorderLayout.SOUTH);

        aplicarTema();
        controlador.agregarObservador(this::refrescar);
        refrescar();
    }

    public void establecerSimulacion(ControladorSimulacion nuevaSimulacion) {
        this.simulacion = Objects.requireNonNull(nuevaSimulacion, "La simulación no puede ser nula");
        simulacion.agregarObservador(tabla::repaint);
    }

    @Override
    public void aplicarTema() {
        Tema tema = Tema.actual();
        tabla.setBackground(tema.panelElevado());
        tabla.setForeground(tema.textoPrimario());
        tabla.setGridColor(tema.panelBorde());
        tabla.setSelectionBackground(Tema.mezclar(Tema.ACTIVO, tema.panelElevado(), 0.20));
        tabla.setSelectionForeground(tema.textoPrimario());
        tabla.getTableHeader().setBackground(tema.panelFondo());
        tabla.getTableHeader().setForeground(tema.textoSecundario());
        resumen.setForeground(tema.textoSecundario());
        repaint();
    }

    public void refrescar() {
        if (tabla.isEditing()) {
            tabla.removeEditor();
        }
        String firma = firmaActual();
        if (firma.equals(firmaEstructura)) {
            modeloTabla.fireTableDataChanged();
        } else {
            firmaEstructura = firma;
            modeloTabla.fireTableStructureChanged();
            configurarColumnas();
        }
        resumen.setText(describirCobertura());
    }

    private void configurarTabla() {
        tabla.setRowHeight(ALTO_FILA);
        tabla.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        tabla.setFont(TipografiaApp.MONO);
        tabla.setCellSelectionEnabled(true);
        tabla.setShowGrid(true);
        tabla.setIntercellSpacing(new Dimension(1, 1));
        tabla.setDefaultRenderer(Object.class, new RenderizadorCelda());
        tabla.setFillsViewportHeight(true);

        JTableHeader encabezado = tabla.getTableHeader();
        encabezado.setReorderingAllowed(false);
        encabezado.setResizingAllowed(false);
        encabezado.setDefaultRenderer(new RenderizadorEncabezado());
        encabezado.setPreferredSize(new Dimension(0, ALTO_FILA));
    }

    private String firmaActual() {
        return controlador.modelo().estados() + " / " + controlador.modelo().simbolos();
    }

    private void configurarColumnas() {
        List<String> opciones = new ArrayList<>();
        opciones.add(SIN_DEFINIR);
        opciones.addAll(controlador.modelo().estados());

        for (int indice = 0; indice < tabla.getColumnCount(); indice++) {
            TableColumn columna = tabla.getColumnModel().getColumn(indice);
            columna.setPreferredWidth(indice == 0 ? ANCHO_ENCABEZADO : ANCHO_COLUMNA);
            columna.setMinWidth(indice == 0 ? 90 : 64);
            if (indice > 0) {
                columna.setCellEditor(new DefaultCellEditor(crearSelector(opciones)));
            }
        }
    }

    private JComboBox<String> crearSelector(List<String> opciones) {
        JComboBox<String> selector = new JComboBox<>(opciones.toArray(new String[0]));
        selector.setUI(new InterfazSelector());
        selector.setFont(TipografiaApp.MONO);
        selector.setBorder(BorderFactory.createEmptyBorder(0, Medidas.paso(2), 0, 0));
        selector.setRenderer(new RenderizadorOpcion());
        selector.setBackground(Tema.actual().panelFondo());
        selector.setForeground(Tema.actual().textoPrimario());
        return selector;
    }

    private String describirCobertura() {
        int total = controlador.modelo().estados().size() * controlador.modelo().simbolos().size();
        if (total == 0) {
            return "Define Σ y Q para construir δ";
        }
        int definidas = 0;
        for (String estado : controlador.modelo().estados()) {
            for (char simbolo : controlador.modelo().simbolos()) {
                if (controlador.modelo().destino(estado, simbolo) != null) {
                    definidas++;
                }
            }
        }
        return definidas + " de " + total + " transiciones definidas";
    }

    public boolean esCeldaEnCurso(int fila, int columna) {
        if (simulacion == null || !simulacion.hayResultado() || columna <= 0) {
            return false;
        }
        var paso = simulacion.pasoActual();
        if (paso == null) {
            return false;
        }
        List<String> estados = controlador.modelo().estados();
        List<Character> simbolos = controlador.modelo().simbolos();
        if (fila >= estados.size() || columna - 1 >= simbolos.size()) {
            return false;
        }
        return estados.get(fila).equals(paso.estadoOrigen().nombre())
                && simbolos.get(columna - 1) == paso.simbolo();
    }

    private final class ModeloTablaMatriz extends AbstractTableModel {

        @Override
        public int getRowCount() {
            return controlador.modelo().estados().size();
        }

        @Override
        public int getColumnCount() {
            return controlador.modelo().simbolos().size() + 1;
        }

        @Override
        public String getColumnName(int columna) {
            return columna == 0 ? "δ" : String.valueOf(controlador.modelo().simbolos().get(columna - 1));
        }

        @Override
        public boolean isCellEditable(int fila, int columna) {
            return columna > 0;
        }

        @Override
        public Object getValueAt(int fila, int columna) {
            List<String> estados = controlador.modelo().estados();
            if (fila >= estados.size()) {
                return null;
            }
            String estado = estados.get(fila);
            if (columna == 0) {
                return etiquetaEstado(estado);
            }
            List<Character> simbolos = controlador.modelo().simbolos();
            if (columna - 1 >= simbolos.size()) {
                return null;
            }
            String destino = controlador.modelo().destino(estado, simbolos.get(columna - 1));
            return destino == null ? SIN_DEFINIR : destino;
        }

        @Override
        public void setValueAt(Object valor, int fila, int columna) {
            List<String> estados = controlador.modelo().estados();
            List<Character> simbolos = controlador.modelo().simbolos();
            if (columna <= 0 || fila >= estados.size() || columna - 1 >= simbolos.size()) {
                return;
            }
            String destino = SIN_DEFINIR.equals(valor) || valor == null ? null : String.valueOf(valor);
            controlador.establecerTransicion(estados.get(fila), simbolos.get(columna - 1), destino);
        }

        private String etiquetaEstado(String estado) {
            String marca = controlador.modelo().esInicial(estado) ? "→ " : "";
            String aceptacion = controlador.modelo().esAceptacion(estado) ? " *" : "";
            return marca + estado + aceptacion;
        }
    }

    private final class RenderizadorCelda extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable tablaOrigen, Object valor, boolean seleccionada,
                                                       boolean enfocada, int fila, int columna) {
            super.getTableCellRendererComponent(tablaOrigen, valor, seleccionada, enfocada, fila, columna);
            Tema tema = Tema.actual();
            boolean sinDefinir = columna > 0 && SIN_DEFINIR.equals(valor);
            boolean enCurso = esCeldaEnCurso(fila, columna);

            setHorizontalAlignment(columna == 0 ? SwingConstants.LEFT : SwingConstants.CENTER);
            setFont(columna == 0 ? TipografiaApp.MONO_FUERTE : TipografiaApp.MONO);
            setBorder(BorderFactory.createEmptyBorder(0, Medidas.paso(2), 0, Medidas.paso(2)));

            if (enCurso) {
                setBackground(Tema.mezclar(Tema.ACTIVO, tema.panelElevado(), 0.85));
                setForeground(Tema.TINTA_SOBRE_ACENTO);
            } else if (columna == 0) {
                setBackground(tema.panelFondo());
                setForeground(tema.textoPrimario());
            } else if (sinDefinir) {
                setBackground(tema.celdaSinDefinir());
                setForeground(Tema.RECHAZADA);
            } else {
                setBackground(seleccionada ? tablaOrigen.getSelectionBackground() : tema.panelElevado());
                setForeground(tema.textoPrimario());
            }

            setToolTipText(sinDefinir ? "Transición sin definir: δ no es total" : null);
            return this;
        }
    }

    private static final class RenderizadorEncabezado extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable tablaOrigen, Object valor, boolean seleccionada,
                                                       boolean enfocada, int fila, int columna) {
            super.getTableCellRendererComponent(tablaOrigen, valor, seleccionada, enfocada, fila, columna);
            Tema tema = Tema.actual();
            setHorizontalAlignment(columna == 0 ? SwingConstants.LEFT : SwingConstants.CENTER);
            setFont(TipografiaApp.MONO_FUERTE);
            setForeground(columna == 0 ? Tema.ACTIVO : tema.textoSecundario());
            setBackground(tema.panelFondo());
            setOpaque(true);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, tema.panelBorde()),
                    BorderFactory.createEmptyBorder(0, Medidas.paso(2), 0, Medidas.paso(2))));
            return this;
        }
    }

    private static final class RenderizadorOpcion extends JLabel implements ListCellRenderer<Object> {

        @Override
        public Component getListCellRendererComponent(JList<?> lista, Object valor, int indice,
                                                      boolean seleccionado, boolean enfocado) {
            Tema tema = Tema.actual();
            setText(String.valueOf(valor));
            setOpaque(true);
            setFont(TipografiaApp.MONO);
            setHorizontalAlignment(SwingConstants.CENTER);
            setBorder(BorderFactory.createEmptyBorder(Medidas.PASO, Medidas.paso(2),
                    Medidas.PASO, Medidas.paso(2)));
            setBackground(seleccionado
                    ? Tema.mezclar(Tema.ACTIVO, tema.panelElevado(), 0.25)
                    : tema.panelElevado());
            setForeground(SIN_DEFINIR.equals(valor) ? Tema.RECHAZADA : tema.textoPrimario());
            return this;
        }
    }

    private static final class InterfazSelector extends BasicComboBoxUI {

        @Override
        protected JButton createArrowButton() {
            JButton flecha = new JButton() {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    try {
                        Medidas.calidad(g2);
                        g2.setColor(Tema.actual().textoSecundario());
                        Path2D.Double chevron = new Path2D.Double();
                        double centroX = getWidth() / 2.0;
                        double centroY = getHeight() / 2.0;
                        chevron.moveTo(centroX - 4, centroY - 2);
                        chevron.lineTo(centroX, centroY + 2.5);
                        chevron.lineTo(centroX + 4, centroY - 2);
                        g2.setStroke(new java.awt.BasicStroke(1.6f,
                                java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND));
                        g2.draw(chevron);
                    } finally {
                        g2.dispose();
                    }
                }
            };
            flecha.setBorder(null);
            flecha.setContentAreaFilled(false);
            flecha.setFocusable(false);
            flecha.setPreferredSize(new Dimension(18, 18));
            return flecha;
        }

        @Override
        public void paintCurrentValueBackground(Graphics g, java.awt.Rectangle limites, boolean tieneFoco) {
            g.setColor(Tema.actual().panelFondo());
            g.fillRect(limites.x, limites.y, limites.width, limites.height);
        }
    }
}
