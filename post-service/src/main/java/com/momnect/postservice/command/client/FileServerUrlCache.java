package com.momnect.postservice.command.client;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

@Component
@RequiredArgsConstructor
public class FileServerUrlCache {
    private final FileServiceClient client;
    private final AtomicReference<String> cached = new AtomicReference<>(null);
    private volatile Instant lastFetch = Instant.EPOCH;
    private final Duration ttl = Duration.ofMinutes(10);

    public String get() {
        if (cached.get() == null || Instant.now().isAfter(lastFetch.plus(ttl))) {
            String base = client.getServerUrl();
            if (base.endsWith("/")) base = base.substring(0, base.length()-1);
            cached.set(base);
            lastFetch = Instant.now();
        }
        return cached.get();
    }
}
