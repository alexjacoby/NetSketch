package ajacoby.netdraw;

import java.net.ServerSocket;
import java.util.List;

/**
 * Server for NetDraw which allows clients to connect and subscribe
 * to updates.
 * <p>
 * For a "real" server, check out kryonet or similar projects.
 * <p>
 * Based on Rueben Dubester's (?) VelocitasMortem project circa 2015.
 *
 * @author alex.jacoby@k12.dc.edu / ajacoby@gmail.com
 * @version June 8, 2022
 */
public class NetDrawServer {
   private List<DrawEvent> events;

   private ServerSocket serverSocket;
}
