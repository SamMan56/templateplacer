package net.dfplots.templateplacer
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.client.command.v1.ClientCommandManager
import net.minecraft.block.Block
import net.minecraft.block.Blocks
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.nbt.NbtCompound
import net.minecraft.screen.slot.SlotActionType
import net.minecraft.text.Text
import net.minecraft.util.Hand
import net.minecraft.util.hit.BlockHitResult
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
                var interactionManager = client.interactionManager

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

                fun StepByStep.waitForInvToOpen() {
                    work {
                        if (client.currentScreen == null)
                            Result.KEEP_TRYING
                        else Result.SUCCESS
                    }
                }

                fun StepByStep.tryWithScreen(f: (GenericContainerScreen) -> Result) {
                    work {
                        val currentScreen = client.currentScreen
                        if (currentScreen != null && currentScreen is GenericContainerScreen)
                            f(currentScreen)
                        else Result.FAIL
                    }
                }

                fun StepByStep.clickAndWait(slotId: Int) {
                    tryWithScreen { genericContainerScreen ->
                        if (genericContainerScreen.screenHandler.inventory.getStack(slotId).item == Items.AIR)
                            return@tryWithScreen Result.KEEP_TRYING

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

                fun StepByStep.clickToClose(slotId: Int) {
                    tryWithScreen { genericContainerScreen ->
                        printToClient(genericContainerScreen.screenHandler.inventory.getStack(slotId).name.asString())
                        if (genericContainerScreen.screenHandler.inventory.getStack(slotId).item == Items.AIR)
                            return@tryWithScreen Result.KEEP_TRYING

                        client.interactionManager?.clickSlot(
                            genericContainerScreen.screenHandler.syncId,
                            slotId,
                            0,
                            SlotActionType.PICKUP,
                            player
                        )
                        Result.SUCCESS
                    }
                    work {
                        printToClient("e")
                        if (client.currentScreen != null) Result.KEEP_TRYING
                        else Result.SUCCESS
                    }
                }

                fun StepByStep.send(message: String) {
                    work {
                        player.sendChatMessage(message)

                        Result.SUCCESS
                    }
                }

                StepByStep {
                    send("/plot spawn")
                    send("/plot clear")
                    waitForInvToOpen()

                    // configure clearing settings to plot
                    clickAndWait(9)
                    clickAndWait(14)
                    clickAndWait(15)
                    clickAndWait(16)
                    clickAndWait(44)

                    // press clear
                    clickToClose(11)

                    work {
                        player.inventory.selectedSlot = 0

                        Result.SUCCESS
                    }

                    // get high enough
                    work {
                        if (player.pos.y < 51.9) {
                            player.velocity = Vec3d(0.0, 0.1, 0.0)
                            Result.KEEP_TRYING
                        }
                        else Result.SUCCESS
                    }
                    work {
                        player.velocity = Vec3d(0.0, 0.0, 0.0)
                        player.abilities.flying = true
                        player.sendAbilitiesUpdate()

                        Result.SUCCESS
                    }

                    work { // initial burst of speed
                        player.velocity = Vec3d(-1.0, 0.0, 0.0)

                        Result.SUCCESS
                    }
                    work {
                        if (player.velocity.x > -0.2) { // if they hit the edge
                            Result.SUCCESS
                        } else {
                            player.velocity = Vec3d(-1.0, 0.0, 0.0)
                            Result.KEEP_TRYING
                        }
                    }
                    work { // initial burst of speed
                        player.velocity = Vec3d(0.0, 0.0, -1.0)

                        Result.SUCCESS
                    }
                    work {
                        if (player.velocity.z > -0.2) { // if they hit the edge
                            Result.SUCCESS
                        } else {
                            player.velocity = Vec3d(0.0, 0.0, -1.0)
                            Result.KEEP_TRYING
                        }
                    }
                    work {
                        player.pitch = 90.0F
                        Result.SUCCESS
                    }

                    steps {
                        var placePos = player.blockPos.offset(north, 3).withY(50)

                        player.inventory
                            .toList()
                            .filter { itemStack ->
                                val nbt = itemStack.nbt
                                val publicBukkitValues = nbt?.get("PublicBukkitValues") as? NbtCompound
                                val codetemplatedata = publicBukkitValues?.get("hypercube:codetemplatedata")

                                codetemplatedata != null
                            }
                            .forEach { codeItemStack ->
                                steps {
                                    work {
                                        if (player.blockPos.x >= placePos.x) {
                                            Result.SUCCESS
                                        } else {
                                            player.velocity = Vec3d(1.0, 0.0, 0.0)
                                            Result.KEEP_TRYING
                                        }
                                    }
                                    work {
                                        client.setScreen(CreativeInventoryScreen(player))
                                        player.inventory.setStack(player.inventory.selectedSlot, codeItemStack)
                                        player.playerScreenHandler.sendContentUpdates()
                                        player.closeScreen()

                                        if (client.crosshairTarget !is BlockHitResult)
                                            return@work Result.FAIL

                                        val blockHitResult = client.crosshairTarget as BlockHitResult

                                        interactionManager?.interactBlock(
                                            player,
                                            client.world,
                                            Hand.MAIN_HAND,
                                            blockHitResult
                                        )

                                        placePos = placePos.offset(north, 3)

                                        Result.SUCCESS
                                    }
                                }
                            }
                    }
                }.start()

                1
            }
        )
    }
}