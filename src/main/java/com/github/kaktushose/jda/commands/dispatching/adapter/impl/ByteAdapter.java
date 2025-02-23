package com.github.kaktushose.jda.commands.dispatching.adapter.impl;

import com.github.kaktushose.jda.commands.dispatching.CommandContext;
import com.github.kaktushose.jda.commands.dispatching.adapter.TypeAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * Type adapter for byte values.
 *
 * @author Kaktushose
 * @version 2.0.0
 * @since 2.0.0
 */
public class ByteAdapter implements TypeAdapter<Byte> {

    /**
     * Attempts to parse a String to a Byte.
     *
     * @param raw     the String to parse
     * @param context the {@link CommandContext}
     * @return the parsed Byte or an empty Optional if the parsing fails
     */
    @Override
    public Optional<Byte> parse(@NotNull String raw, @NotNull CommandContext context) {
        try {
            return Optional.of(Byte.valueOf(raw));
        } catch (NumberFormatException ignored) {
            return Optional.empty();
        }
    }
}
