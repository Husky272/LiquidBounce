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
package net.ccbluex.liquidbounce.integration.interop.protocol

import com.google.gson.*
import net.ccbluex.liquidbounce.api.ClientApi.formatAvatarUrl
import net.ccbluex.liquidbounce.config.ConfigSystem.registerCommonTypeAdapters
import net.ccbluex.liquidbounce.config.Configurable
import net.ccbluex.liquidbounce.integration.theme.ComponentSerializer
import net.ccbluex.liquidbounce.integration.theme.component.Component
import net.ccbluex.liquidbounce.utils.client.convertToString
import net.ccbluex.liquidbounce.utils.client.isPremium
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.processContent
import net.minecraft.SharedConstants
import net.minecraft.client.network.ServerInfo
import net.minecraft.client.session.Session
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.item.ItemStack
import net.minecraft.registry.DynamicRegistryManager
import net.minecraft.registry.Registries
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.hit.EntityHitResult
import net.minecraft.util.hit.HitResult
import net.minecraft.world.GameMode
import java.lang.reflect.Type
import java.util.*

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class ProtocolExclude

class ProtocolExclusionStrategy : ExclusionStrategy {
    override fun shouldSkipClass(clazz: Class<*>?) = false
    override fun shouldSkipField(field: FieldAttributes) = field.getAnnotation(ProtocolExclude::class.java) != null
}

object ProtocolConfigurableWithComponentSerializer : JsonSerializer<Configurable> {

    override fun serialize(
        src: Configurable,
        typeOfSrc: Type,
        context: JsonSerializationContext
    ): JsonElement {
        if (src is Component) {
            return ComponentSerializer.serialize(src, typeOfSrc, context)
        }

        return JsonObject().apply {
            addProperty("name", src.name)
            add("value", context.serialize(src.inner.filter {
                !it.notAnOption
            }))
            add("valueType", context.serialize(src.valueType))
        }
    }
}

object ProtocolConfigurableSerializer : JsonSerializer<Configurable> {

    override fun serialize(
        src: Configurable,
        typeOfSrc: Type,
        context: JsonSerializationContext
    ) = JsonObject().apply {
        addProperty("name", src.name)
        add("value", context.serialize(src.inner.filter {
            !it.notAnOption
        }))
        add("valueType", context.serialize(src.valueType))
    }
}

class ServerInfoSerializer : JsonSerializer<ServerInfo> {
    override fun serialize(src: ServerInfo?, typeOfSrc: Type?, context: JsonSerializationContext?)
        = src?.asJsonObject()

    fun ServerInfo.asJsonObject() = JsonObject().apply {
        addProperty("name", name)
        addProperty("address", address)
        addProperty("status", status.name)
        add("playerList", protocolGson.toJsonTree(playerListSummary))
        add("label", protocolGson.toJsonTree(label))
        add("playerCountLabel", protocolGson.toJsonTree(playerCountLabel))
        add("version", protocolGson.toJsonTree(version))
        addProperty("protocolVersion", protocolVersion)
        addProperty("protocolVersionMatches", protocolVersion == SharedConstants.getGameVersion().protocolVersion)
        addProperty("ping", ping)
        add("players", JsonObject().apply {
            addProperty("max", players?.max)
            addProperty("online", players?.online)
        })
        addProperty("resourcePackPolicy", ResourcePolicy.fromMinecraftPolicy(resourcePackPolicy).policyName)

        favicon?.let {
            addProperty("icon", Base64.getEncoder().encodeToString(it))
        }
    }

}

enum class ResourcePolicy(val policyName: String) {
    PROMPT("Prompt"),
    ENABLED("Enabled"),
    DISABLED("Disabled");

    fun toMinecraftPolicy() = when (this) {
        PROMPT -> ServerInfo.ResourcePackPolicy.PROMPT
        ENABLED -> ServerInfo.ResourcePackPolicy.ENABLED
        DISABLED -> ServerInfo.ResourcePackPolicy.DISABLED
    }

    companion object {
        fun fromMinecraftPolicy(policy: ServerInfo.ResourcePackPolicy) = when (policy) {
            ServerInfo.ResourcePackPolicy.PROMPT -> PROMPT
            ServerInfo.ResourcePackPolicy.ENABLED -> ENABLED
            ServerInfo.ResourcePackPolicy.DISABLED -> DISABLED
        }

        fun fromString(policy: String) = entries.find { it.policyName == policy }

    }

}

class GameModeSerializer : JsonSerializer<GameMode> {
    override fun serialize(src: GameMode?, typeOfSrc: Type?, context: JsonSerializationContext?)
        = src?.let { JsonPrimitive(it.getName()) }
}

class ItemStackSerializer : JsonSerializer<ItemStack> {
    override fun serialize(src: ItemStack?, typeOfSrc: Type?, context: JsonSerializationContext?)
        = src?.let {
            JsonObject().apply {
                addProperty("identifier", Registries.ITEM.getId(it.item).toString())
                add("displayName", protocolGson.toJsonTree(it.name))
                addProperty("count", it.count)
                addProperty("damage", it.damage)
                addProperty("maxDamage", it.maxDamage)
                addProperty("empty", it.isEmpty)
            }
    }

}

class IdentifierSerializer : JsonSerializer<Identifier> {
    override fun serialize(src: Identifier?, typeOfSrc: Type?, context: JsonSerializationContext?)
        = src?.let { JsonPrimitive(it.toString()) }
}

class StatusEffectInstanceSerializer : JsonSerializer<StatusEffectInstance> {
    override fun serialize(
        src: StatusEffectInstance?,
        typeOfSrc: Type?,
        context: JsonSerializationContext?
    ) = src?.let {
        JsonObject().apply {
            addProperty("effect", Registries.STATUS_EFFECT.getId(it.effectType.value()).toString())
            addProperty("localizedName", it.effectType.value().name.convertToString())
            addProperty("duration", it.duration)
            addProperty("amplifier", it.amplifier)
            addProperty("ambient", it.isAmbient)
            addProperty("infinite", it.isInfinite)
            addProperty("visible", it.shouldShowParticles())
            addProperty("showIcon", it.shouldShowIcon())
            addProperty("color", it.effectType.value().color)
        }
    }

}

class SessionSerializer : JsonSerializer<Session> {
    override fun serialize(src: Session?, typeOfSrc: Type?, context: JsonSerializationContext?)
        = src?.let {
            JsonObject().apply {
                addProperty("username", it.username)
                addProperty("uuid", it.uuidOrNull.toString())
                addProperty("accountType", it.accountType.getName())
                addProperty("avatar", formatAvatarUrl(it.uuidOrNull, it.username))
                addProperty("premium", it.isPremium())
            }
        }
}

class TextSerializer : JsonSerializer<Text> {
    override fun serialize(src: Text?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
        return Text.Serialization.toJson(
            src?.processContent(), mc.world?.registryManager ?: DynamicRegistryManager.EMPTY
        )
    }
}

class HitResultSerializer : JsonSerializer<HitResult> {

    override fun serialize(src: HitResult, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        return JsonObject().apply {
            // Due to Minecraft obfuscation, we have to use a when statement here
            addProperty("type", when (src.type) {
                HitResult.Type.BLOCK -> "block"
                HitResult.Type.ENTITY -> "entity"
                else -> "miss"
            })

            when (src) {
                is BlockHitResult -> {
                    add("pos", context.serialize(src.pos))
                    add("blockPos", context.serialize(src.blockPos))
                    add("side", JsonPrimitive(src.side.getName()))
                    add("isInsideBlock", JsonPrimitive(src.isInsideBlock))
                }
                is EntityHitResult -> {
                    add("pos", context.serialize(src.pos))
                    add("entityName", context.serialize(src.entity.nameForScoreboard))
                    add("entityType", JsonPrimitive(Registries.ENTITY_TYPE.getId(src.entity.type).toString()))
                    add("entityPos", context.serialize(src.entity.pos))
                }
                else -> { }
            }
        }
    }

}


internal val genericProtocolGson = GsonBuilder()
    .addSerializationExclusionStrategy(ProtocolExclusionStrategy())
    .registerCommonTypeAdapters()
    .registerTypeHierarchyAdapter(Configurable::class.javaObjectType, ProtocolConfigurableSerializer)
    .create()

internal val protocolGson = GsonBuilder()
    .addSerializationExclusionStrategy(ProtocolExclusionStrategy())
    .registerCommonTypeAdapters()
    .registerTypeHierarchyAdapter(Configurable::class.javaObjectType, ProtocolConfigurableWithComponentSerializer)
    .registerTypeHierarchyAdapter(Text::class.java, TextSerializer())
    .registerTypeAdapter(Session::class.java, SessionSerializer())
    .registerTypeAdapter(ServerInfo::class.java, ServerInfoSerializer())
    .registerTypeAdapter(GameMode::class.java, GameModeSerializer())
    .registerTypeAdapter(ItemStack::class.java, ItemStackSerializer())
    .registerTypeAdapter(Identifier::class.java, IdentifierSerializer())
    .registerTypeAdapter(StatusEffectInstance::class.java, StatusEffectInstanceSerializer())
    .registerTypeHierarchyAdapter(HitResult::class.java, HitResultSerializer())
    .create()

