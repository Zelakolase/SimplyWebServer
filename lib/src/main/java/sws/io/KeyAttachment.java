package sws.io;

import java.nio.channels.Channel;
import java.nio.channels.Selector;
import java.util.function.Function;

public class KeyAttachment {
    private final Channel channel;
    private final Selector selector;
    private Function<HandlerArgs, Void> handler;
    private Object attachment;

    public KeyAttachment(Channel channel, Selector selector, Function<HandlerArgs, Void> handler) {
        this.channel = channel;
        this.handler = handler;
        this.selector = selector;
    }

    public void handle() {
        handler.apply(new HandlerArgs(selector, channel, this));
    }

    public void attach(Object attachment) {
        this.attachment = attachment;
    }

    public Object attachment() {
        return this.attachment;
    }

    public void setHandler(Function<HandlerArgs, Void> handler) {
        this.handler = handler;
    }

    public static class HandlerArgs {
        public Selector selector;
        public Channel channel;
        public KeyAttachment keyAttachment;

        public HandlerArgs(Selector selector, Channel channel, KeyAttachment keyAttachment) {
            this.selector = selector;
            this.channel = channel;
            this.keyAttachment = keyAttachment;
        }
    }
}
