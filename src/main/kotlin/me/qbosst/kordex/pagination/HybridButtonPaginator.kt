package me.qbosst.kordex.pagination

import com.kotlindiscord.kord.extensions.components.ComponentContainer
import com.kotlindiscord.kord.extensions.pagination.BaseButtonPaginator
import com.kotlindiscord.kord.extensions.pagination.EXPAND_EMOJI
import com.kotlindiscord.kord.extensions.pagination.SWITCH_EMOJI
import com.kotlindiscord.kord.extensions.pagination.builders.PaginatorBuilder
import com.kotlindiscord.kord.extensions.pagination.pages.Pages
import dev.kord.core.entity.ReactionEmoji
import dev.kord.core.entity.User
import dev.kord.rest.builder.message.create.allowedMentions
import dev.kord.rest.builder.message.create.embed
import dev.kord.rest.builder.message.modify.allowedMentions
import dev.kord.rest.builder.message.modify.embed
import me.qbosst.kordex.commands.hybrid.HybridCommandContext
import me.qbosst.kordex.commands.hybrid.behaviour.edit
import me.qbosst.kordex.commands.hybrid.entity.PublicHybridMessage
import java.util.*
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

class HybridButtonPaginator(
    pages: Pages,
    owner: User? = null,
    timeoutSeconds: Long? = null,
    keepEmbed: Boolean = true,
    switchEmoji: ReactionEmoji = if (pages.groups.size == 2) EXPAND_EMOJI else SWITCH_EMOJI,
    bundle: String? = null,
    locale: Locale? = null,
    val parentContext: HybridCommandContext<*>,
) : BaseButtonPaginator(pages, owner, timeoutSeconds, keepEmbed, switchEmoji, bundle, locale) {
    @OptIn(ExperimentalTime::class)
    override var components: ComponentContainer = ComponentContainer(timeoutSeconds?.let { Duration.seconds(it) })
    var interaction: PublicHybridMessage? = null

    override suspend fun send() {
        components.removeAll()
        val scopedInteraction = interaction
        if (scopedInteraction == null) {
            setup()

            interaction = parentContext.publicFollowUp {
                allowedMentions { repliedUser = false }
                embed { applyPage() }

                with(parentContext) {
                    this@publicFollowUp.setup(this@HybridButtonPaginator.components)
                }
            }
        } else {
            updateButtons()

            scopedInteraction.edit {
                embed { applyPage() }

                with(parentContext) {
                    this@edit.setup(this@HybridButtonPaginator.components)
                }
            }
        }
    }

    override suspend fun destroy() {
        if (!active) {
            return
        }

        active = false
        components.removeAll()

        if (!keepEmbed) {
            interaction!!.delete()
        } else {
            interaction!!.edit {
                allowedMentions { repliedUser = false }
                embed { applyPage() }

                this.components = mutableListOf()
            }
        }

        runTimeoutCallbacks()
    }
}

/** Convenience function for creating an interaction button paginator from a paginator builder. **/
fun HybridButtonPaginator(
    builder: PaginatorBuilder,
    parentContext: HybridCommandContext<*>,
): HybridButtonPaginator = HybridButtonPaginator(
    pages = builder.pages,
    owner = builder.owner,
    timeoutSeconds = builder.timeoutSeconds,
    keepEmbed = builder.keepEmbed,
    bundle = builder.bundle,
    locale = builder.locale,
    parentContext = parentContext,

    switchEmoji = builder.switchEmoji ?: if (builder.pages.groups.size == 2) EXPAND_EMOJI else SWITCH_EMOJI,
)