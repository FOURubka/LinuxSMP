package com.rubka.linuxsmp;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;

public final class Text {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();
    private static final MiniMessage MM = MiniMessage.miniMessage();

    /** Gradient brand prefix used on all chat feedback. */
    public static final String PREFIX = "<gradient:#7f5af0:#2cb67d><bold>LinuxSMP</bold></gradient> <dark_gray>»</dark_gray> ";

    private Text() {
    }

    /** Legacy '&' colour codes -> Component (italic off, so lore/menus look clean). */
    public static Component c(String legacy) {
        return LEGACY.deserialize(legacy).decoration(TextDecoration.ITALIC, false);
    }

    /** MiniMessage (tags, gradients, hex) -> Component (italic off). */
    public static Component mm(String mini) {
        return MM.deserialize(mini).decoration(TextDecoration.ITALIC, false);
    }

    public static Component prefix() {
        return mm(PREFIX);
    }

    /** Send a prefixed message; body accepts legacy '&' codes. */
    public static void msg(CommandSender target, String legacyBody) {
        target.sendMessage(prefix().append(c(legacyBody)));
    }
}
