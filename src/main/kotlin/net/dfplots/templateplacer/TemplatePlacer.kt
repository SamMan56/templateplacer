package net.dfplots.templateplacer
import com.mojang.brigadier.builder.LiteralArgumentBuilder.literal
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.client.network.ClientCommandSource
import net.minecraft.text.Text
import net.minecraft.util.math.BlockPos

@Suppress("UNUSED")
object TemplatePlacer: ModInitializer {
    private const val MOD_ID = "templateplacer"
    override fun onInitialize() {
        println("Example mod has been initialized.")

        ClientCommandRegistrationCallback.EVENT.register { dispatcher, registryAccess ->
            dispatcher.register(
                ClientCommandManager.literal("place").executes { context ->
                    val world = context.source.world
                    val player = context.source.player
                    val client = context.source.client
                    client.messageHandler.onGameMessage(Text.literal("Template boop"), false)

                    1
                }
            )
        }
    }
}