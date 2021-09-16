package com.github.kaktushose.jda.commands.dispatching.parser;

import com.github.kaktushose.jda.commands.dispatching.CommandContext;
import com.github.kaktushose.jda.commands.settings.GuildSettings;
import net.dv8tion.jda.api.events.GenericEvent;

public abstract class Parser<T extends GenericEvent> {

    CommandContext parseInternal(GenericEvent event, GuildSettings settings) {
        return parse((T) event, settings);
    }

    public abstract CommandContext parse(T event, GuildSettings settings);

}
