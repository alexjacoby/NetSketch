package ajacoby.netsketch;

import ajacoby.stdlib.Draw;

import java.awt.*;
import java.awt.geom.Point2D;
import java.io.Serializable;

public class DrawEvent implements Serializable {
   public static enum DrawEventType {POINT, LINE, CLEAR}

   /**
    * Maintains parameters for different types of DrawEvents.
    * <p>
    *    TODO: Consider subclassing DrawEvent and using a static factory method instead.
    * </p>
    */
   private abstract class DrawEventPayload implements Serializable {
      private static final long serialVersionUID = 1;
      public abstract void draw(Draw win);
   }

   private class PointPayload extends DrawEventPayload {
      public final Point2D pt;
      public final Color color;
      public final double radius;

      public PointPayload(Point2D pt, Color color, double radius) {
         this.pt = pt;
         this.color = color;
         this.radius = radius;
      }

      @Override
      public void draw(Draw win) {
         synchronized (win) {
            win.setPenColor(color);
            win.filledCircle(pt.getX(), pt.getY(), radius);
         }
      }
   } // PointPayload

   private class LinePayload extends DrawEventPayload {
      public final Point2D pt1;
      public final Point2D pt2;
      public final Color color;
      public final double radius;

      public LinePayload(Point2D pt1, Point2D pt2, Color color, double radius) {
         this.pt1 = pt1;
         this.pt2 = pt2;
         this.color = color;
         this.radius = radius;
      }

      @Override
      public void draw(Draw win) {
         synchronized (win) {
            win.setPenColor(color);
            win.setPenRadius(radius);
            win.line(pt1.getX(), pt1.getY(), pt2.getX(), pt2.getY());
         }
      }
   } // LinePayload

   private class ClearPayload extends DrawEventPayload {
      @Override
      public void draw(Draw win) {
         synchronized (win) {
            win.clear();
         }
      }
   } // ClearPayload

   private static final long serialVersionUID = 2;

   private String source;
   private DrawEventType type;
   private DrawEventPayload payload;

   public DrawEvent(String source,
                    Point2D pt1, Point2D pt2,
                    Color color,
                    double radius,
                    DrawEventType type) {
      this.source = source;
      this.type = type;
      this.payload = switch (type) {
         case POINT -> new PointPayload(pt1, color, radius);
         case LINE -> new LinePayload(pt1, pt2, color, radius);
         case CLEAR -> new ClearPayload();
      };
   }

   /**
    * Constructor for simpler event types like CLEAR.
    */
   public DrawEvent(String source, DrawEventType type) {
      this.type = type;
      this.source = source;
      this.payload = new ClearPayload();
   }

   public String getSource() {
      return source;
   }

   public DrawEventType getType() {
      return type;
   }

   @Override
   public String toString() {
      return "DrawEvent{" +
            "source='" + source + '\'' +
            ", type=" + type +
            '}';
   }

   public void draw(Draw win) {
      payload.draw(win);
   }

}
