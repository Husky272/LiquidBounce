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
 *
 */
package net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.client

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import io.netty.handler.codec.http.FullHttpResponse
import net.ccbluex.liquidbounce.integration.theme.ThemeManager
import net.ccbluex.liquidbounce.render.FontCache
import net.ccbluex.netty.http.model.RequestObject
import net.ccbluex.netty.http.util.httpOk

// GET /api/v1/client/theme
@Suppress("UNUSED_PARAMETER")
fun getThemeInfo(requestObject: RequestObject): FullHttpResponse = httpOk(JsonObject().apply {
    addProperty("theme", ThemeManager.activeTheme.name)
    addProperty("wallpaper", ThemeManager.activeWallpaper?.name)
})

// GET /api/v1/client/fonts
@Suppress("UNUSED_PARAMETER")
fun getFonts(requestObject: RequestObject): FullHttpResponse = httpOk(JsonArray().apply {
    FontCache.fontCache
        .filter { (_, holder) -> holder.isLoaded }
        .forEach { (name, _) ->
            add(name)
        }
})
