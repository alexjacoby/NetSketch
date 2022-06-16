package ajacoby.netsketch;

import ajacoby.stdlib.Draw;

import java.awt.*;
import java.awt.geom.Point2D;
import java.io.Serializable;

public class DrawEvent implements Serializable {
   public static enum DrawEventType {POINT, LINE, CLEAR}

   private static final long serialVersionUID = 1;

   private DrawEventType type;
   private String source;
   private Point2D pt1, pt2;
   private Color color = Color.BLACK;
   private double radius = 0.005;

   public DrawEvent(String source,
                    Point2D pt1, Point2D pt2,
                    Color color,
                    double radius,
                    DrawEventType type) {
      this.source = source;
      this.pt1 = pt1;
      this.pt2 = pt2;
      this.color = color;
      this.radius = radius;
      this.type = type;
   }

   /**
    * Constructor for simpler event types like CLEAR.
    */
   public DrawEvent(String source, DrawEventType type) {
      this.type = type;
      this.source = source;
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

   public double getRadius() { return radius; }

   public void setRadius(double radius) { this.radius = radius; }

   @Override
   public String toString() {
      return "DrawEvent{" +
            "source='" + source + '\'' +
            ", pt1=" + pt1 +
            ", pt2=" + pt2 +
            ", color=" + color +
            ", radius=" + radius +
            ", type=" + type +
            '}';
   }

   public void draw(Draw win) {
      Point2D pt1 = getPt1();
      double x1 = (pt1 != null)? pt1.getX() : 0;
      double y1 = (pt1 != null)? pt1.getY() : 0;
      Point2D pt2 = getPt2();
      double x2 = (pt2 != null)? pt2.getX() : 0;
      double y2 = (pt2 != null)? pt2.getY() : 0;
      synchronized (win) {
         win.setPenColor(getColor());
         win.setPenRadius(radius);
         switch (getType()) {
            case POINT -> {
               win.filledCircle(x1, y1, radius);
            }
            case LINE -> {
               win.line(x1, y1, x2, y2);
            }
            case CLEAR -> {
               win.clear();
            }
         }
      }
   }

}
