package ajacoby.netdraw;

import java.awt.*;
import java.awt.geom.Point2D;

public class DrawEvent {
   public static enum DrawEventType {POINT, LINE}

   private String source;
   private Point2D location;
   private Color color;
   private DrawEventType type;

   public DrawEvent(String source, Point2D location, Color color, DrawEventType type) {
      this.source = source;
      this.location = location;
      this.color = color;
      this.type = type;
   }

   public String getSource() {
      return source;
   }

   public Point2D getLocation() {
      return location;
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
            ", location=" + location +
            ", color=" + color +
            ", type=" + type +
            '}';
   }
}
