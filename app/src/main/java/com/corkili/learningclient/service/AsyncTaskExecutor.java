package com.corkili.learningclient.service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AsyncTaskExecutor {

    private static ExecutorService executorService = Executors.newFixedThreadPool(16);

    public static void execute(Runnable task) {
        executorService.execute(task);
    }

}
