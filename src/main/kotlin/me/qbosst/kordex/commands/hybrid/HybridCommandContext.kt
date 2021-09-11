package me.qbosst.kordex.commands.hybrid

import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.CommandContext
import com.kotlindiscord.kord.extensions.commands.application.message.MessageCommandContext
import com.kotlindiscord.kord.extensions.commands.application.slash.PublicSlashCommandContext
import com.kotlindiscord.kord.extensions.commands.chat.ChatCommandContext
import com.kotlindiscord.kord.extensions.components.ComponentContainer
import com.kotlindiscord.kord.extensions.pagination.builders.PaginatorBuilder
import com.kotlindiscord.kord.extensions.utils.getKoin
import dev.kord.core.Kord
import dev.kord.core.behavior.GuildBehavior
import dev.kord.core.behavior.MemberBehavior
import dev.kord.core.behavior.UserBehavior
import dev.kord.core.behavior.channel.MessageChannelBehavior
import dev.kord.core.cache.data.MessageData
import dev.kord.core.entity.Message
import dev.kord.core.event.Event
import dev.kord.core.event.message.MessageCreateEvent
import me.qbosst.kordex.commands.hybrid.builder.EphemeralHybridMessageCreateBuilder
import me.qbosst.kordex.commands.hybrid.builder.HybridMessageModifyBuilder
import me.qbosst.kordex.commands.hybrid.builder.PublicHybridMessageCreateBuilder
import me.qbosst.kordex.commands.hybrid.entity.EphemeralHybridMessage
import me.qbosst.kordex.commands.hybrid.entity.PublicHybridMessage
import me.qbosst.kordex.pagination.HybridButtonPaginator

@JvmInline
value class HybridCommandContext<T : Arguments>(val context: CommandContext) {

    val kord: Kord get() = context.eventObj.kord
    val eventObj: Event get() = context.eventObj

    val channel: MessageChannelBehavior
        get() = when (context) {
            is PublicSlashCommandContext<*> -> context.channel
            is ChatCommandContext<*> -> context.channel
            else -> error("Unknown context type provided.")
        }

    suspend fun getGuild(): GuildBehavior? {
        return when (context) {
            is PublicSlashCommandContext<*> -> context.getGuild()
            is ChatCommandContext<*> -> context.getGuild()
            else -> error("Unknown context type provided.")
        }
    }

    val member: MemberBehavior?
        get() = when (context) {
            is PublicSlashCommandContext<*> -> context.member
            is ChatCommandContext<*> -> context.member
            else -> error("Unknown context type provided.")
        }

    val user: UserBehavior?
        get() = when (context) {
            is PublicSlashCommandContext<*> -> context.user
            is ChatCommandContext<*> -> context.user
            else -> error("Unknown context type provided")
        }

    val message: Message?
        get() = when (context) {
            is PublicSlashCommandContext<*> -> null
            is ChatCommandContext<*> -> context.message
            else -> error("Unknown context type provided.")
        }

    @Suppress("UNCHECKED_CAST")
    val arguments: T
        get() = when (context) {
            is PublicSlashCommandContext<*> -> context.arguments
            is ChatCommandContext<*> -> context.arguments
            else -> error("Unknown context type provided.")
        } as T

    suspend fun getPrefix() = when (context) {
        is PublicSlashCommandContext<*> -> "/"
        is ChatCommandContext<*> -> with(getKoin().get<ExtensibleBot>().settings.chatCommandsBuilder) {
            prefixCallback.invoke(context.eventObj as MessageCreateEvent, defaultPrefix)
        }
        else -> error("Unknown context type provided.")
    }

    /**
     * Note: This will not be ephemeral if [context] is from a [MessageCommandContext]
     */
    suspend inline fun ephemeralFollowUp(
        builder: EphemeralHybridMessageCreateBuilder.() -> Unit
    ): EphemeralHybridMessage {
        val messageBuilder = EphemeralHybridMessageCreateBuilder().apply(builder)

        val (response, interaction) = when (context) {
            is PublicSlashCommandContext<*> -> {
                val interaction = context.interactionResponse

                kord.rest.interaction.createFollowupMessage(
                    interaction.applicationId,
                    interaction.token,
                    messageBuilder.toSlashRequest()
                ) to interaction
            }

            is MessageCommandContext<*> -> {
                val messageId = message?.id

                kord.rest.channel.createMessage(
                    channel.id,
                    when (messageId) {
                        null -> messageBuilder.toMessageRequest()
                        else -> messageBuilder.toMessageRequest(messageReference = messageId)
                    }
                ) to null
            }

            else -> error("Unknown context type provided")
        }

        val data = MessageData.from(response)
        return EphemeralHybridMessage(Message(data, kord), interaction?.applicationId, interaction?.token, kord)
    }

    suspend inline fun publicFollowUp(builder: PublicHybridMessageCreateBuilder.() -> Unit): PublicHybridMessage {
        val messageBuilder = PublicHybridMessageCreateBuilder().apply(builder)

        val (response, interaction) = when (context) {
            is PublicSlashCommandContext<*> -> {
                val interaction = context.interactionResponse

                kord.rest.interaction.createFollowupMessage(
                    interaction.applicationId,
                    interaction.token,
                    messageBuilder.toSlashRequest()
                ) to interaction
            }

            is MessageCommandContext<*> -> {
                val messageId = message?.id

                kord.rest.channel.createMessage(
                    channel.id,
                    when (messageId) {
                        null -> messageBuilder.toMessageRequest()
                        else -> messageBuilder.toMessageRequest(messageReference = messageId)
                    }
                ) to null
            }
            else -> error("Unknown context type provided")
        }

        val data = MessageData.from(response)
        return PublicHybridMessage(Message(data, kord), interaction?.applicationId, interaction?.token, kord)
    }

    /**
     * Convenience function for adding components to your message via the [Components] class.
     *
     * @see Components
     */
    suspend fun PublicHybridMessageCreateBuilder.components(
        timeoutSeconds: Long? = null,
        body: suspend ComponentContainer.() -> Unit
    ): ComponentContainer {
        val components = ComponentContainer()

        body(components)
        setup(
            components
            /**, timeoutSeconds **/
        )

        return components
    }

    /**
     * Convenience function for adding components to your message via the [Components] class.
     *
     * @see Components
     */
    suspend fun HybridMessageModifyBuilder.components(
//        timeoutSeconds: Long? = null,
        body: suspend ComponentContainer.() -> Unit
    ): ComponentContainer {
        val components = ComponentContainer()

        body(components)
        setup(
            components
            /** , timeoutSeconds **/
        )

        return components
    }

    suspend fun PublicHybridMessageCreateBuilder.setup(
        component: ComponentContainer
//        , timeoutSeconds: Long? = null
    ) = with(component) {
        applyToMessage()
    }

    suspend fun HybridMessageModifyBuilder.setup(
        component: ComponentContainer,
//        timeoutSeconds: Long? = null
    ) = with(component) {
        applyToMessage()
    }

    /**
     * Convenience function to create a button paginator using a builder DSL syntax. Handles the contextual stuff for you.
     */
    suspend fun paginator(
        defaultGroup: String = "",
        body: PaginatorBuilder.() -> Unit
    ): HybridButtonPaginator {
        val builder = PaginatorBuilder(context.getLocale(), defaultGroup = defaultGroup)

        body(builder)

        return HybridButtonPaginator(builder, this)
    }
}