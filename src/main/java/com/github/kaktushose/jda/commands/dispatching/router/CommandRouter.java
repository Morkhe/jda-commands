package com.github.kaktushose.jda.commands.dispatching.router;

import com.github.kaktushose.jda.commands.dispatching.CommandContext;
import com.github.kaktushose.jda.commands.reflect.CommandDefinition;
import net.dv8tion.jda.api.MessageBuilder;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class CommandRouter implements Router {

    @Override
    public void findCommands(CommandContext context, Collection<CommandDefinition> commands) {
        CommandDefinition command = null;
        String[] input = context.getInput();

        AtomicInteger matchingLength = new AtomicInteger(0);
        for (int i = input.length - 1; i > -1; i--) {

            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < i + 1; j++) {
                sb.append(input[j]).append(" ");
            }
            String generatedLabel = sb.toString().trim();
            List<CommandDefinition> possibleCommands = commands.stream().filter(cmd -> cmd.getLabels().stream().anyMatch(label -> {
                        String[] expectedLabels = label.split(" ");
                        String[] actualLabels = generatedLabel.split(" ");
                        if (expectedLabels.length != actualLabels.length) {
                            return false;
                        }

                        boolean matches = true;
                        for (int k = 0; k < expectedLabels.length; k++) {
                            if (!matches) {
                                return false;
                            }

                            if (context.getSettings().isIgnoreCase()) {
                                matches = expectedLabels[k].toLowerCase().startsWith(actualLabels[k].toLowerCase());
                            } else {
                                matches = expectedLabels[k].startsWith(actualLabels[k]);
                            }

                            if (matches) {
                                matchingLength.set(actualLabels.length);
                            }
                        }
                        return matches;
                    })
            ).collect(Collectors.toList());

            if (possibleCommands.size() == 1) {
                command = possibleCommands.get(0);
                break;
            }

            if (possibleCommands.size() > 1) {
                context.setCancelled(true);
                break;
            }
        }

        if (command == null || context.isCancelled()) {
            context.setErrorMessage(new MessageBuilder().append("no command found").build());
            context.setCancelled(true);
        }

        context.setInput(Arrays.copyOfRange(input, matchingLength.get(), input.length));
        context.setCommand(command);
    }
}
