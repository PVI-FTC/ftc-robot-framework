package pvi.harness.bridge;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import pvi.harness.runtime.JsonReader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Stage U2, harness side: the real socket. A test client connects over 127.0.0.1 the way Unity
 * will, sends message lines and reads one reply line for each.
 */
class BridgeServerTest {

    /**
     * WHAT: Over a real local connection, each message line gets one reply line, in order, and
     * "bye" closes the connection.
     *
     * <p>HOW: The server listens on a free port (chosen by the computer) and serves one client on a
     * background thread. The test client sends hello, load, init and bye.</p>
     *
     * <p>WHY: The session logic is tested on its own; this checks the part Unity actually talks to.</p>
     */
    @Test
    @SuppressWarnings("unchecked")
    void eachLineGetsOneReplyOverARealConnection() throws Exception {
        try (ServerSocket server = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            Thread serving = new Thread(() -> {
                try {
                    BridgeServer.serveOneClient(server);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }, "bridge-test-server");
            serving.start();

            List<String> replies = new ArrayList<>();
            try (Socket client = new Socket(InetAddress.getLoopbackAddress(), server.getLocalPort())) {
                client.setSoTimeout(20_000);
                Writer out = new OutputStreamWriter(client.getOutputStream(), StandardCharsets.UTF_8);
                BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8));
                for (String line : List.of("{\"type\": \"hello\", \"version\": 1}",
                        "{\"type\": \"load\", \"opMode\": \"teamA-teleop\"}", "{\"type\": \"init\"}", "{\"type\": \"bye\"}")) {
                    out.write(line + "\n");
                    out.flush();
                    replies.add(in.readLine());
                }
                assertEquals(null, in.readLine(), "the server closes the connection after bye");
            }
            serving.join(20_000);

            List<Object> types = new ArrayList<>();
            for (String reply : replies) {
                types.add(((Map<String, Object>) JsonReader.parse(reply)).get("type"));
            }
            assertEquals(List.of("welcome", "loaded", "frame", "goodbye"), types);
            assertTrue(replies.get(2).contains("\"stage\": \"INIT\""), replies.get(2));
        }
    }
}
