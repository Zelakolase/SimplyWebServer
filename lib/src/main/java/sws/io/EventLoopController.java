package sws.io;

import java.io.IOException;
import java.nio.channels.Selector;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedDeque;
import sws.utils.Flag;

public class EventLoopController {
    private final ConcurrentLinkedDeque<IOEvent> ioEventQueue = new ConcurrentLinkedDeque<>();
    private final ArrayList<Flag> eventLoopFlags;
    private final ArrayList<Selector> selectors;
    private final Random random = new Random();

    public EventLoopController(int poolSize) {
        this.eventLoopFlags = new ArrayList<>(poolSize);
        this.selectors = new ArrayList<>(poolSize);
    }

    public void pushEvent(IOEvent ioEvent) {
        ioEventQueue.add(ioEvent);
        selectors.get(random.nextInt(selectors.size())).wakeup();
    }

    public EventLoop createEventLoop() throws IOException {
        var flag = new Flag();
        eventLoopFlags.add(flag);
        var eventLoop = new EventLoop(flag, this, ioEventQueue);
        selectors.add(eventLoop.getSelector());
        return eventLoop;
    }
}
