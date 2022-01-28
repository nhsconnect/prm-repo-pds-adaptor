package uk.nhs.prm.deductions.pdsadaptor.testhelpers;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

import java.util.ArrayList;

public class TestLogAppender extends AppenderBase<ILoggingEvent> {
    ArrayList<ILoggingEvent> loggingEvents = new ArrayList<>();

    @Override
    protected void append(ILoggingEvent eventObject) {
        loggingEvents.add(eventObject);
    }

    public ILoggingEvent getLastLoggedEvent() {
        if (loggingEvents.isEmpty()) return null;
        return loggingEvents.get(loggingEvents.size() - 1);
    }
}
