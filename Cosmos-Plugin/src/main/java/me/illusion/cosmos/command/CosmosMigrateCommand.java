package me.illusion.cosmos.command;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import me.illusion.cosmos.CosmosPlugin;
import me.illusion.cosmos.database.CosmosDataContainer;
import me.illusion.cosmos.utilities.command.SimpleCommand;
import me.illusion.cosmos.utilities.text.Placeholder;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

public class CosmosMigrateCommand implements SimpleCommand {



    private final CosmosPlugin plugin;

    public CosmosMigrateCommand(CosmosPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "cosmos.migrate.*.*.*";
    }

    @Override
    public String getPermission() {
        return "cosmos.migrate";
    }

    @Override
    public void execute(CommandSender sender, String... args) {

        String templateName = args[0];
        String source = args[1];
        String destination = args[2];

        if (templateName.isEmpty()) {
            plugin.getMessages().sendMessage(sender, "migrate.invalid-template-arg");
            return;
        }

        if (source.isEmpty()) {
            plugin.getMessages().sendMessage(sender, "migrate.invalid-source-arg");
            return;
        }

        if (destination.isEmpty()) {
            plugin.getMessages().sendMessage(sender, "migrate.invalid-destination-arg");
            return;
        }

//        List<Placeholder<?>> placeholders = Arrays.asList(
//                new Placeholder<>("%template%", templateName),
//                new Placeholder<>("%source%", source),
//                new Placeholder<>("%destination%", destination)
//        );

        Placeholder<CommandSender> sourcePlaceholder = new Placeholder<>("%source%", source);
        Placeholder<CommandSender> destinationPlaceholder = new Placeholder<>("%destination%", destination);
        Placeholder<CommandSender> templatePlaceholder = new Placeholder<>("%template%", templateName);

        CosmosDataContainer sourceContainer = plugin.getContainerRegistry().getContainer(source);
        CosmosDataContainer destinationContainer = plugin.getContainerRegistry().getContainer(destination);

        if (sourceContainer == null) {
            plugin.getMessages().sendMessage(
                    sender,
                    "migrate.invalid-source",
                    sourcePlaceholder
                    );
            return;
        }

        if (destinationContainer == null) {
            plugin.getMessages().sendMessage(
                    sender,
                    "migrate.invalid-destination",
                    destinationPlaceholder
            );
            return;
        }

        if (templateName.equalsIgnoreCase("all")) {
            sourceContainer.fetchAllTemplates().thenAccept(allTemplates -> {
                List<CompletableFuture<?>> futures = new ArrayList<>();

                for (String fetchedTemplateName : allTemplates) {
                    futures.add(sourceContainer.fetchTemplate(fetchedTemplateName)
                        .thenCompose(template -> destinationContainer.saveTemplate(fetchedTemplateName, template)));
                }

                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).thenRun(() -> {
                    plugin.getMessages().sendMessage(
                            sender,
                            "migrate.success",
                            sourcePlaceholder,
                            destinationPlaceholder
                    );
                });
            });

            plugin.getMessages().sendMessage(
                    sender,
                    "migrate.started",
                    sourcePlaceholder,
                    destinationPlaceholder
            );
            return;
        }

        sourceContainer.fetchTemplate(templateName).thenCompose(template -> {
            if (template == null) {
                plugin.getMessages().sendMessage(sender,
                        "migrate.invalid-template",
                        templatePlaceholder
                );
                return CompletableFuture.completedFuture(null);
            }

            return destinationContainer.saveTemplate(templateName, template).thenRun(() -> plugin.getMessages().sendMessage(
                    sender,
                    "migrate.success",
                    sourcePlaceholder,
                    destinationPlaceholder));
        });

        plugin.getMessages().sendMessage(
                sender,
                "migrate.started",
                sourcePlaceholder,
                destinationPlaceholder
        );
    }
}
