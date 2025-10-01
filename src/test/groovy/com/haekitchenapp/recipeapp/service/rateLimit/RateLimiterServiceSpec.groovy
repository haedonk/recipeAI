package com.haekitchenapp.recipeapp.service.rateLimit

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import spock.lang.Specification

import java.time.Instant
import java.util.concurrent.ConcurrentLinkedDeque

class RateLimiterServiceSpec extends Specification {

    def "denies request when per-minute threshold exceeded"() {
        given: "a service whose minute limit has already been reached"
        def service = new RateLimiterService()
        service.metaClass.setProperty(service, "maxPerMinute", 3)
        service.metaClass.setProperty(service, "maxPerHour", 10)
        def now = Instant.now().epochSecond
        def deque = new ConcurrentLinkedDeque<Long>()
        3.times { deque.addLast(now - 10) }
        service.@userRequests.put("minute-user", deque)

        when: "the user attempts another request"
        def allowed = service.isAllowed("minute-user", service.@maxPerMinute, service.@maxPerHour)

        then: "it is rejected because the per-minute quota is exhausted"
        !allowed
        service.@userRequests.get("minute-user").size() == 3
    }

    def "denies request when per-hour threshold exceeded while per-minute usage is low"() {
        given: "a service with mostly older requests that still count for the hourly limit"
        def service = new RateLimiterService()
        service.metaClass.setProperty(service, "maxPerMinute", 10)
        service.metaClass.setProperty(service, "maxPerHour", 4)
        def now = Instant.now().epochSecond
        def deque = new ConcurrentLinkedDeque<Long>()
        4.times { deque.addLast(now - 120) }
        service.@userRequests.put("hour-user", deque)

        when: "the user attempts another request"
        def allowed = service.isAllowed("hour-user", service.@maxPerMinute, service.@maxPerHour)

        then: "it is rejected because the hourly quota is exhausted even though the minute quota is not"
        !allowed
        service.@userRequests.get("hour-user").every { now - it >= 60 }
    }

    def "decreaseRateCount prunes newest entries logs adjustment and ignores unknown users"() {
        given: "a service with recent requests stored for a user"
        def service = new RateLimiterService()
        def userKey = "known-user"
        def now = Instant.now().epochSecond
        def deque = new ConcurrentLinkedDeque<Long>()
        [now - 50, now - 40, now - 30].each { deque.addLast(it) }
        service.@userRequests.put(userKey, deque)

        // Set up logging capture with proper level
        Logger logger = (Logger) org.slf4j.LoggerFactory.getLogger(RateLimiterService)
        def originalLevel = logger.level
        logger.setLevel(ch.qos.logback.classic.Level.DEBUG)
        def appender = new ListAppender<ILoggingEvent>()
        appender.start()
        logger.addAppender(appender)

        when: "reducing the request count for that user"
        service.decreaseRateCount(userKey, 2)

        then: "only the oldest entry remains and the adjustment is logged"
        service.@userRequests.get(userKey).size() == 1
        service.@userRequests.get(userKey).peekFirst() == now - 50
        appender.list.any { it.formattedMessage.contains("Decreased rate count for ${userKey}") }

        when: "another user with no history is reduced"
        def loggedBefore = appender.list.size()
        service.decreaseRateCount("unknown-user", 3)

        then: "no exception is thrown and nothing new is logged"
        appender.list.size() == loggedBefore
        !service.@userRequests.containsKey("unknown-user")

        cleanup:
        logger.detachAppender(appender)
        logger.setLevel(originalLevel)  // Restore original log level
    }
}
