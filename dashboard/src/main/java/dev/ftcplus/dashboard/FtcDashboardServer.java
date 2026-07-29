package dev.ftcplus.dashboard;

import dev.ftcplus.core.Component;
import dev.ftcplus.core.DashboardAttachable;
import dev.ftcplus.core.DashboardListener;
import dev.ftcplus.core.HardwareDevice;
import dev.ftcplus.core.Robot;
import dev.ftcplus.core.Runtime;
import dev.ftcplus.core.StreamingLog;
import dev.ftcplus.core.signal.Signal;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoWSD;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public final class FtcDashboardServer extends NanoWSD
        implements DashboardListener, DashboardAttachable {

    private static final int PORT = 7273;
    private static final Logger log = Logger.getLogger("FtcDashboard");

    private final Set<NanoWSD.WebSocket> clients =
        Collections.newSetFromMap(new ConcurrentHashMap<>());

    private Robot<?, ?, ?> robot;
    private Runtime runtime;

    public FtcDashboardServer() {
        super(PORT);
    }


    @Override
    public void attach(Robot<?, ?, ?> robot, Runtime runtime) {
        this.robot   = robot;
        this.runtime = runtime;
        startServer();
    }


    @Override
    public void onUpdate() {
        if (clients.isEmpty()) return;
        if (robot != null && robot.powerBudget() != null) {
            try {
                broadcast(DashboardSerializer.serializePower(robot.powerBudget(), robot).toString());
            } catch (Exception ignored) {}
        }
    }

    @Override
    public void onSignal(Signal signal) {
        try {
            broadcast(DashboardSerializer.serializeSignal(
                    signal.getClass().getSimpleName(), null
            ).toString());
        } catch (Exception ignored) {}
    }


    private void startServer() {
        try {
            start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
            log.info("FTC+ Dashboard started at http://ftc.local:" + PORT);
        } catch (IOException e) {
            log.severe("Failed to start dashboard: " + e.getMessage());
        }
    }

    public void stopServer() {
        stop();
    }

    @Override
    protected NanoWSD.WebSocket openWebSocket(NanoHTTPD.IHTTPSession handshake) {
        return new DashboardSocket(handshake);
    }

    public void broadcast(String message) {
        for (NanoWSD.WebSocket client : clients) {
            try {
                client.send(message);
            } catch (IOException e) {
                // client disconnected
            }
        }
    }


    private final class DashboardSocket extends NanoWSD.WebSocket {

        DashboardSocket(NanoHTTPD.IHTTPSession handshake) {
            super(handshake);
        }

        @Override
        protected void onOpen() {
            clients.add(this);
            log.info("Dashboard client connected.");

            if (robot != null) {
                try {
                    send(DashboardSerializer.serializeTree(robot).toString());
                } catch (IOException | JSONException e) {
                    log.warning("Failed to send initial tree: " + e.getMessage());
                }
            }
        }

        @Override
        protected void onClose(NanoWSD.WebSocketFrame.CloseCode code,
                               String reason, boolean initiatedByRemote) {
            clients.remove(this);
            log.info("Dashboard client disconnected.");
        }

        @Override
        protected void onMessage(NanoWSD.WebSocketFrame message) {
            try {
                JSONObject msg = new JSONObject(message.getTextPayload());
                String type = msg.getString("type");

                switch (type) {
                    case "SET_SETTING":
                        handleSetSetting(msg);
                        break;
                    case "RUN_DIAGNOSTIC":
                        // TODO: trigger diagnostic by path
                        break;
                    case "RUN_CALIBRATION":
                        // TODO: trigger calibration by path
                        break;
                }
            } catch (Exception e) {
                log.warning("Failed to handle message: " + e.getMessage());
            }
        }

        @Override protected void onPong(NanoWSD.WebSocketFrame pong) {}

        @Override
        protected void onException(IOException e) {
            clients.remove(this);
        }

        private void handleSetSetting(JSONObject msg) throws JSONException {
            String className = msg.getString("class");
            String fieldName = msg.getString("field");
            Object value     = msg.get("value");
            walkAndSetSetting(robot, className, fieldName, value);
        }

        private void walkAndSetSetting(Component component, String className,
                                        String fieldName, Object value) {
            if (component.getClass().getName().equals(className)) {
                try {
                    java.lang.reflect.Field field =
                        component.getClass().getDeclaredField(fieldName);
                    field.trySetAccessible();

                    if (field.getType() == int.class)
                        field.setInt(component, ((Number) value).intValue());
                    else if (field.getType() == double.class)
                        field.setDouble(component, ((Number) value).doubleValue());
                    else if (field.getType() == float.class)
                        field.setFloat(component, ((Number) value).floatValue());
                    else if (field.getType() == boolean.class)
                        field.setBoolean(component, (Boolean) value);
                    else
                        field.set(component, value);

                    StreamingLog.setting(className, fieldName, value);

                } catch (Exception e) {
                    log.warning("Failed to set " + className + "." + fieldName + ": " + e.getMessage());
                }
            }

            for (Component child : component.children()) {
                walkAndSetSetting(child, className, fieldName, value);
            }
        }
    }
}
