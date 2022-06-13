package ajacoby.netdraw;

import ajacoby.stdlib.Draw;
import ajacoby.stdlib.DrawListener;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Point2D;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Client implements Serializable {

   public static final int NOT_CONNECTED = -1;

   // Add if you want to allow clean simultaneous drawing!
   // private Map<String, List<Point2D.Double>> pointsMap;
   private transient List<Point2D> points;

   private String playerName;
   private int clientID = NOT_CONNECTED;
   private boolean hasID = false;
   private Color color;
   private transient ClientThread thread;
   private transient Draw win;

   public Client(String name, String serverIPAddr, int serverPort) {
      points = new ArrayList<>();
      color = Color.BLACK;
      win = new Draw("NetDraw: " + name);
      System.out.println("Client: Establishing connection");
      thread = new ClientThread(serverIPAddr, serverPort, this);
      thread.start();
      while (clientID == NOT_CONNECTED) {
         System.out.println("waiting: current id is " + clientID);
         try { Thread.sleep(10); } catch (InterruptedException e) {
            throw new RuntimeException(e);
         }
      }
      if (clientID == Client.NOT_CONNECTED){
         System.out.println("Something went wrong obtaining a client ID. Disconnecting from server.");
         thread.disconnect();
      }
      System.out.println("got ID: " + clientID);
      initDrawListeners();
   }

   private void initDrawListeners() {
      win.addListener(new DrawListener() {
         @Override
         public void mouseClicked(double x, double y) {
            Point2D pt = new Point2D.Double(x, y);
            System.out.println("mouse clicked: " + pt);
            DrawEvent de = new DrawEvent(playerName, pt, color, DrawEvent.DrawEventType.POINT);
            draw(de);
            pushDrawEvent(de);
         }

         @Override
         public void mouseDragged(double x, double y) {
            Point2D pt = new Point2D.Double(x, y);
            System.out.println("mouse clicked: " + pt);
            DrawEvent de = new DrawEvent(playerName, pt, color, DrawEvent.DrawEventType.LINE);
            draw(de);
            pushDrawEvent(de);
         }
      });
   }

   public void setClientID(int clientID) {
      this.clientID = clientID;
   }

   public int getClientID() {
      return clientID;
   }


   public void pushDrawEvent(DrawEvent de) {
      thread.pushDrawEvent(de);
   }

   public void disconnect(){
      thread.disconnect();
   }

   public void draw(DrawEvent de) {
      win.setPenColor(de.getColor());
      String source = de.getSource();
      Point2D location = de.getLocation();
      switch (de.getType()) {
         case POINT:
            win.filledCircle(location.getX(), location.getY(), 0.005);
            break;
         case LINE:
            if (points.size() > 1) {
               Point2D lastLoc = points.get(points.size() - 1);
               win.line(lastLoc.getX(), lastLoc.getY(),
                     location.getX(), location.getY());
            } else {
               win.filledCircle(location.getX(), location.getY(), 0.005);
            }
            break;
      }
      points.add(location);
   }
}
