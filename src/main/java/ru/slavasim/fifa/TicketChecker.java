package ru.slavasim.fifa;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import org.springframework.web.client.RestTemplate;
import ru.slavasim.fifa.model.AvScore;
import ru.slavasim.fifa.model.Availability;
import ru.slavasim.fifa.model.AvailabilityData;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static java.util.stream.Collectors.*;

public class TicketChecker/* extends TelegramLongPollingBot*/ implements Runnable {
    private static final String avURL = "https://tickets.fifa.com/API/WCachedL1/{language}/Availability/GetAvailability";
    private static final String[] languages = {"en", "ru", "fr", "es", "de"};
    private static final int[] cats = {14, 15, 16, 17, 56};
    private static final String[] catLabels = {"1", "2", "3", "4", "OV"};
    private static final String TELEGRAM_BOT_URL = "https://api.telegram.org/bot";
    private final String botToken;
    private String mainChatId;
    private String proxyAddress;
    private int proxyPort;
    private int[] categories;
    private LocalDateTime lastCachedTime = null;
    private final String USER_AGENT = "Mozilla/5.0";
    private Set<String> lastMatchSet = new HashSet<>();
    private Map<String, Set<Integer>> lastMatchMap = new HashMap<>();
    private static boolean firstTime = true;
    private ConcurrentHashMap<String, Integer[]> preferences = new ConcurrentHashMap();
    private ConcurrentHashMap<String, String> matches = new ConcurrentHashMap();

    private static final Cache<String, String> cache;

    static {
        cache = CacheBuilder.newBuilder()
                .expireAfterWrite(20, TimeUnit.MINUTES)
                .build();
    }

    public TicketChecker(String botToken, String chatId, String proxyAddress, int proxyPort, int[] categories) {
        this.botToken = botToken;
        this.mainChatId = chatId;
        this.proxyAddress = proxyAddress;
        this.proxyPort = proxyPort;
        this.categories = categories;
    }

    public void run() {
        loadPreferences();
        RestTemplate restTemplate = new RestTemplate();
        try {
            for (String language : languages) {
                String url = avURL.replace("{language}", language);
                Availability availability = restTemplate.getForObject(url, Availability.class);
                if (availability.getCached()) {
                    LocalDateTime dateTime = ZonedDateTime.parse(availability.getCachedOn(), DateTimeFormatter.ISO_DATE_TIME).withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
                    if (lastCachedTime == null || dateTime.isAfter(lastCachedTime)) {
                        lastCachedTime = dateTime;
                        processAvailability(availability, language);
                    }
                } else {
                    processAvailability(availability, language);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadPreferences() {
        try {
            RestTemplate restTemplate = new RestTemplate();
            String url = "https://spreadsheets.google.com/feeds/list/1sYEOdKnhfICo9SP9lR2SLuu6WxNxPc88X7JBUhwI4_o/od6/public/values?alt=json";
            Object response = restTemplate.getForObject(url, Object.class);
            List entries = (List) ((Map) ((Map) response).get("feed")).get("entry");
            entries.forEach(m -> {
                Integer[] cats = new Integer[5];
                Map match = (Map) m;
                String mId = (String) ((Map) match.get("gsx$id")).get("$t");
                String mName = (String) ((Map) match.get("gsx$name")).get("$t");
                for (int i = 0; i < 5; i++) {
                    String cat = (String) ((Map) match.get(String.format("gsx$cat%d", i + 1))).get("$t");
                    cats[i] = Integer.parseInt(cat);
                }
                matches.put(mId, mName);
                preferences.put(mId, cats);
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void processAvailability(Availability availability, String language) {
        List<AvailabilityData> freshData = availability.getData();
        List<AvScore> avList = freshData.stream()
                .map(d -> new AvScore(d.p, catNum(d.c), d.a >= 1))
                .filter(as -> as.available)
                .filter(as -> as.category >= 0)
                .filter(as -> Arrays.binarySearch(categories, cats[as.category]) >= 0)
                .peek(as -> as.score = preferences.get(as.matchId)[as.category])
                .filter(as -> as.score > 0)
                .collect(toList());

        Map<String, Set<Integer>> freshMap = avList.stream()
                .collect(groupingBy(as -> as.matchId, mapping(as -> as.category, toSet())));

        Map<String, Set<Integer>> diff = findDifference(freshMap);
        diff = clearDifference(diff);
        fillCache(freshMap);
        lastMatchMap = freshMap;
        if (!diff.isEmpty()) {
            StringBuilder builder = new StringBuilder().append("Появились новые билеты(").append(language.toUpperCase()).append("):");
            diff.entrySet().stream()
                    .sorted((e1, e2) -> {
                        int score1 = e1.getValue().stream().mapToInt(x -> preferences.get(e1.getKey())[x]).max().orElse(0);//sum();
                        int score2 = e2.getValue().stream().mapToInt(x -> preferences.get(e2.getKey())[x]).max().orElse(0);//.sum();
                        if (score1 == score2)
                            return e1.getKey().compareTo(e2.getKey());
                        return score2 - score1;
                    })
                    .forEachOrdered(e -> {
                        String matchId = e.getKey();
                        builder.append("\n").append(matchId).append(":").append(matches.get(matchId)).append(catList(lastMatchMap.get(matchId)));
                    });
            String message = builder.toString();
            sendAlert(mainChatId, message);
            sendAlert("561688131", message);
            sendAlert("617958264", message);
        }

/*        Set<String> freshSet = avList.stream()
                .map(as -> as.matchId)
                .collect(toSet());

        if (!lastMatchSet.containsAll(freshSet)) {
            HashSet<String> diff = new HashSet<>(freshSet);
            diff.removeAll(lastMatchSet);
            lastMatchSet = freshSet;
            if (!diff.isEmpty()) {
                StringBuilder builder = new StringBuilder().append("Появились новые билеты(").append(language.toUpperCase()).append("):");
                for (String match : diff) {
                    builder.append("\n").append(match).append(":").append(matches.get(match));
                }
                String message = builder.toString();
                sendAlert(mainChatId, message);
                if (!firstTime) {
                    sendAlert("561688131", message);
                }
                firstTime = false;
            }
        }
*/

/*

        boolean found = freshData.stream().anyMatch(d -> Arrays.binarySearch(categories, d.c) >= 0 && d.a == 1);
        if (found) {
            sendAlert(mainChatId, message);
            sendAlert("561688131", message);
        }
*/
    }

    private void fillCache(Map<String, Set<Integer>> freshMap) {
        for (Map.Entry<String, Set<Integer>> entry : freshMap.entrySet()) {
            String matchId = entry.getKey();
            Set<Integer> set = entry.getValue();
            for (Integer cat : set) {
                String cacheKey = matchId + ":" + cat;
                cache.put(cacheKey, "1");
            }
        }
    }

    private Map<String, Set<Integer>> clearDifference(Map<String, Set<Integer>> diff) {
        Map<String, Set<Integer>> clearDiff = new HashMap<>();
        for (Map.Entry<String, Set<Integer>> entry : diff.entrySet()) {
            String matchId = entry.getKey();
            Set<Integer> set = entry.getValue();
            Set<Integer> newset = new HashSet<>();
            for (Integer cat : set) {
                String cacheKey = matchId + ":" + cat;
                if (!cache.asMap().containsKey(cacheKey)) {
                    newset.add(cat);
                }
            }
            if (!newset.isEmpty()) {
                clearDiff.put(matchId, newset);
            }
        }
        return clearDiff;
    }

    private Map<String, Set<Integer>> findDifference(Map<String, Set<Integer>> freshMap) {
        Map<String, Set<Integer>> diff = new HashMap<>();
        for (Map.Entry<String, Set<Integer>> entry : freshMap.entrySet()) {
            if (!lastMatchMap.containsKey(entry.getKey())) {
                diff.put(entry.getKey(), entry.getValue());
                continue;
            }
            Set<Integer> nums = new HashSet<>(entry.getValue());
            nums.removeAll(lastMatchMap.get(entry.getKey()));
            if (!nums.isEmpty()) {
                diff.put(entry.getKey(), nums);
            }
        }
        return diff;
    }

    private String catList(Set<Integer> nums) {
        Set<String> ordered = new TreeSet<>(nums.stream().map(n -> catLabels[n]).collect(toSet()));
        String[] strings = ordered.toArray(new String[ordered.size()]);
        return "(" + String.join(",", strings) + ")";
    }

    private int catNum(Integer c) {
        for (int i = 0; i < cats.length; i++) {
            if (cats[i] == c) {
                return i;
            }
        }
        return -1;
    }

    private void sendAlert(String chatId, String message) {
        try {
//            String url = TELEGRAM_BOT_URL + botToken + "/sendMessage?chat_id=" + chatId + "&text=Tickets";
            URI uri = new URIBuilder(TELEGRAM_BOT_URL + botToken + "/sendMessage")
                    .setParameter("chat_id", chatId)
                    .setParameter("text", message)
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
//            System.out.println(chatId + ": " + message);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
