package net.dfplots.templateplacer
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.client.command.v1.ClientCommandManager
import net.minecraft.block.Block
import net.minecraft.block.Blocks
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen
import net.minecraft.inventory.Inventory
import net.minecraft.item.Item
import net.minecraft.item.Items
import net.minecraft.screen.slot.SlotActionType
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

                fun Inventory.toList() = List(size(), this::getStack)

                // TODO: add a check if they holding iron ingot

                val borderBlock = Blocks.STONE
                val floorBlock = Blocks.GRASS_BLOCK

                // where north points towards the plot
                val north = Direction.EAST
                val east = Direction.SOUTH
                val south = Direction.WEST
                val west = Direction.NORTH

                fun TaskSet.waitForInvToOpen(): Unit {
                    tryTo {
                        if (client.currentScreen == null)
                            Result.KEEP_TRYING
                        else Result.SUCCESS
                    }
                }

                fun TaskSet.tryWithScreen(f: (GenericContainerScreen) -> Result) {
                    tryTo {
                        val currentScreen = client.currentScreen
                        if (currentScreen != null && currentScreen is GenericContainerScreen)
                            f(currentScreen)
                        else Result.FAIL
                    }
                }

                fun TaskSet.clickAndWait(slotId: Int) {
                    tryWithScreen { genericContainerScreen ->
                        if (genericContainerScreen.screenHandler.inventory.getStack(slotId).item == Items.AIR)
                            return@tryWithScreen Result.FAIL

                        client.interactionManager?.clickSlot(
                            genericContainerScreen.screenHandler.syncId,
                            slotId,
                            0,
                            SlotActionType.PICKUP,
                            player
                        )
                        Result.SUCCESS
                    }
                    tryWithScreen { genericContainerScreen ->
                        if (player.currentScreenHandler.cursorStack.item != Items.AIR)
                            Result.KEEP_TRYING // wait until server puts an item there
                        else Result.SUCCESS
                    }
                }

                fun TaskSet.clickToClose(slotId: Int) {
                    tryWithScreen { genericContainerScreen ->
                        printToClient(genericContainerScreen.screenHandler.inventory.getStack(slotId).name.asString())
                        if (genericContainerScreen.screenHandler.inventory.getStack(slotId).item == Items.AIR)
                            return@tryWithScreen Result.FAIL

                        client.interactionManager?.clickSlot(
                            genericContainerScreen.screenHandler.syncId,
                            slotId,
                            0,
                            SlotActionType.PICKUP,
                            player
                        )
                        Result.SUCCESS
                    }
                    tryTo {
                        printToClient("e")
                        if (client.currentScreen != null) Result.KEEP_TRYING
                        else Result.SUCCESS
                    }
                }

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
                    tryTo { // stop the player flying out of bounds
                        player.velocity = Vec3d(0.0, 0.0, 0.0)

                        Result.SUCCESS
                    }
                    tryTo {
                        player.sendChatMessage("/plot clear")

                        Result.SUCCESS
                    }
                    addTasks { waitForInvToOpen() }

                    // configure clearing settings to plot
                    addTasks { clickAndWait(9) }
                    addTasks { clickAndWait(14) }
                    addTasks { clickAndWait(15) }
                    addTasks { clickAndWait(16) }
                    addTasks { clickAndWait(44) }

                    // press clear
                    addTasks { clickToClose(11) }

                    addTasks {
                        printToClient("1")

                        tryTo {
                            printToClient("2")
                            Result.SUCCESS
                        }

                        tryTo {
                            printToClient("3")
                            Result.SUCCESS
                        }

                        Result.SUCCESS
                    }

                    tryTo {
                        printToClient("2")

                        Result.SUCCESS
                    }
                }

                1
            }
        )
    }
}