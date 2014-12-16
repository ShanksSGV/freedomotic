package com.freedomotic.plugins.devices.jscube.energyathome;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.freedomotic.api.EventTemplate;
import com.freedomotic.api.Protocol;
import com.freedomotic.events.ProtocolRead;
import com.freedomotic.reactions.Command;
import com.freedomotic.things.EnvObjectLogic;
import com.freedomotic.things.ThingRepository;
import com.google.inject.Inject;

public class EnergyAtHome extends Protocol {

    @Inject
    private ThingRepository thingsRepository;

    protected static URL url;
    protected static HttpURLConnection connection;

    protected String flexIP = configuration.getProperty("flexIP");
    private final String protocolName = configuration.getProperty("protocol.name");
    private final int POLLING_TIME = configuration
            .getIntProperty("pollingtime", 1000);

    private static final Logger LOG = Logger.getLogger(EnergyAtHome.class
            .getName());

    public EnergyAtHome() {
        super("Energy@Home",
                "/energyathome/energyathome-manifest.xml");
        setPollingWait(POLLING_TIME);
    }

    @Override
    protected void onStart() {
        if (!getDevices()) { // exit if no devices were found
            setPollingWait(-1);
            super.stop();
        }
    }

    @Override
    protected boolean canExecute(Command arg0) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    protected void onCommand(Command c) {
        if (c.getProperty("code").equals("0")) {
            String body;
            if (c.getProperty("state").equals("ON")) {
                body = "{\"operation\":\"setTrue\"}";
            } else {
                body = "{\"operation\":\"setFalse\"}";
            }
            postToFlex(flexIP + "api/functions/" + c.getProperty("identifier")
                    + ":OnOff", body);
        }
    }

    @Override
    protected void onEvent(EventTemplate e) {

    }

    @Override
    protected void onRun() {
        for (EnvObjectLogic thing : thingsRepository.findAll()) {
            if (thing.getPojo().getProtocol().equals(protocolName)) {
                String address = thing.getPojo().getPhisicalAddress();
                String name = thing.getPojo().getName();
                String body = "{\"operation\":\"getCurrent\"}";
                String line = postToFlex(
                        flexIP + "api/functions/" + address + ":EnergyMeter", body);
                try {
                    JSONObject json = new JSONObject(line);
                    Double value = json.getJSONObject("result").getDouble("level");
                    LOG.log(Level.INFO, "Object {0}is consuming: {1}W", new Object[]{address, value});
                    buildEvent(name, address, "powerUsage", String.valueOf(value), null);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /*
     * getDevices() prende i devices collegati al flexGW e li registra su Fd; se
     * l'oggetto non è presente, in questa fase viene creato.
     */
    protected boolean getDevices() {
        String line = getToFlex(flexIP + "api/devices");
        try {
            JSONArray json = new JSONArray(line);
            for (int i = 0; i < json.length(); i++) {
                String address = json.getJSONObject(i).getString(
                        "dal.device.UID");
                String name = json.getJSONObject(i).getString("component.name");
                String type = getType(address);
                LOG.log(Level.INFO, "...Synchronizing object {0} {1}", new Object[]{type, address});
                if (type.equalsIgnoreCase("OnOff")) {
                    String status = String.valueOf(getStatus(address));
                    buildEvent(name, address, "powered", status, "SmartPlug");
                }
            }
            return true;
        } catch (JSONException e) {
            LOG.log(Level.INFO, "Error in parsing JSON! Plugin will stop!");
            e.printStackTrace();
            return false;
        }

    }

    /*
     * getType(String address) restituisce il "tipo" di device di un certo
     * address; su questa base, verrà creato il relativo oggetto FD (se esiste
     * un adeguato template). La scelta è basata sulla prima function.UID che si
     * trova.
     */
    protected String getType(String address) {
        String line = getToFlex(flexIP + "api/devices/" + address
                + "/functions");
        try {
            JSONArray json = new JSONArray(line);
            String[] temp = json.getJSONObject(0).getString("dal.function.UID")
                    .split(":");
            String type = temp[temp.length - 1];
            return type;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    /*
     * buildEvent(String name, String address, String property, String value,
     * String type) genera un evento per l'oggetto caratterizzato dai parametri
     * passati; se l'oggetto esiste, gli stati vengono sincronizzati altrimenti
     * l'oggetto viene creato.
     */
    protected void buildEvent(String name, String address, String property,
            String value, String type) {
        ProtocolRead event = new ProtocolRead(this, protocolName, address);
        event.addProperty("object.name", name);
        event.addProperty("object.protocol", protocolName);
        event.addProperty("object.address", address);
        event.addProperty("behavior.name", property);
        event.addProperty("behaviorValue", value);
        if (type != null) {
            event.addProperty("object.class", type);
            LOG.log(Level.INFO, event.getPayload().toString());
            notifyEvent(event); // evento per la creazione
        }
        notifyEvent(event); // evento per l'aggiornamento del parametro

    }

    /*
     * getStatus(String address) restituisce lo stato di un dispositivo che
     * possiede la function OnOff
     */
    protected boolean getStatus(String address) {
        boolean status = false;
        String body = "{\"operation\":\"getData\"}";
        String line = postToFlex(
                flexIP + "api/functions/" + address + ":OnOff", body);
        try {
            JSONObject json = new JSONObject(line);
            status = json.getJSONObject("result").getBoolean("value");
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return status;
    }

    /*
     * Funzione generica per fare una GET al FlexGW
     */
    private String getToFlex(String urlToInvoke) {
        String line = null;
        try {
            url = new URL(urlToInvoke.replaceAll(" ", "%20"));
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            BufferedReader read = new BufferedReader(new InputStreamReader(
                    connection.getInputStream()));
            line = read.readLine();
        } catch (MalformedURLException e) {
            LOG.log(Level.INFO,
                    "Malformed URL! Please check IP address in manifest.xml!");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return line;
    }

    /*
     * Funzione generica per fare una POST al FlexGW
     */
    private String postToFlex(String urlToInvoke, String body) {
        try {
            url = new URL(urlToInvoke.replaceAll(" ", "%20"));
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");

            connection.setDoOutput(true);
            connection.setInstanceFollowRedirects(false);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setUseCaches(false);

            DataOutputStream wr = new DataOutputStream(
                    connection.getOutputStream());
            wr.writeBytes(body);
            wr.flush();
            wr.close();

            BufferedReader read = new BufferedReader(new InputStreamReader(
                    connection.getInputStream()));
            String line = read.readLine();
            return line;
        } catch (MalformedURLException e) {
            LOG.log(Level.INFO,
                    "Malformed URL! Please check IP address in manifest.xml!");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

}
