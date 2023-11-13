package sws.io;

import java.io.IOException;
import java.nio.channels.Pipe;
import java.nio.channels.Selector;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedDeque;
import sws.utils.Flag;

public class EventLoopController {
    private final ConcurrentLinkedDeque<IOEvent> ioEventQueue = new ConcurrentLinkedDeque<>();
    private final ArrayList<Flag> eventLoopFlags;
    private final ArrayList<Selector> selectors;

    public EventLoopController(int poolSize) {
        this.eventLoopFlags = new ArrayList<>(poolSize);
        this.selectors = new ArrayList<>(poolSize);
    }

    public void pushEvent(IOEvent ioEvent) {
        ioEventQueue.add(ioEvent);
        for (var selector : selectors) {
            selector.wakeup();
        }
    }

    public EventLoop createEventLoop() throws IOException {
        var flag = new Flag();
        var wakeupChannel = Pipe.open();
        wakeupChannel.sink().configureBlocking(false);
        eventLoopFlags.add(flag);
        var eventLoop = new EventLoop(flag, this, ioEventQueue);
        selectors.add(eventLoop.getSelector());
        return eventLoop;
    }
}
