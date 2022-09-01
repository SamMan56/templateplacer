package net.dfplots.templateplacer
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.client.command.v1.ClientCommandManager
import net.minecraft.block.Block
import net.minecraft.block.Blocks
import net.minecraft.text.Text
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction

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

                // find grass block below player
                val floorPos = player.blockPos.withY(49)

                // find top left corner of codespace
                var topLeftPos = BlockPos(floorPos)
                // this should move onto the border
                limitedWhile({ isBlock(topLeftPos, floorBlock) }) {
                    topLeftPos = topLeftPos.offset(west)
                }
                // then move up off the iron border
                limitedWhile({ isBlock(topLeftPos, borderBlock) }) {
                    topLeftPos = topLeftPos.offset(north)
                }
                // then one back
                topLeftPos = topLeftPos.offset(south)


                // for top right, there is no border to follow, keep moving till find iron again
                var topRightPos = topLeftPos.offset(east)
                limitedWhile({ isBlock(topRightPos, floorBlock) }) {
                    topRightPos = topRightPos.offset(east)
                }

                // there is a border to follow for backs, keep going along then move one forward
                var bottomLeftPos = BlockPos(topLeftPos)
                var bottomRightPos = BlockPos(topRightPos)
                limitedWhile({ isBlock(bottomLeftPos, borderBlock) }) {
                    // shift both equally (its a rectangle)
                    bottomLeftPos = bottomLeftPos.offset(south)
                    bottomRightPos = bottomRightPos.offset(south)
                }

                bottomLeftPos = bottomLeftPos.offset(north)
                bottomRightPos = bottomRightPos.offset(north)

                printToClient(topLeftPos.toString())
                printToClient(topRightPos.toString())
                printToClient(bottomLeftPos.toString())
                printToClient(bottomRightPos.toString())
                printToClient("")

                1
            }
        )
    }
}