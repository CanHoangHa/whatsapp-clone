package com.hoangha.whatsappclone.user;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.*;

@Service
public class UserPresenceService {
    private static final long OFFLINE_DELAY_SECONDS = 5;

    private final ScheduledExecutorService scheduler =
            Executors.newScheduledThreadPool(1);

    private final Map<String, ScheduledFuture<?>> offlineTasks = new ConcurrentHashMap<>();

    public void scheduleOffline(
            String userId,
            Runnable offlineAction
    ) {
        cancelOffline(userId);

        ScheduledFuture<?> task = scheduler.schedule(
                offlineAction,
                OFFLINE_DELAY_SECONDS,
                TimeUnit.SECONDS
        );

        offlineTasks.put(userId, task);
    }

    public void cancelOffline(String userId) {
        ScheduledFuture<?> task = offlineTasks.remove(userId);
        if (task != null) task.cancel(false);
    }
}
