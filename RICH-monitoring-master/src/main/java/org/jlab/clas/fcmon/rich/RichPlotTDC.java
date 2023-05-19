/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jlab.clas.fcmon.rich;

import java.awt.FlowLayout;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.geom.Line2D;
import java.util.Map;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.event.MouseInputAdapter;
import org.jlab.clas.fcmon.rich.RichHitCollection.Edge;
import org.jlab.clas.fcmon.rich.RichHitCollection.RichHit;
import org.jlab.clas.fcmon.rich.RichHitCollection.RichTDC;
import org.jlab.groot.data.H1F;
import org.jlab.detector.view.DetectorShape2D;
import org.jlab.groot.graphics.EmbeddedCanvas;


/**
 *
 * @author kenjo
 * RICH 2 added by Zachary Nickischer at Duquesne Univeristy
 */
public final class RichPlotTDC extends RichPlot {

    private class HistTDC {

        H1F htdc0 = new H1F("RICH TDC0", "TDC", 300, 0, 300);
        H1F htdc1 = new H1F("RICH TDC1", "TDC", 300, 0, 300);
        H1F hdeltaT = new H1F("RICH delta", "delta TDC", 150, 0, 150);


        public HistTDC() {
            htdc1.setLineColor(2);
        }

        public void setTitle(String title) {

            htdc0.setTitle(title);
            htdc1.setTitle(title);
            hdeltaT.setTitle(title);

        }
    }

    private final JPanel mainPanel = new JPanel(new BorderLayout());
    private final JComboBox tdcBox, lvlBox;
    private int selectedITile = 0, selectedIMaroc = 0;
    private final int ntiles = 138, nmarocs = 3, npixs = 64;

    private HistTDC hdet = new HistTDC();
    private HistTDC[][] hpmt = new HistTDC[ntiles][nmarocs];
    private HistTDC[][][] hpix = new HistTDC[ntiles][nmarocs][npixs];

    public RichPlotTDC(String name) {
        tdcBox = new JComboBox(new String[]{"TDC", "Tover"});
        lvlBox = new JComboBox(new String[]{"detector", "pmt", "pixel"});

        lvlBox.addActionListener(ev -> redraw());
        tdcBox.addActionListener(ev -> redraw());

        JToolBar toolBar = new JToolBar();
        toolBar.setLayout(new FlowLayout());
        toolBar.add(tdcBox);
        toolBar.add(lvlBox);

        /*
        canvas = canv;
        canvas.setLayout(null);

        ResizableLine lbl = new ResizableLine(0, 00, 1000, 500);
        DragListener drag = new DragListener();
        lbl.addMouseListener(drag);
        lbl.addMouseMotionListener(drag);
        canvas.add(lbl);

        lbl.setBounds(10, 10, 1000, 1000);

        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.add(canvas);
        rightPanel.add(lbl);
         */
        mainPanel.add(toolBar, BorderLayout.PAGE_START);
        mainPanel.add(canvas, BorderLayout.CENTER);
        mainPanel.setName(name);

        reset();
    }

    public class ResizableLine extends JComponent {

        private double x1, y1, x2, y2;

        ResizableLine(double x1, double y1, double x2, double y2) {
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
        }

        @Override
        public void paintComponent(Graphics g) {
            Line2D line = new Line2D.Double(x1, y1, x2, y2);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setColor(Color.GREEN);
            g2d.draw(line);
        }
    }

    public class DragListener extends MouseInputAdapter {

        Point location;
        MouseEvent pressed;

        @Override
        public void mousePressed(MouseEvent me) {
            pressed = me;
        }

        @Override
        public void mouseDragged(MouseEvent me) {
            Component component = me.getComponent();
            location = component.getLocation(location);
            int x = location.x - pressed.getX() + me.getX();
            int y = location.y - pressed.getY() + me.getY();
            component.setLocation(x, y);
        }
    }

    private void redraw() {
        canvas.clear();
        canvas.divide(1, 1);


        canvas.setStatBoxFontSize(14);
        if (lvlBox.getSelectedIndex() == 0) {
            if (tdcBox.getSelectedIndex() == 0) {
                canvas.draw(hdet.htdc0);
                canvas.draw(hdet.htdc1, "same");
            } else {
                canvas.cd(1);
                hdet.hdeltaT.setOptStat("1111");
                canvas.draw(hdet.hdeltaT);
            }
        } else if (lvlBox.getSelectedIndex() == 1) {
            if (tdcBox.getSelectedIndex() == 0) {
                canvas.draw(hpmt[selectedITile][selectedIMaroc].htdc0);
                canvas.draw(hpmt[selectedITile][selectedIMaroc].htdc1, "same");
            } else {
                hpmt[selectedITile][selectedIMaroc].hdeltaT.setOptStat("1111");
                canvas.draw(hpmt[selectedITile][selectedIMaroc].hdeltaT);
            }
        } else {
            canvas.divide(8, 8);
            if (tdcBox.getSelectedIndex() == 0) {
                for (int ipix = 0; ipix < npixs; ipix++) {
                    canvas.cd(ipix);
                    canvas.draw(hpix[selectedITile][selectedIMaroc][ipix].htdc0);
                    canvas.draw(hpix[selectedITile][selectedIMaroc][ipix].htdc1, "same");
                }
            } else {
                for (int ipix = 0; ipix < npixs; ipix++) {
                    canvas.cd(ipix);
                    canvas.draw(hpix[selectedITile][selectedIMaroc][ipix].hdeltaT);
                }
            }
        }
    }

    @Override
    public void reset() {
        hdet = new HistTDC();
        hpmt = new HistTDC[ntiles][nmarocs];
        hpix = new HistTDC[ntiles][nmarocs][npixs];

        hdet = new HistTDC();
        hdet.setTitle("Integrated over RICH");

        for (int itile = 0; itile < ntiles; itile++) {
            for (int imaroc = 0; imaroc < nmarocs; imaroc++) {
                hpmt[itile][imaroc] = new HistTDC();
                hpmt[itile][imaroc].setTitle("Integrated over PMT: tile " + (itile + 1) + " maroc " + imaroc);
            }
        }

        for (int itile = 0; itile < ntiles; itile++) {
            for (int imaroc = 0; imaroc < nmarocs; imaroc++) {
                for (int ipix = 0; ipix < npixs; ipix++) {
                    hpix[itile][imaroc][ipix] = new HistTDC();
                    hpix[itile][imaroc][ipix].setTitle("tile/maroc/pix: " + (itile + 1) + " / " + imaroc + " / " + (ipix + 1));
                }
            }
        }

        redraw();
    }

    @Override
   public void fill(Map<Integer, RichHitCollection> rhits) {
        for (RichHitCollection rhit : rhits.values()) {

            for (RichHit rh : rhit.hitList) {

                int t1 = rh.time + rh.delta;

                hdet.htdc0.fill(rh.time);
                hpmt[rhit.itile][rhit.imaroc].htdc0.fill(rh.time);
                hpix[rhit.itile][rhit.imaroc][rhit.ipix].htdc0.fill(rh.time);

                hdet.htdc1.fill(t1);
                hpmt[rhit.itile][rhit.imaroc].htdc1.fill(t1);
                hpix[rhit.itile][rhit.imaroc][rhit.ipix].htdc1.fill(t1);

                hdet.hdeltaT.fill(rh.delta);
                hpmt[rhit.itile][rhit.imaroc].hdeltaT.fill(rh.delta);
                hpix[rhit.itile][rhit.imaroc][rhit.ipix].hdeltaT.fill(rh.delta);
            }
        }
    }

    @Override
    public JPanel getPanel() {
        return mainPanel;
    }

    public void processShape(DetectorShape2D shape) {
        selectedITile = shape.getDescriptor().getLayer() - 1;
        selectedIMaroc = shape.getDescriptor().getComponent();
        redraw();
    }
}
