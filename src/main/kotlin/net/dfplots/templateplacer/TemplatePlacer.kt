package net.dfplots.templateplacer
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.client.command.v1.ClientCommandManager
import net.minecraft.block.Block
import net.minecraft.block.Blocks
import net.minecraft.text.Text
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.math.Vec3d

@Suppress("UNUSED")
object TemplatePlacer: ModInitializer {
    private const val MOD_ID = "templateplacer"
    override fun onInitialize() {
        println("Example mod has been initialized.")

        ClientCommandManager.DISPATCHER.register(
            ClientCommandManager.literal("place").executes { context ->
                val world = context.source.world
                val player = context.source.player
                val client = context.source.client

                fun printToClient(message: String) {
                    client.inGameHud.chatHud.addMessage(Text.of(message))
                }

                fun isBlock(blockPos: BlockPos, block: Block) =
                    world.getBlockState(blockPos).block == block

                fun limitedWhile(p: () -> Boolean, f: () -> Unit) {
                    var i = 0
                    while (p() && i < 1000) {
                        f()
                        i++
                    }
                }

                // TODO: add a check if they holding iron ingot

                val borderBlock = Blocks.STONE
                val floorBlock = Blocks.GRASS_BLOCK

                // where north points towards the plot
                val north = Direction.EAST
                val east = Direction.SOUTH
                val south = Direction.WEST
                val west = Direction.NORTH

                stepByStep {
                    tryTo { // initial burst of speed
                        player.velocity = Vec3d(0.0, 0.0, -1.0)

                        Result.SUCCESS
                    }
                    tryTo {
                        if (player.velocity.z > -0.2) { // if they hit a wall
                            printToClient("Couldn't navigate back to the codespace root!")
                            return@tryTo Result.FAIL
                        }

                        player.velocity = Vec3d(0.0, 0.0, -1.0)

                        // find grass block below player
                        val floorPos = player.blockPos.withY(49)

                        if (isBlock(floorPos, borderBlock))
                            Result.SUCCESS
                        else Result.KEEP_TRYING
                    }
                }

                1
            }
        )
    }
}