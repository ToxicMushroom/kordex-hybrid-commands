package me.qbosst.kordex.commands.hybrid

import com.kotlindiscord.kord.extensions.InvalidCommandException
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.application.slash.PublicSlashCommand
import com.kotlindiscord.kord.extensions.commands.application.slash.SlashGroup
import com.kotlindiscord.kord.extensions.commands.chat.ChatGroupCommand
import com.kotlindiscord.kord.extensions.commands.chat.ChatSubCommand
import com.kotlindiscord.kord.extensions.extensions.Extension

class HybridSubCommand<T: Arguments>(
    extension: Extension,
    arguments: (() -> T)? = null,
    val parent: BasicHybridCommand<out Arguments>
): BasicHybridCommand<T>(extension, arguments) {

    fun slashSettings(init: SlashSettings.() -> Unit) {
        slashSettings.apply(init)
    }

    fun messageSettings(init: MessageSettings.() -> Unit) {
        messageSettings.apply(init)
    }

    override fun validate() {
        super.validate()

        if(!hasBody) {
            throw InvalidCommandException(name, "No command action given.")
        }
    }

    fun toMessageCommand(
        parent: ChatGroupCommand<out Arguments>
    ): ChatSubCommand<T> = ChatSubCommand(extension, arguments, parent).apply {
        this.name = this@HybridSubCommand.name
        this.description = this@HybridSubCommand.description
        this.checkList += this@HybridSubCommand.checkList
        this.requiredPerms += this@HybridSubCommand.requiredPerms

        this.enabled = this@HybridSubCommand.messageSettings.enabled
        this.hidden = this@HybridSubCommand.messageSettings.hidden
        this.aliases = this@HybridSubCommand.messageSettings.aliases

        action { this@HybridSubCommand.body.invoke(HybridCommandContext(this)) }
    }

    fun toSlashCommand(
        parent: PublicSlashCommand<out Arguments>? = null,
        group: SlashGroup? = null
    ): PublicSlashCommand<T> = PublicSlashCommand(extension, arguments, parent, group).apply {
        this.name = this@HybridSubCommand.name
        this.description = this@HybridSubCommand.description
        this.checkList += this@HybridSubCommand.checkList
        this.requiredPerms += this@HybridSubCommand.requiredPerms

        action { this@HybridSubCommand.body.invoke(HybridCommandContext(this)) }
    }
}