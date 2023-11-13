package sws.io;

import java.io.IOException;
import java.nio.channels.Selector;
import java.util.concurrent.ConcurrentLinkedDeque;
import sws.utils.Flag;

public class EventLoop implements Runnable {
    private final Flag flag;
    private final Selector selector;
    private final EventLoopController eventLoopController;
    private final ConcurrentLinkedDeque<IOEvent> ioEventQueue;

    public EventLoop(Flag flag, EventLoopController eventLoopController,
            ConcurrentLinkedDeque<IOEvent> ioEventQueue) throws IOException {
        this.flag = flag;
        this.selector = Selector.open();
        this.ioEventQueue = ioEventQueue;
        this.eventLoopController = eventLoopController;
    }

    @Override
    public void run() {
        while (flag.isSet()) {
            try {
                selector.select();

                IOEvent ioEvent;
                while ((ioEvent = ioEventQueue.poll()) != null) {
                    ioEvent.channel.register(selector, ioEvent.ops, ioEvent.opContext);
                }

                final var iter = selector.selectedKeys().iterator();
                while (iter.hasNext()) {
                    var key = iter.next();
                    if (!key.isValid()) {
                        iter.remove();
                        continue;
                    }

                    var opContext = (OperationContext) key.attachment();
                    if (opContext != null) {
                        opContext.handler.handle(eventLoopController, selector, key, opContext);
                    }
                    iter.remove();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public Selector getSelector() {
        return selector;
    }
}
