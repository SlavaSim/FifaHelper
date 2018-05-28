package ru.slavasim.fifa;

import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Application {
    public static void main(String[] args) throws ExecutionException, InterruptedException {
//        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("spring/spring-app.xml");
        String botToken = args[0];
        String chatId = args[1];
        String proxyAddress = args[2];
        int proxyPort = Integer.parseInt(args[3]);
        String[] scat = args[4].split("~");
        int[] categories = Arrays.stream(scat)
                .mapToInt(Integer::parseInt)
                .toArray();

        TicketChecker checker = new TicketChecker(botToken, chatId, proxyAddress, proxyPort, categories);
//        checker.run();
        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleAtFixedRate(checker, 0, 10, TimeUnit.SECONDS);
    }
}
