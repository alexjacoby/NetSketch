package ajacoby.netsketch;

import ajacoby.stdlib.Draw;
import ajacoby.stdlib.DrawListener;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Scanner;

/**
 * Connects to NetSketchServer to allow user to draw on shared canvas.
 * <p>
 * Demo of and experiment with basic networking, threads, and Swing components.
 *
 * @author A. Jacoby (June 2022)
 */
public class NetSketchClient implements Runnable {
   public static final String CONNECT_PREFIX = "NetSketchClient connect: ";
   private static int numClients = 0;

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
      numClients++;
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
      window = new JFrame("NetSketch Client: " + name);
      window.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
      window.addWindowListener(new WindowAdapter() {
         @Override
         public void windowClosed(WindowEvent e) {
            super.windowClosed(e);
            numClients--;
            if (numClients == 0) {
               System.exit(0);
            }
         }
      });
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
      int offset = 30 * numClients;
      window.setLocation(200 + offset, 200 + offset);
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

   public void run() {
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

   /** Creates a new thread drawing random points. */
   public void stressTest(final long maxPause) {
      Thread t = new Thread() {
         public void run() {
            try {
               System.out.println("Stress testing...");
               while (isClientRunning) {
                  Point2D.Double pt1 = new Point2D.Double(Math.random(), Math.random());
                  DrawEvent de = new DrawEvent(name, pt1, null, color, radius, DrawEvent.DrawEventType.POINT);
                  de.draw(draw);
                  send(de);
                  Thread.sleep((long) (Math.random() * maxPause));
               }
            } catch (Exception e) {
               throw new RuntimeException(e);
            }
         }
      };
      t.start();
   }

   private static String getRandomName() {
      String[] names = {"Calvin", "Hobbes", "Hillary", "Sally", "Snoopy",
            "Woodstock", "Charlie", "Opus", "Bill", "Nancy", "Jason"};
      int idx = (int) (Math.random() * names.length);
      return names[idx];
   }

   /** Creates a new client and runs it in its own thread. */
   public static NetSketchClient buildClient(String ipAddr) {
      String name = getRandomName();
      Scanner scan = new Scanner(System.in);
      System.out.print("Name? [" + name + "] ");
      String nameInput = scan.nextLine();
      if (!nameInput.isBlank()) {
         name = nameInput;
      }
      NetSketchClient client = new NetSketchClient(ipAddr, name);
      Thread clientThread = new Thread(client);
      clientThread.start();
      return client;
   }

   /**
    * Create a pool of clients to stress test the system: trying to
    * uncover threading issues.
    */
   public static void stressTest(String ipAddr, long maxPause) {
      Scanner scan = new Scanner(System.in);
      System.out.print("Number of testers? [3] ");
      String numTestersResp = scan.nextLine();
      int numTesters = (numTestersResp.isBlank())? 3 : Integer.parseInt(numTestersResp);
      ArrayList<NetSketchClient> clients = new ArrayList<>();
      for (int i = 0; i < numTesters; i++) {
         NetSketchClient client = buildClient(ipAddr);
         clients.add(client);
         client.stressTest(maxPause);
      }
   }

   public static void main(String[] args) {
      Scanner scan = new Scanner(System.in);
      System.out.print("IP to connect to? [127.0.0.1] ");
      String ipAddr = scan.nextLine();
      ipAddr = ipAddr.isBlank()? "127.0.0.1" : ipAddr;
      System.out.print("Stress test? [Y/n] ");
      String stressResp = scan.nextLine().toLowerCase();
      boolean doStressTest = stressResp.isBlank()? true : stressResp.startsWith("y");
      if (doStressTest) {
         stressTest(ipAddr, 50);
      } else {
         buildClient(ipAddr);
      }
   }

}
