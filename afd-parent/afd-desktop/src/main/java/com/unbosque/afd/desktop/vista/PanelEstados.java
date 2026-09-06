package com.unbosque.afd.desktop.vista;

import com.unbosque.afd.desktop.controlador.ControladorAutomata;
import com.unbosque.afd.desktop.render.Paleta;

import javax.swing.AbstractCellEditor;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.util.List;
import java.util.Objects;

public class PanelEstados extends JPanel {

    private static final String[] COLUMNAS = {"Nombre", "Inicial", "Aceptacion"};

    private final ControladorAutomata controlador;
    private final ModeloTablaEstados modeloTabla;
    private final JTable tabla;
    private final JTextField campoNombre = new JTextField(10);
    private final JLabel mensaje = new JLabel(" ");

    public PanelEstados(ControladorAutomata controlador) {
        this.controlador = Objects.requireNonNull(controlador, "El controlador no puede ser nulo");
        this.modeloTabla = new ModeloTablaEstados();
        this.tabla = new JTable(modeloTabla);
        setLayout(new BorderLayout(0, 6));
        setBorder(BorderFactory.createTitledBorder("Estados (Q)"));
        setOpaque(false);

        configurarTabla();
        add(construirEntrada(), BorderLayout.NORTH);
        add(new JScrollPane(tabla), BorderLayout.CENTER);
        add(construirPie(), BorderLayout.SOUTH);
    }

    public void refrescar() {
        if (tabla.isEditing()) {
            tabla.removeEditor();
        }
        modeloTabla.fireTableDataChanged();
    }

    private void configurarTabla() {
        tabla.setRowHeight(24);
        tabla.setGridColor(Paleta.BORDE_SUAVE);
        tabla.getTableHeader().setReorderingAllowed(false);
        tabla.setSelectionBackground(Paleta.mezclar(Paleta.ARISTA, Paleta.ESTADO_RELLENO, 0.20));
        tabla.setSelectionForeground(Paleta.ESTADO_BORDE);

        tabla.getColumnModel().getColumn(0).setPreferredWidth(150);
        tabla.getColumnModel().getColumn(1).setPreferredWidth(60);
        tabla.getColumnModel().getColumn(2).setPreferredWidth(80);
        tabla.getColumnModel().getColumn(1).setCellRenderer(new RenderizadorRadio());
        tabla.getColumnModel().getColumn(1).setCellEditor(new EditorRadio());
    }

    private JPanel construirEntrada() {
        JPanel entrada = new JPanel();
        entrada.setOpaque(false);
        entrada.setLayout(new BoxLayout(entrada, BoxLayout.X_AXIS));

        campoNombre.setMaximumSize(new Dimension(180, 26));
        campoNombre.addActionListener(evento -> agregar());

        JButton agregar = new JButton("Agregar");
        agregar.addActionListener(evento -> agregar());

        entrada.add(new JLabel("Nombre: "));
        entrada.add(campoNombre);
        entrada.add(Box.createHorizontalStrut(6));
        entrada.add(agregar);
        entrada.add(Box.createHorizontalGlue());
        return entrada;
    }

    private JPanel construirPie() {
        JPanel pie = new JPanel(new BorderLayout());
        pie.setOpaque(false);

        JButton eliminar = new JButton("Eliminar seleccionado");
        eliminar.addActionListener(evento -> eliminarSeleccionado());

        mensaje.setForeground(Paleta.RECHAZADA);
        mensaje.setFont(mensaje.getFont().deriveFont(Font.PLAIN, 11f));

        JPanel fila = new JPanel();
        fila.setOpaque(false);
        fila.setLayout(new BoxLayout(fila, BoxLayout.X_AXIS));
        fila.add(eliminar);
        fila.add(Box.createHorizontalGlue());

        pie.add(fila, BorderLayout.NORTH);
        pie.add(mensaje, BorderLayout.SOUTH);
        return pie;
    }

    private void agregar() {
        String nombre = campoNombre.getText();
        if (controlador.agregarEstado(nombre)) {
            mensaje.setText(" ");
        } else {
            mensaje.setText(nombre == null || nombre.isBlank()
                    ? "El nombre no puede estar vacio"
                    : "Ya existe un estado con ese nombre");
        }
        campoNombre.setText("");
        campoNombre.requestFocusInWindow();
    }

    private void eliminarSeleccionado() {
        int fila = tabla.getSelectedRow();
        if (fila < 0 || fila >= controlador.modelo().estados().size()) {
            mensaje.setText("Selecciona un estado en la tabla");
            return;
        }
        mensaje.setText(" ");
        controlador.eliminarEstado(controlador.modelo().estados().get(fila));
    }

    private final class ModeloTablaEstados extends AbstractTableModel {

        @Override
        public int getRowCount() {
            return controlador.modelo().estados().size();
        }

        @Override
        public int getColumnCount() {
            return COLUMNAS.length;
        }

        @Override
        public String getColumnName(int columna) {
            return COLUMNAS[columna];
        }

        @Override
        public Class<?> getColumnClass(int columna) {
            return columna == 0 ? String.class : Boolean.class;
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
            return switch (columna) {
                case 0 -> estado;
                case 1 -> controlador.modelo().esInicial(estado);
                default -> controlador.modelo().esAceptacion(estado);
            };
        }

        @Override
        public void setValueAt(Object valor, int fila, int columna) {
            List<String> estados = controlador.modelo().estados();
            if (fila >= estados.size()) {
                return;
            }
            String estado = estados.get(fila);
            if (columna == 1) {
                controlador.establecerInicial(estado);
            } else if (columna == 2) {
                controlador.establecerAceptacion(estado, Boolean.TRUE.equals(valor));
            }
        }
    }

    private static final class RenderizadorRadio extends JRadioButton implements TableCellRenderer {

        private RenderizadorRadio() {
            setHorizontalAlignment(SwingConstants.CENTER);
            setOpaque(true);
        }

        @Override
        public Component getTableCellRendererComponent(JTable tabla, Object valor, boolean seleccionada,
                                                       boolean enfocada, int fila, int columna) {
            setSelected(Boolean.TRUE.equals(valor));
            setBackground(seleccionada ? tabla.getSelectionBackground() : tabla.getBackground());
            setForeground(Paleta.INICIAL);
            return this;
        }
    }

    private final class EditorRadio extends AbstractCellEditor implements TableCellEditor {

        private final JRadioButton boton = new JRadioButton();

        private EditorRadio() {
            boton.setHorizontalAlignment(SwingConstants.CENTER);
            boton.setForeground(Paleta.INICIAL);
            boton.addActionListener(evento -> fireEditingStopped());
        }

        @Override
        public Object getCellEditorValue() {
            return Boolean.TRUE;
        }

        @Override
        public Component getTableCellEditorComponent(JTable tabla, Object valor, boolean seleccionada,
                                                     int fila, int columna) {
            boton.setSelected(Boolean.TRUE.equals(valor));
            boton.setBackground(tabla.getBackground());
            return boton;
        }
    }
}
