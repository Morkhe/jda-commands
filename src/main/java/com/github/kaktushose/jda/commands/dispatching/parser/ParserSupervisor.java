package com.github.kaktushose.jda.commands.dispatching.parser;

import com.github.kaktushose.jda.commands.dispatching.CommandContext;
import com.github.kaktushose.jda.commands.dispatching.CommandDispatcher;
import com.github.kaktushose.jda.commands.dispatching.parser.impl.DefaultMessageParser;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Registry for {@link Parser Parsers}. This is also the event listener that will call the corresponding parser.
 *
 */
public class ParserSupervisor extends ListenerAdapter {

    private static final Logger log = LoggerFactory.getLogger(ParserSupervisor.class);
    private final CommandDispatcher dispatcher;
    private final Map<Class<? extends GenericEvent>, Parser<? extends GenericEvent>> listeners;

    /**
     * Constructs a new ParserSupervisor.
     *
     * @param dispatcher the calling {@link CommandDispatcher}
     */
    public ParserSupervisor(CommandDispatcher dispatcher) {
        listeners = new HashMap<>();
        this.dispatcher = dispatcher;
        register(MessageReceivedEvent.class, new DefaultMessageParser());
    }

    /**
     * Registers a new {@link Parser} for the given subtype of {@link GenericEvent}.
     *
     * @param listener the subtype of {@link GenericEvent}
     * @param parser the {@link Parser} to register
     */
    public void register(Class<? extends GenericEvent> listener, Parser<? extends GenericEvent> parser) {
        listeners.put(listener, parser);
        log.debug("Registered parser {} for event {}", parser.getClass().getName(), listener.getSimpleName());
    }

    /**
     * Unregisters the {@link Parser} for the given subtype of {@link GenericEvent}.
     *
     * @param listener the subtype of {@link GenericEvent}
     */
    public void unregister(Class<? extends GenericEvent> listener) {
        listeners.remove(listener);
        log.debug("Unregistered parser binding for event {}", listener.getSimpleName());
    }

    /**
     * Distributes {@link GenericEvent GenericEvents} to the corresponding parser. If the parsing didn't fail, will call
     * {@link CommandDispatcher#onEvent(CommandContext)}
     *
     * @param event the {@link GenericEvent GenericEvents} to distribute
     */
    @Override
    public void onGenericEvent(@NotNull GenericEvent event) {
        if (!listeners.containsKey(event.getClass())) {
            return;
        }
        log.debug("Received {}", event.getClass().getSimpleName());
        Parser<?> parser = listeners.get(event.getClass());
        log.debug("Calling {}", parser.getClass().getName());
        CommandContext context = parser.parseInternal(event, dispatcher);

        if (context.isCancelled()) {
            if (context.getErrorMessage() != null) {
                context.getEvent().getChannel().sendMessage(context.getErrorMessage()).queue();
            }
            return;
        }

        dispatcher.onEvent(context);
    }
}
