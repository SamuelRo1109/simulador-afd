package com.unbosque.afd.desktop.vista;

import com.unbosque.afd.core.modelo.ErrorValidacion;
import com.unbosque.afd.core.modelo.ResultadoValidacion;
import com.unbosque.afd.desktop.controlador.ControladorAutomata;
import com.unbosque.afd.desktop.render.Paleta;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.util.Objects;

public class PanelValidacion extends JPanel {

    private static final int ANCHO_TEXTO = 300;

    private final ControladorAutomata controlador;
    private final DefaultListModel<ErrorValidacion> modeloLista = new DefaultListModel<>();
    private final JList<ErrorValidacion> lista = new JList<>(modeloLista);
    private final JLabel encabezado = new JLabel(" ");
    private final JButton completarTrampa = new JButton("Completar con estado trampa");

    public PanelValidacion(ControladorAutomata controlador) {
        this.controlador = Objects.requireNonNull(controlador, "El controlador no puede ser nulo");
        setLayout(new BorderLayout(0, 6));
        setBorder(BorderFactory.createTitledBorder("Validacion"));
        setOpaque(false);

        encabezado.setFont(encabezado.getFont().deriveFont(Font.BOLD, 12f));

        lista.setCellRenderer(new RenderizadorHallazgo());
        lista.setBackground(Paleta.ESTADO_RELLENO);

        completarTrampa.setEnabled(false);
        completarTrampa.setToolTipText("Cierra las transiciones faltantes con un estado absorbente");
        completarTrampa.addActionListener(evento -> controlador.completarConEstadoTrampa());

        JPanel pie = new JPanel();
        pie.setOpaque(false);
        pie.setLayout(new BoxLayout(pie, BoxLayout.X_AXIS));
        pie.add(completarTrampa);
        pie.add(Box.createHorizontalGlue());

        add(encabezado, BorderLayout.NORTH);
        add(new JScrollPane(lista), BorderLayout.CENTER);
        add(pie, BorderLayout.SOUTH);
    }

    public void refrescar() {
        ResultadoValidacion resultado = controlador.validacionActual();

        modeloLista.clear();
        resultado.errores().forEach(modeloLista::addElement);
        resultado.advertencias().forEach(modeloLista::addElement);

        int errores = resultado.errores().size();
        int advertencias = resultado.advertencias().size();
        if (resultado.hallazgos().isEmpty()) {
            encabezado.setText("Automata valido, sin advertencias");
            encabezado.setForeground(Paleta.ACEPTADA);
        } else if (errores == 0) {
            encabezado.setText("Automata valido con " + advertencias + " advertencia(s)");
            encabezado.setForeground(Paleta.ACTIVO);
        } else {
            encabezado.setText(errores + " error(es) y " + advertencias + " advertencia(s)");
            encabezado.setForeground(Paleta.RECHAZADA);
        }

        completarTrampa.setEnabled(controlador.puedeCompletarConEstadoTrampa());
    }

    private static String codigoCorto(ErrorValidacion hallazgo) {
        String codigo = hallazgo.codigo();
        int separador = codigo.indexOf('_');
        return separador < 0 ? codigo : codigo.substring(0, separador);
    }

    private static final class RenderizadorHallazgo extends JLabel implements ListCellRenderer<ErrorValidacion> {

        private RenderizadorHallazgo() {
            setOpaque(true);
            setBorder(BorderFactory.createEmptyBorder(3, 6, 3, 6));
            setVerticalAlignment(TOP);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends ErrorValidacion> lista,
                                                      ErrorValidacion hallazgo,
                                                      int indice,
                                                      boolean seleccionado,
                                                      boolean enfocado) {
            setText("<html><body style='width:" + ANCHO_TEXTO + "px'><b>[" + codigoCorto(hallazgo) + "]</b> "
                    + hallazgo.mensaje() + "</body></html>");
            setForeground(hallazgo.esError() ? Paleta.RECHAZADA : Paleta.ACTIVO);
            setBackground(seleccionado
                    ? Paleta.mezclar(Paleta.ARISTA, Paleta.ESTADO_RELLENO, 0.15)
                    : Paleta.ESTADO_RELLENO);
            return this;
        }
    }
}
