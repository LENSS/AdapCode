/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package serialcomm;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.QuadCurve2D;

/**
 *
 * @author lenss
 */

public class SavedLines {
    private double x1[];
    private double y1[];
    private double x2[];
    private double y2[];
    private double ctrlx[];
    private double ctrly[];
    private Color color[];
    private int nodeid[];
    private static final int MAX = 500;
    private int index;


    public SavedLines () {
        x1 = new double[MAX];
        x2 = new double[MAX];
        y1 = new double[MAX];
        y2 = new double[MAX];
        ctrlx = new double[MAX];
        ctrly = new double[MAX];
        color = new Color[MAX];
        nodeid = new int[MAX];
        index = 0;
    }

    public void saveLine(double x1_, double y1_, double ctrlx_, double ctrly_, double x2_, double y2_, Color color_, int nodeid_) {
        x1[index] = x1_;
        x2[index] = x2_;
        y1[index] = y1_;
        y2[index] = y2_;
        ctrlx[index] = ctrlx_;
        ctrly[index] = ctrly_;
        color[index] = color_;
        nodeid[index] = nodeid_;
        index++;
    }

    public void drawThickLine(int nodeid_) {
        Graphics2D g = (Graphics2D) SerialCommApp.getApplication().getView().getPanel().getGraphics();
        QuadCurve2D q = new QuadCurve2D.Float();

        for (int i = 0; i < MAX; i++) {
            if (nodeid[i] == nodeid_) {
                System.out.println("matched drawing thick line.");
                q.setCurve(x1[i], y1[i], ctrlx[i], ctrly[i], x2[i], y2[i]);
                g.setStroke(new BasicStroke(5));
                g.setColor(color[index]);
                g.draw(q);
            }
        }
    }

}

