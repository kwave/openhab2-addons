package org.openhab.binding.kostal.inverter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.State;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebscrapeHandler extends BaseThingHandler {
    private static Logger logger = LoggerFactory.getLogger(WebscrapeHandler.class);
    private boolean active;
    private SourceConfig config;
    private List<ChannelConfig> channelConfigs;

    public WebscrapeHandler(Thing thing) {
        super(thing);
        channelConfigs = new ArrayList<>();
        channelConfigs.add(new ChannelConfig("acPower", "td", 4));
        channelConfigs.add(new ChannelConfig("totalEnergy", "td", 7));
        channelConfigs.add(new ChannelConfig("dayEnergy", "td", 10));
        channelConfigs.add(new ChannelConfig("status", "td", 13));
    }

    @Override
    public void initialize() {
        config = getConfigAs(SourceConfig.class);
        updateStatus(ThingStatus.ONLINE);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(new Runnable() {
            @Override
            public void run() {
                while (active) {
                    try {
                        refresh();
                    } catch (Exception e) {
                        logger.warn("Error refreshing source " + getThing().getUID(), e);
                    }
                }
            }

        });
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        // Read only
    }

    @Override
    public void preDispose() {
        super.preDispose();
        active = false;
    }

    private void refresh() throws Exception {
        Document doc = getDoc();
        for (ChannelConfig cConfig : channelConfigs) {
            String value = getTag(doc, cConfig.tag).get(cConfig.num);
            Channel channel = getThing().getChannel(cConfig.id);
            State state = getState(value);
            updateState(channel.getUID(), state);
        }
        Thread.sleep(5000);
    }

    private static List<String> getTag(Document doc, String tag) {
        ArrayList<String> result = new ArrayList<String>();
        Iterator<Element> elIt = doc.getElementsByTag(tag).iterator();
        while (elIt.hasNext()) {
            String content = elIt.next().text();
            content = content.replace("\u00A0", "").trim();
            if (!content.isEmpty()) {
                result.add(content);
            }
        }
        return result;
    }

    private Document getDoc() throws IOException {
        String login = config.userName + ":" + config.password;
        String base64login = new String(Base64.getEncoder().encode(login.getBytes()));
        Document doc = Jsoup.connect(config.url).header("Authorization", "Basic " + base64login).get();
        return doc;
    }

    private State getState(String value) {
        try {
            return new DecimalType(Integer.parseInt(value));
        } catch (NumberFormatException e) {
            return new StringType(value);
        }
    }

}
