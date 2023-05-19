/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jlab.clas.fcmon.rich;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.geom.Rectangle2D;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.swing.JCheckBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JToolBar;
import javax.swing.SpinnerNumberModel;
import javax.swing.Timer;
import javax.swing.event.ChangeListener;
import org.jlab.clas.fcmon.RichView;
import org.jlab.clas.fcmon.RichView.RICHtile;
import org.jlab.clas.fcmon.rich.RichHitCollection.RichHit;
import org.jlab.detector.view.DetectorShape2D;
import org.jlab.groot.base.TColorPalette;
import org.jlab.clas.fcmon.RICHMon;

/**
 *
 * @author kenjo
 * RICH 2 added by Zachary Nickischer at Duquesne Univeristy
 */
public final class RichPlotOccupancy extends RichPlot {

    private final JPanel mainPanel = new JPanel(new BorderLayout());
    private final RichPanel evdisPanel = new RichPanel();
    private final JCheckBox evdisBox;
    private Boolean evdisMode = false;
    Map<Integer, RichPixWeight> hits = new ConcurrentHashMap<>();
    TColorPalette colPalette = new TColorPalette();
    private double scaleWeight = 1;
    private int leadingMin = 0, leadingMax = 300, trailingMin = 0, trailingMax = 300, deltaMin = 0, deltaMax = 150;
    private int npmts = 0;

    private final JLabel evntLbl = new JLabel();

    public RichPlotOccupancy(String name){
        evdisBox = new JCheckBox("Event Display Mode");
        evdisBox.addActionListener(ev -> evdisMode = evdisBox.isSelected());

        JLabel lbl0 = new JLabel("Set maximum");
        JLabel lbl1 = new JLabel("%");
        JSpinner scaleField = new JSpinner(new SpinnerNumberModel(50, 0, 100, 0.01));
        scaleField.addChangeListener(ev -> scaleWeight = ((double) scaleField.getValue()) / 100.0);

        JPanel toolbars = new JPanel(new GridLayout(0, 1));

        JToolBar mainBar = new JToolBar();
        mainBar.setLayout(new FlowLayout());
        mainBar.add(evdisBox);
        mainBar.addSeparator();
        mainBar.add(lbl0);
        mainBar.add(scaleField);
        mainBar.add(lbl1);

        JToolBar rangeBar = new JToolBar();
        rangeBar.setLayout(new FlowLayout());

        class RangeSpinners {

            JSpinner thr0, thr1;

            RangeSpinners(String name, int min, int max) {
                thr0 = new JSpinner(new SpinnerNumberModel(min, min, max, 1));
                thr1 = new JSpinner(new SpinnerNumberModel(max, min, max, 1));

                JLabel thr0Lbl = new JLabel(name + " from:");
                thr0Lbl.setLabelFor(thr0);
                JLabel thr1Lbl = new JLabel("to:");
                thr1Lbl.setLabelFor(thr1);
                rangeBar.add(thr0Lbl);
                rangeBar.add(thr0);
                rangeBar.add(thr1Lbl);
                rangeBar.add(thr1);
                rangeBar.addSeparator();
            }

            void addChangeListener(ChangeListener cl0, ChangeListener cl1) {
                thr0.addChangeListener(cl0);
                thr1.addChangeListener(cl1);
            }

            int getMin() {
                return (int) thr0.getValue();
            }

            int getMax() {
                return (int) thr1.getValue();
            }
        };

        RangeSpinners leadRange = new RangeSpinners("Leading", leadingMin, leadingMax);
        leadRange.addChangeListener(ev -> leadingMin = leadRange.getMin(), ev -> leadingMax = leadRange.getMax());
        RangeSpinners trailRange = new RangeSpinners("Trailing", trailingMin, trailingMax);
        trailRange.addChangeListener(ev -> trailingMin = trailRange.getMin(), ev -> trailingMax = trailRange.getMax());
        RangeSpinners deltaRange = new RangeSpinners("TimeOver", deltaMin, deltaMax);
        deltaRange.addChangeListener(ev -> deltaMin = deltaRange.getMin(), ev -> deltaMax = deltaRange.getMax());

        JLabel npmtlbl = new JLabel("number of hit MAPMTs:");
        JSpinner npmtSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 391, 1));
        npmtSpinner.addChangeListener(ev -> npmts = (int) npmtSpinner.getValue());
        npmtlbl.setLabelFor(npmtSpinner);
        rangeBar.add(npmtlbl);
        rangeBar.add(npmtSpinner);

        toolbars.add(mainBar);
        toolbars.add(rangeBar);

        mainPanel.add(toolbars, BorderLayout.PAGE_START);
        mainPanel.add(evdisPanel, BorderLayout.CENTER);
       
        mainPanel.setName(name);


        reset();
    }

    @Override
    public void reset() {
        hits.clear();
    }

    @Override
    public void process(Map<Integer, RichHitCollection> rhitMap) {
        for (RichHitCollection rhits : rhitMap.values()) {
            ListIterator<RichHit> iter = rhits.hitList.listIterator();
            while (iter.hasNext()) {
                RichHit rhit = iter.next();
                int trailingTime = rhit.time + rhit.delta;

                if (rhit.time < leadingMin || rhit.time > leadingMax
                        || trailingTime < trailingMin || trailingTime > trailingMax
                        || rhit.delta < deltaMin || rhit.delta > deltaMax) {
                    iter.remove();
                }
            }
        }

        int[] counts = new int[2000];
        for (RichHitCollection rh : rhitMap.values()) {
            int id = rh.itile * 10 + rh.imaroc;
            counts[id]++;
        }

        int npmthit = 0;
        for (int i = 0; i < 2000; i++) {
            if (counts[i] > 0) {
                npmthit++;
            }
        }
        if (npmthit < npmts) {
            rhitMap.clear();
        }
    }

    @Override
    public void fill(Map<Integer, RichHitCollection> rhitMap) {
        if (evdisMode) {
            hits.clear();
        }

        for (Map.Entry<Integer, RichHitCollection> entry : rhitMap.entrySet()) {
            if (!hits.containsKey(entry.getKey())) {
                hits.put(entry.getKey(), new RichPixWeight(entry.getValue()));
            }
            hits.get(entry.getKey()).fill(entry.getValue());
        }
        if (evdisMode) {
            evdisPanel.repaint();
        }
    }

    @Override
    public JPanel getPanel() {
        return mainPanel;
    }

    @Override
    public void setCanvasUpdate(int period) {
        Timer updateTimer = new Timer(period, ev -> evdisPanel.repaint());
        updateTimer.start();

    }

    private class RichPixWeight {

        int itile = 0;
        int imaroc = 0;
        int ipix = 0;
        int nhits = 0;

        RichPixWeight(RichHitCollection rhit) {
            itile = rhit.itile;
            imaroc = rhit.imaroc;
            ipix = rhit.ipix;
        }

        void fill(RichHitCollection rhit) {
            nhits += rhit.hitList.size();
        }
    }

    private class RichPanel extends JPanel {

        RichView rview = new RichView();
        List<DetectorShape2D> pmtShapes = new ArrayList<>();
        private double xmin = 1E6, xmax = -1E6, ymin = 1E6, ymax = -1E6;

        public RichPanel() {
            setLayout(null);
            setBackground(Color.lightGray);

            for (RICHtile rtile : rview.getTiles()) {
                for (DetectorShape2D rpmt : rtile.getPMTs()) {
                    for (int ipoint = 0; ipoint < rpmt.getShapePath().size(); ipoint++) {
                        double xx = rpmt.getShapePath().point(ipoint).x();
                        double yy = rpmt.getShapePath().point(ipoint).y();
                        xmin = Math.min(xmin, xx);
                        xmax = Math.max(xmax, xx);
                        ymin = Math.min(ymin, yy);
                        ymax = Math.max(ymax, yy);
                    }
                }
            }

        }

        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setPaint(Color.darkGray);

            double scale = 0.9 * Math.min(getWidth() / (xmax - xmin), getHeight() / (ymax - ymin));
            double offx = (getWidth() - scale * (xmax - xmin)) / 2;
            double offy = (getHeight() - scale * (ymax - ymin)) / 2;
            double ww = 0;

            for (RICHtile rtile : rview.getTiles()) {
                for (DetectorShape2D rpmt : rtile.getPMTs()) {
                    double xx = rpmt.getShapePath().point(0).x();
                    double yy = rpmt.getShapePath().point(0).y();
                    ww = Math.abs(rpmt.getShapePath().point(0).y() - rpmt.getShapePath().point(1).y()) * scale;

                    xx = (xx - xmin) * scale + offx;
                    yy = (yy - ymin) * scale + offy;
                    Rectangle2D.Double pmt = new Rectangle2D.Double(xx, yy, ww, ww);

                    g2.draw(pmt);
                }
            }

            if (!hits.isEmpty()) {
                double maxWeight = 0;
                if (evdisMode) {
                    g2.setPaint(Color.MAGENTA);
                } else {
                    maxWeight = hits.values().stream().max(Comparator.comparing(rw -> rw.nhits)).get().nhits * scaleWeight;
                    g2.setPaint(Color.white);
                    g2.fillRect(getWidth() - 60, getHeight() / 4, 60, getHeight() * 3 / 4);
                    colPalette.draw(g2, getWidth() - 55, getHeight() * 9 / 32, 20, getHeight() * 11 / 16, 0, maxWeight, false);
                }

                for (RichPixWeight rpix : hits.values()) {
                    double xx = rview.getTile(rpix.itile).getPixel(rpix.imaroc, rpix.ipix).x;
                    double yy = rview.getTile(rpix.itile).getPixel(rpix.imaroc, rpix.ipix).y;
                    xx = (xx - xmin) * scale + offx;
                    yy = (yy - ymin) * scale + offy;

                    if (!evdisMode) {
                        g2.setPaint(colPalette.getColor3D(rpix.nhits, maxWeight, false));
                    }
                    g2.fill(new Rectangle2D.Double(xx, yy, ww / 8, ww / 8));
                }
            }
        }
    }
}
