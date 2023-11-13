package sws.io;

import java.nio.channels.SelectableChannel;

public class IOEvent {
    public final SelectableChannel channel;

    public final int ops;

    public final OperationContext opContext;

    public IOEvent(SelectableChannel channel, int ops, OperationContext attachment) {
        this.channel = channel;
        this.ops = ops;
        this.opContext = attachment;
    }
}
