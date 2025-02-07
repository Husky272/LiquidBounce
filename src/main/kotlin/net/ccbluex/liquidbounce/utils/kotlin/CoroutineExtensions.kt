/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */
@file:Suppress("NOTHING_TO_INLINE")

package net.ccbluex.liquidbounce.utils.kotlin

import kotlinx.coroutines.*
import net.ccbluex.liquidbounce.utils.client.mc
import java.util.concurrent.Executors
import kotlin.coroutines.CoroutineContext
import kotlin.reflect.KProperty

private val loomExecutor = Executors.newVirtualThreadPerTaskExecutor()

private val loomDispatcher = loomExecutor.asCoroutineDispatcher()

val Dispatchers.Loom: CoroutineDispatcher
    get() = loomDispatcher

private val loomBuilder = Thread.ofVirtual()

fun virtualThread(
    name: String = "Loom-${System.currentTimeMillis() % 1000}",
    start: Boolean = true,
    block: Runnable,
): Thread = with(loomBuilder) {
    name(name)

    if (start) start(block) else unstarted(block)
}

private object RenderDispatcher : CoroutineDispatcher() {
    override fun dispatch(context: CoroutineContext, block: Runnable) {
        mc.renderTaskQueue.add(block)
    }
}

val Dispatchers.Render: CoroutineDispatcher
    get() = RenderDispatcher

inline operator fun <T> ThreadLocal<T>.getValue(receiver: Any?, property: KProperty<*>): T = get()

inline operator fun <T> ThreadLocal<T>.setValue(receiver: Any?, property: KProperty<*>, value: T) = set(value)
