package ajacoby.netsketch;

import ajacoby.stdlib.Draw;
import ajacoby.stdlib.DrawListener;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

/**
 * Connects to NetSketchServer to allow user to draw on shared canvas.
 * <p>
 * Demo of and experiment with basic networking, threads, and Swing components.
 *
 * @author A. Jacoby (June 2022)
 */
public class NetSketchClient {
   public static final String CONNECT_PREFIX = "NetSketchClient connect: ";
   private Socket clientSocket;
   private ObjectOutputStream out;
   private ObjectInputStream in;
   private String name;
   /** Draw object works like a canvas embedded in our JFrame window. */
   private Draw draw;
   /** Window with draw canvas and controls. */
   private JFrame window;
   /** Each client starts with a random color. */
   private Color color = Draw.randomColor();
   /** Pen radius for this client. */
   private double radius = 0.005;
   /** Last mouse coordinate for drag operations. */
   private Point2D lastPoint;
   /** Flag to shut down. */
   private boolean isClientRunning = true;

   public NetSketchClient(String host, String name) {
      this.name = name;
      System.out.println("NetSketchClient connecting to " + host + ":" + NetSketchServer.PORT);
      // Network setup
      try {
         clientSocket = new Socket(host, NetSketchServer.PORT);
         System.out.println("Connected!");
         out = new ObjectOutputStream(clientSocket.getOutputStream());
         in = new ObjectInputStream(clientSocket.getInputStream());
         out.writeObject(CONNECT_PREFIX + name);
         out.flush();
      } catch (UnknownHostException e) {
         throw new RuntimeException(e);
      } catch (IOException e) {
         throw new RuntimeException(e);
      }
      initDraw();
      initWindow();
   } // NetSketchClient()

   private void initDraw() {
      draw = new Draw();
      // Hide the default window since we'll embed it in our own

      draw.addListener(new DrawListener() {
         @Override public void mouseDragged(double x, double y) {
            Point2D pt2 = new Point2D.Double(x, y);
            if (lastPoint != null) {
               DrawEvent de = new DrawEvent(name,
                     lastPoint, pt2, color, radius,
                     DrawEvent.DrawEventType.LINE);
               de.draw(draw);
               send(de);
            }
            lastPoint = pt2;
         }

         @Override public void mouseReleased(double x, double y) {
            lastPoint = null;
         }

         @Override public void mouseClicked(double x, double y) {
            Point2D pt1 = new Point2D.Double(x, y);
            Point2D pt2 = null;
            DrawEvent de = new DrawEvent(name,
                  pt1, pt2, color, radius,
                  DrawEvent.DrawEventType.POINT);
            de.draw(draw);
            send(de);
         }
      });
   }

   private void initWindow() {
      window = new JFrame("NetSketch Client");
      window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      window.setLayout(new BoxLayout(window.getContentPane(), BoxLayout.LINE_AXIS));
      // Add the draw canvas
      window.add(draw.getJLabel());
      // Add the controls to the right
      Box controlBox = Box.createVerticalBox();
      window.add(controlBox);
      controlBox.add(Box.createVerticalGlue());
      JColorChooser colorChooser = new JColorChooser(color);
      colorChooser.getSelectionModel().addChangeListener(e -> {
         color = colorChooser.getColor();
      });
      controlBox.add(colorChooser);
      // Pen radius label and slider
      Box radiusSliderBox = Box.createHorizontalBox();
      radiusSliderBox.add(Box.createHorizontalGlue());
      radiusSliderBox.add(new JLabel("Pen Radius:"));
      JSlider radiusSlider = new JSlider(1, 100, 5);
      radiusSlider.addChangeListener(new ChangeListener() {
         @Override
         public void stateChanged(ChangeEvent e) {
            radius = radiusFromPercent(radiusSlider.getValue());
         }
      });
      radiusSliderBox.add(radiusSlider);
      radiusSliderBox.add(Box.createHorizontalGlue());
      controlBox.add(radiusSliderBox);
      // Clear Button
      JButton clearBtn = new JButton("Clear!");
      clearBtn.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            clearCanvas();
         }
      });
      controlBox.add(clearBtn);
      controlBox.add(Box.createVerticalGlue());
      // Finalize
      window.pack();
      window.setLocation(400, 200);
      window.setVisible(true);
   }

   private double radiusFromPercent(int value) {
      final double MAX_RADIUS = 0.1;
      return MAX_RADIUS * value / 100;
   }

   private void clearCanvas() {
      DrawEvent de = new DrawEvent(name,
            DrawEvent.DrawEventType.CLEAR);
      de.draw(draw);
      send(de);
   }

   private void send(DrawEvent de) {
      try {
         out.reset();
         out.writeObject(de);
         out.flush();
      } catch (IOException e) {
         throw new RuntimeException(e);
      }
   } // send

   private void handleServerUpdates() {
      try {
         System.out.println("Waiting for updates from server...");
         while (isClientRunning) {
            DrawEvent de = (DrawEvent) in.readObject();
            de.draw(draw);
         }
      } catch (Exception e) {
         throw new RuntimeException(e);
      }
   }

   private static String getRandomName() {
      String[] names = {"Calvin", "Hobbes", "Hillary", "Sally", "Snoopy",
            "Woodstock", "Charlie", "Opus", "Bill", "Nancy", "Jason"};
      int idx = (int) (Math.random() * names.length);
      return names[idx];
   }
   public static void main(String[] args) {
      Scanner scan = new Scanner(System.in);
      System.out.print("IP to connect to? [127.0.0.1] ");
      String ipAddr = scan.nextLine();
      if (ipAddr.isBlank()) {
         ipAddr = "127.0.0.1";
      }
      String name = getRandomName();
      System.out.print("Name? [" + name + "] ");
      String nameInput = scan.nextLine();
      if (!nameInput.isBlank()) {
         name = nameInput;
      }
      NetSketchClient client = new NetSketchClient(ipAddr, name);
      client.handleServerUpdates();
   }

}
