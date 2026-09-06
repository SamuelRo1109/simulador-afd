package com.unbosque.afd.desktop.vista;

import com.unbosque.afd.desktop.controlador.ControladorAutomata;
import com.unbosque.afd.desktop.render.Paleta;

import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PanelMatrizTransiciones extends JPanel {

    public static final String SIN_DEFINIR = "—";

    private static final int ANCHO_COLUMNA = 96;

    private final ControladorAutomata controlador;
    private final ModeloTablaMatriz modeloTabla;
    private final JTable tabla;
    private final JLabel resumen = new JLabel(" ");

    private String firmaEstructura = "";

    public PanelMatrizTransiciones(ControladorAutomata controlador) {
        this.controlador = Objects.requireNonNull(controlador, "El controlador no puede ser nulo");
        this.modeloTabla = new ModeloTablaMatriz();
        this.tabla = new JTable(modeloTabla);
        setLayout(new BorderLayout(0, 6));
        setBorder(BorderFactory.createTitledBorder("Matriz de transiciones (δ)"));
        setOpaque(false);

        tabla.setRowHeight(24);
        tabla.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        tabla.setGridColor(Paleta.BORDE_SUAVE);
        tabla.setCellSelectionEnabled(true);
        tabla.getTableHeader().setReorderingAllowed(false);
        tabla.setDefaultRenderer(Object.class, new RenderizadorCelda());

        resumen.setFont(resumen.getFont().deriveFont(Font.PLAIN, 11f));
        resumen.setForeground(Paleta.TEXTO_TENUE);

        add(new JScrollPane(tabla), BorderLayout.CENTER);
        add(resumen, BorderLayout.SOUTH);
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

    private String firmaActual() {
        return controlador.modelo().estados() + " / " + controlador.modelo().simbolos();
    }

    private void configurarColumnas() {
        List<String> opciones = new ArrayList<>();
        opciones.add(SIN_DEFINIR);
        opciones.addAll(controlador.modelo().estados());

        for (int indice = 0; indice < tabla.getColumnCount(); indice++) {
            TableColumn columna = tabla.getColumnModel().getColumn(indice);
            columna.setPreferredWidth(indice == 0 ? 130 : ANCHO_COLUMNA);
            columna.setMinWidth(indice == 0 ? 90 : 70);
            if (indice > 0) {
                columna.setCellEditor(new DefaultCellEditor(new JComboBox<>(opciones.toArray(new String[0]))));
            }
        }
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
        public Component getTableCellRendererComponent(JTable tabla, Object valor, boolean seleccionada,
                                                       boolean enfocada, int fila, int columna) {
            super.getTableCellRendererComponent(tabla, valor, seleccionada, enfocada, fila, columna);
            boolean sinDefinir = columna > 0 && SIN_DEFINIR.equals(valor);

            setHorizontalAlignment(columna == 0 ? SwingConstants.LEFT : SwingConstants.CENTER);
            setFont(getFont().deriveFont(columna == 0 ? Font.BOLD : Font.PLAIN));
            setForeground(sinDefinir ? Paleta.RECHAZADA : Paleta.ESTADO_BORDE);
            setBackground(columna == 0
                    ? Paleta.FONDO
                    : sinDefinir ? Paleta.CELDA_SIN_DEFINIR : Paleta.ESTADO_RELLENO);
            setToolTipText(sinDefinir ? "Transicion sin definir: delta no es total" : null);
            return this;
        }
    }
}
