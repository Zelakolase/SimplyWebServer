package sws.io;

import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

public class OperationContext {
    public Handler handler;
    public Object attachment;

    public OperationContext(Handler handler, Object attachment) {
        this.handler = handler;
        this.attachment = attachment;
    }

    public interface Handler {
        public void handle(EventLoopController eventLoopController, Selector selector,
                SelectionKey key, OperationContext operationContext);
    }
}
