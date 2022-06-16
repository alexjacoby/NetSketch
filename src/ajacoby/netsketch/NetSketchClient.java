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
 * Demo of and experiment with basic networking and threads.
 *
 * @author A. Jacoby (June 2022)
 */
public class NetSketchClient {
   private Socket clientSocket;
   private ObjectOutputStream out;
   private ObjectInputStream in;
   private Draw win;
   private JFrame controlWin;
   /** Each client currently gets a random color. */
   private Color color = Color.getHSBColor((float) Math.random(), 1f, 1f);
   /** Pen radius for this client. */
   private double radius = 0.005;
   /** Last mouse coordinate for drag operations. */
   private Point2D lastPoint;
   /** Flag to shut down. */
   private boolean isClientRunning = true;

   public NetSketchClient(String host) {
      System.out.println("NetSketchClient connecting to " + host + ":" + NetSketchServer.PORT);
      // Network setup
      try {
         clientSocket = new Socket(host, NetSketchServer.PORT);
         System.out.println("Connected!");
         out = new ObjectOutputStream(clientSocket.getOutputStream());
         in = new ObjectInputStream(clientSocket.getInputStream());
      } catch (UnknownHostException e) {
         throw new RuntimeException(e);
      } catch (IOException e) {
         throw new RuntimeException(e);
      }
      // Window setup
      win = new Draw("NetSketchClient");
      win.addListener(new DrawListener() {
         @Override public void mouseDragged(double x, double y) {
            Point2D pt2 = new Point2D.Double(x, y);
            if (lastPoint != null) {
               DrawEvent de = new DrawEvent("client name",
                     lastPoint, pt2, color, radius,
                     DrawEvent.DrawEventType.LINE);
               de.draw(win);
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
            DrawEvent de = new DrawEvent("client name",
                  pt1, pt2, color, radius,
                  DrawEvent.DrawEventType.POINT);
            de.draw(win);
            send(de);
         }

         @Override
         public void windowClosed() {
            isClientRunning = false;
            // Previous line doesn't really work since thread is usually in
            // blocking call to in.readObject()!
            System.exit(0);
         }
      });
      initControlWin();
   } // NetSketchClient()

   private void initControlWin() {
      controlWin = new JFrame("NetSketch Controls");
      controlWin.getContentPane().setLayout(new BoxLayout(controlWin.getContentPane(), BoxLayout.PAGE_AXIS));
      JColorChooser colorChooser = new JColorChooser(color);
      colorChooser.getSelectionModel().addChangeListener(e -> {
         color = colorChooser.getColor();
      });
      controlWin.add(colorChooser);
      JButton clearBtn = new JButton("Clear!");
      clearBtn.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            clearCanvas();
         }
      });
      // Pen radius label and slider
      Box radiusSliderBox = new Box(BoxLayout.X_AXIS);
      radiusSliderBox.add(new JLabel("Pen Radius:"));
      JSlider radiusSlider = new JSlider(1, 100, 5);
      radiusSlider.addChangeListener(new ChangeListener() {
         @Override
         public void stateChanged(ChangeEvent e) {
            radius = radiusFromPercent(radiusSlider.getValue());
         }
      });
      radiusSliderBox.add(radiusSlider);
      controlWin.add(radiusSliderBox);
      // Clear Button
      controlWin.add(clearBtn);
      // Finalize
      controlWin.pack();
      controlWin.setVisible(true);
   }

   private double radiusFromPercent(int value) {
      final double MAX_RADIUS = 0.1;
      return MAX_RADIUS * value / 100;
   }

   private void clearCanvas() {
      DrawEvent de = new DrawEvent("client name",
            DrawEvent.DrawEventType.CLEAR);
      de.draw(win);
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
            de.draw(win);
         }
      } catch (Exception e) {
         throw new RuntimeException(e);
      }
   }

   public static void main(String[] args) {
      Scanner scan = new Scanner(System.in);
      System.out.print("IP to connect to? [127.0.0.1] ");
      String ipAddr = scan.nextLine();
      if (ipAddr.isBlank()) {
         ipAddr = "127.0.0.1";
      }
      NetSketchClient client = new NetSketchClient(ipAddr);
      client.handleServerUpdates();
   }

}
