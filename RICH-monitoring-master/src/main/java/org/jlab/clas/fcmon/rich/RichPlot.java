/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jlab.clas.fcmon.rich;

import java.util.Map;
import javax.swing.JPanel;
import org.jlab.groot.graphics.EmbeddedCanvas;
import org.jlab.detector.view.DetectorShape2D;

/**
 *
 * @author kenjo
 */
public abstract class RichPlot {

    protected EmbeddedCanvas canvas;

    RichPlot(){
        canvas = new EmbeddedCanvas();
    }
    public abstract void reset();

    public abstract void fill(Map<Integer, RichHitCollection> rhits);

    public void process(Map<Integer, RichHitCollection> rhits){
        return;
    }
    
    public void setCanvasUpdate(int time) {
        canvas.initTimer(time);
    }

    public void processShape(DetectorShape2D shape) {
    }

    public JPanel getPanel() {
        return canvas;
    }
}
