package net.dfplots.templateplacer
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.client.command.v1.ClientCommandManager
import net.minecraft.block.Block
import net.minecraft.block.Blocks
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen
import net.minecraft.inventory.Inventory
import net.minecraft.item.Items
import net.minecraft.nbt.NbtCompound
import net.minecraft.screen.slot.SlotActionType
import net.minecraft.text.Text
import net.minecraft.util.Hand
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.math.Vec3d
import kotlin.math.absoluteValue
import kotlin.math.sign

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
                    justDo {
                        player.sendChatMessage(message)
                    }
                }

                fun StepByStep.setVelocityUntil(vec3d: Vec3d, pred: () -> Boolean) {
                    work {
                        player.velocity = vec3d

                        Result.SUCCESS
                    }
                    work {
                        if (pred()) {
                            player.velocity = Vec3d(0.0, 0.0, 0.0)
                            Result.SUCCESS
                        }
                        else {
                            player.velocity = vec3d
                            Result.KEEP_TRYING
                        }
                    }
                }

                fun StepByStep.moveTo(to: Vec3d) {
                    work {
                        if ((player.x - to.x).absoluteValue > 8) {
                            player.setPosition(player.x + ((to.x - player.x).sign * 10), player.y, player.z)
                            Result.KEEP_TRYING
                        } else Result.SUCCESS
                    }
                    work {
                        if ((player.y - to.y).absoluteValue > 8) {
                            player.setPosition(player.x, player.y + ((to.y - player.y).sign * 10), player.z)
                            Result.KEEP_TRYING
                        } else Result.SUCCESS
                    }
                    work {
                        if ((player.z - to.z).absoluteValue > 8) {
                            player.setPosition(player.x, player.y, player.z + ((to.z - player.z).sign * 10))
                            Result.KEEP_TRYING
                        } else Result.SUCCESS
                    }
                    justDo {
                        player.setPosition(to)
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

                    justDo {
                        player.inventory.selectedSlot = 0
                    }

                    // get high enough
                    moveTo(Vec3d(player.x, 51.9, player.z))
                    justDo {
                        player.abilities.flying = true
                        player.sendAbilitiesUpdate()
                    }

                    setVelocityUntil(Vec3d(-1.0, 0.0, 0.0)) { player.velocity.x == 0.0 }
                    setVelocityUntil(Vec3d(0.0, 0.0, -1.0)) { player.velocity.z == 0.0 }

                    justDo {
                        player.pitch = 90.0F
                    }

                    steps {
                        var routePos = Vec3d
                            .of(player.blockPos.offset(north, 3))
                            .withAxis(Direction.Axis.Y, 51.9)

                        val startPos = Vec3d(routePos.x, routePos.y, routePos.z)
                        var i = 0

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
                                    moveTo(Vec3d(routePos.x, player.y, player.z))
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

                                    routePos = routePos.withAxis(Direction.Axis.X, routePos.x + 3)
                                    i++
                                    Result.SUCCESS
                                }
                                steps {
                                    if (i == 6) {
                                        send("/plot codespace add -c")

                                        moveTo(Vec3d(startPos.x, player.y, player.z))

                                        steps {
                                            routePos = routePos.withAxis(Direction.Axis.Y, routePos.y + 5)
                                            moveTo(Vec3d(player.x, routePos.y, player.z))
                                            i = 0
                                            routePos = routePos.withAxis(Direction.Axis.X, startPos.x)
                                        }
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