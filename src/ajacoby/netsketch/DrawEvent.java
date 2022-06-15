package ajacoby.netsketch;

import ajacoby.stdlib.Draw;

import java.awt.*;
import java.awt.geom.Point2D;
import java.io.Serializable;

public class DrawEvent implements Serializable {
   public static enum DrawEventType {POINT, LINE}

   private DrawEventType type;
   private String source;
   private Point2D pt1, pt2;
   private Color color;

   public DrawEvent(String source, Point2D pt1, Point2D pt2, Color color, DrawEventType type) {
      this.source = source;
      this.pt1 = pt1;
      this.pt2 = pt2;
      this.color = color;
      this.type = type;
   }

   public String getSource() {
      return source;
   }

   public Point2D getPt1() {
      return pt1;
   }

   public Point2D getPt2() {
      return pt2;
   }

   public Color getColor() {
      return color;
   }

   public DrawEventType getType() {
      return type;
   }

   @Override
   public String toString() {
      return "DrawEvent{" +
            "source='" + source + '\'' +
            ", pt1=" + pt1 +
            ", pt2=" + pt2 +
            ", color=" + color +
            ", type=" + type +
            '}';
   }

   public void draw(Draw win) {
      double x1 = getPt1().getX();
      double y1 = getPt1().getY();
      Point2D pt2 = getPt2();
      double x2 = (pt2 != null)? pt2.getX() : 0;
      double y2 = (pt2 != null)? pt2.getY() : 0;
      synchronized (win) {
         win.setPenColor(getColor());
         switch (getType()) {
            case POINT -> {
               win.filledCircle(x1, y1, 0.005);
            }
            case LINE -> {
               win.line(x1, y1, x2, y2);
            }
         }
      }
   }

}
