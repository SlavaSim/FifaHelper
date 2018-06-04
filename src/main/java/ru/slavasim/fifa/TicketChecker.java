package ru.slavasim.fifa;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import org.springframework.web.client.RestTemplate;
import ru.slavasim.fifa.model.Availability;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

public class TicketChecker/* extends TelegramLongPollingBot*/ implements Runnable {
    public static final String avURL = "https://tickets.fifa.com/API/WCachedL1/en/Availability/GetAvailability";
    public static final String TELEGRAM_BOT_URL = "https://api.telegram.org/bot";
    private final String botToken;
    private String mainChatId;
    private String proxyAddress;
    private int proxyPort;
    private int[] categories;
    private LocalDateTime lastCachedTime = null;
    private final String USER_AGENT = "Mozilla/5.0";

    public TicketChecker(String botToken, String chatId, String proxyAddress, int proxyPort, int[] categories) {
        this.botToken = botToken;
        this.mainChatId = chatId;
        this.proxyAddress = proxyAddress;
        this.proxyPort = proxyPort;
        this.categories = categories;
    }

    public void run() {
        RestTemplate restTemplate = new RestTemplate();
        Availability availability = restTemplate.getForObject(avURL, Availability.class);
        if (availability.getCached()) {
            LocalDateTime dateTime = ZonedDateTime.parse(availability.getCachedOn(), DateTimeFormatter.ISO_DATE_TIME).withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
            if (lastCachedTime == null || dateTime.isAfter(lastCachedTime)) {
                lastCachedTime = dateTime;
                processAvailability(availability);
            }
        } else {
            processAvailability(availability);
        }
    }

    private void processAvailability(Availability availability) {
        boolean found = availability.getData().stream().anyMatch(d -> Arrays.binarySearch(categories, d.c) >= 0 && d.a == 1);
        if (found) {
            sendAlert(mainChatId);
            sendAlert("561688131");
        }
    }

    private void sendAlert(String chatId) {
        try {
//            String url = TELEGRAM_BOT_URL + botToken + "/sendMessage?chat_id=" + chatId + "&text=Tickets";
            URI uri = new URIBuilder(TELEGRAM_BOT_URL + botToken + "/sendMessage")
                    .setParameter("chat_id", chatId)
                    .setParameter("text", "Есть билеты!")
                    .build();

            HttpHost proxy = new HttpHost(proxyAddress, proxyPort);
            DefaultProxyRoutePlanner routePlanner = new DefaultProxyRoutePlanner(proxy);
            CloseableHttpClient httpClient = HttpClients.custom()
                    .setRoutePlanner(routePlanner)
                    .build();
            HttpGet request = new HttpGet(uri);

            // add request header
            request.addHeader("User-Agent", USER_AGENT);

            HttpResponse response = httpClient.execute(request);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
