/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.nowebmodes.other

import net.ccbluex.liquidbounce.features.module.modules.movement.nowebmodes.NoWebMode
import net.ccbluex.liquidbounce.utils.PacketUtils
import net.ccbluex.liquidbounce.utils.block.BlockUtils
import net.minecraft.init.Blocks.web
import net.minecraft.network.play.client.C07PacketPlayerDigging
import net.minecraft.util.EnumFacing

object Grim : NoWebMode("Grim") {
    override fun onUpdate() {
        val searchBlocks = BlockUtils.searchBlocks(2, setOf(web))
        mc.thePlayer.isInWeb = false
        for (block in searchBlocks){
            val blockpos = block.key
            PacketUtils.sendPacket(C07PacketPlayerDigging(C07PacketPlayerDigging.Action.STOP_DESTROY_BLOCK,blockpos, EnumFacing.DOWN))
        }
    }
}
