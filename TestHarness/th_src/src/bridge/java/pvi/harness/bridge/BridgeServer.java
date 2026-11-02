package pvi.harness.bridge;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * The harness side of the live Unity connection: listens on 127.0.0.1 (this computer only),
 * accepts one Unity client at a time, and passes each message line to a {@link BridgeSession},
 * writing back its reply. Start it from {@code Harness/}:
 *
 * <pre>
 *   .\gradlew.bat unityBridge                (port 7465)
 *   .\gradlew.bat unityBridge -Pport=7500
 * </pre>
 *
 * <p>Stop it with Ctrl+C. When a client disconnects, it waits for the next one. Nothing here
 * runs on its own: the OpMode only moves when a message asks it to.</p>
 */
public final class BridgeServer {
    /** Only the main method and helpers here; nothing to create. */
    private BridgeServer() {
    }

    /** Listens on the port in {@code args[0]} (or 7465) and serves clients one after another, forever. */
    public static void main(String[] args) throws IOException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : BridgeProtocol.DEFAULT_PORT;
        try (ServerSocket server = new ServerSocket(port, 1, InetAddress.getLoopbackAddress())) {
            System.out.println("Harness bridge (version " + BridgeProtocol.VERSION + ") listening on 127.0.0.1:"
                    + server.getLocalPort() + ". Start Unity's live mode, or press Ctrl+C to stop.");
            while (true) {
                serveOneClient(server);
            }
        }
    }

    /**
     * Waits for one client, talks to it until it says "bye", fails the handshake or disconnects,
     * then returns. Each client gets a fresh {@link BridgeSession}.
     */
    public static void serveOneClient(ServerSocket server) throws IOException {
        try (Socket client = server.accept()) {
            client.setTcpNoDelay(true); // send each small reply at once, not batched
            System.out.println("Client connected from " + client.getRemoteSocketAddress());
            BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8));
            Writer out = new OutputStreamWriter(client.getOutputStream(), StandardCharsets.UTF_8);
            BridgeSession session = new BridgeSession();
            String line;
            while ((line = in.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                out.write(session.handle(line));
                out.write('\n');
                out.flush();
                if (session.isClosing()) {
                    break;
                }
            }
            System.out.println("Client disconnected.");
            session.close();
        } catch (IOException e) {
            // A client that vanishes mid-run is not a reason to stop serving others.
            System.out.println("Client connection ended: " + e.getMessage());
        }
    }
}
