package ajacoby.netsketch;

import ajacoby.stdlib.Draw;
import ajacoby.stdlib.DrawListener;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * Allows multiple clients to simultaneously draw to a single Draw
 * canvas.
 * <p>
 * TODO: Add (graphical) list of currently connected clients.
 */
public class NetSketchServer {
   /**
    * Thread for listening for updates from one client.
    */
   private class NetSketchServerThread extends Thread {
      private Socket socket;
      private String clientName;
      private ObjectInputStream in;
      private ObjectOutputStream out;
      private boolean continueThread = true;

      private NetSketchServerThread(Socket socket) {
         this.socket = socket;
         System.out.println("New client connection from " + socket.getInetAddress());
         try {
            System.out.println("NetSketchServerThread: attempting to create streams");
            in = new ObjectInputStream(socket.getInputStream());
            out = new ObjectOutputStream(socket.getOutputStream());
            String connectStr = (String) in.readObject();
            clientName = connectStr.substring(NetSketchClient.CONNECT_PREFIX.length());
            System.out.println("Connected to " + clientName + "\n");
         } catch (Exception e) {
            e.printStackTrace();
         }
      } // NetSketchServerThread()

      @Override
      public void run() {
         try {
            shareCanvas();
            System.out.println("NetSketchServerThread: Listening for messages...");
            while (continueThread && isServerAlive) {
               DrawEvent de = (DrawEvent) in.readObject();
               synchronized (drawEvents) {
                  if (de.getType().equals(DrawEvent.DrawEventType.CLEAR)) {
                     drawEvents.clear();
                  } else {
                     drawEvents.add(de);
                  }
               }
               de.draw(draw);
               broadcast(de);
            }
         } catch (Exception e) {
            System.err.println("Exception from client: " + clientName);
            e.printStackTrace();
         }
         removeClient(this);
         try {
            socket.close();
         } catch (IOException e) {
            System.err.println("Unable to close socket: " + e.getMessage());
         }
      } // run

      /**
       * Sends all DrawEvents to a new client - to be called
       * immediately after a new client connects.
       */
      private void shareCanvas() throws IOException {
         System.out.println("Sending current state of canvas: " + drawEvents.size() + " event(s)");
         out.reset();
         synchronized (drawEvents) {
            for (DrawEvent de : drawEvents) {
               out.writeObject(de);
            }
         }
         out.flush();
         System.out.println("done sending canvas\n");
      } // shareCanvas

      public void send(DrawEvent de) {
         try {
            out.reset();
            out.writeObject(de);
            out.flush();
         } catch (IOException e) {
            System.err.println("Exception from client: " + clientName);
            e.printStackTrace();
            continueThread = false; // bail on fail!
            removeClient(this);
         }
      } // send
   } // NetSketchServerThread class

   public static final int PORT = 63414;
   private List<DrawEvent> drawEvents = new ArrayList<>();
   private List<NetSketchServerThread> threads = new ArrayList<>();
   /** Window with draw canvas and controls. */
   private JFrame window;
   /** Draw object works like a canvas embedded in our JFrame window. */
   private Draw draw = new Draw("NetSketchServer");
   /** Flag for threads to know when to shut down. */
   private volatile boolean isServerAlive = true;

   public NetSketchServer() {
      initWindow();
      boolean testing = false;
      if (testing) {
         DrawEvent de = new DrawEvent("server", new Point2D.Double(0.5, 0.5),
               null, Color.GREEN, 0.005,
               DrawEvent.DrawEventType.POINT);
         de.draw(draw);
         drawEvents.add(de);
      }
      try (ServerSocket serverSocket = new ServerSocket(PORT);) {
         System.out.println("Server details:");
         System.out.println("Port: " + serverSocket.getLocalPort());
         System.out.println("InetAddress: " + serverSocket.getInetAddress());
         System.out.println("Local socket Address: " + serverSocket.getLocalSocketAddress());
         System.out.println("Server: listening");
         // Wait for new connections
         while (true) {
            NetSketchServerThread thread = new NetSketchServerThread(serverSocket.accept());
            synchronized (threads) {
               threads.add(thread);
            }
            thread.start();
         }
      } catch (IOException ioe) {
         ioe.printStackTrace();
      }
   }

   private void initWindow() {
      window = new JFrame("NetSketch Server");
      window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      window.setLayout(new BoxLayout(window.getContentPane(), BoxLayout.LINE_AXIS));
      // Add the draw canvas
      window.add(draw.getJLabel());
      // Add the controls to the right
      Box controlBox = Box.createVerticalBox();
      window.add(controlBox);
      controlBox.add(Box.createVerticalGlue());
      controlBox.add(Box.createVerticalGlue());
      // Finalize
      window.pack();
      window.setVisible(true);
   }

   private void broadcast(DrawEvent de) {
      synchronized (threads) {
         threads.forEach(thread -> thread.send(de));
      }
   }

   private void removeClient(NetSketchServerThread thread) {
      synchronized (threads) {
         threads.remove(thread);
      }
   }

   public static void main(String[] args) {
      NetSketchServer server = new NetSketchServer();
   }
}
