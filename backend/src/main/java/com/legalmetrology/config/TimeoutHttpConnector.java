package com.legalmetrology.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Every outbound {@link org.springframework.web.reactive.function.client.WebClient}
 * to an external AI/storage provider must have a bounded timeout — Reactor
 * Netty's default {@link HttpClient} has none, so a stalled connection to a
 * provider (observed live: a hung call to Gemini that outlasted a 2-minute
 * client wait with no response) blocks the request indefinitely instead of
 * failing fast into the retry/fallback paths this codebase already has
 * (OCR provider fallback, graceful pipeline warnings). One shared factory
 * so every provider's {@code WebClient.Builder} gets the same treatment.
 */
public final class TimeoutHttpConnector {

    private TimeoutHttpConnector() {
    }

    public static ClientHttpConnector of(Duration timeout) {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) timeout.toMillis())
                .responseTimeout(timeout)
                .doOnConnected(conn -> conn.addHandlerLast(new ReadTimeoutHandler(timeout.toSeconds(), TimeUnit.SECONDS)));
        return new ReactorClientHttpConnector(httpClient);
    }
}
