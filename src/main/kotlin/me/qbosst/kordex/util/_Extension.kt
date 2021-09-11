package me.qbosst.kordex.util

import com.kotlindiscord.kord.extensions.CommandRegistrationException
import com.kotlindiscord.kord.extensions.InvalidCommandException
import com.kotlindiscord.kord.extensions.annotations.ExtensionDSL
import com.kotlindiscord.kord.extensions.checks.types.Check
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.chatCommand
import com.kotlindiscord.kord.extensions.modules.unsafe.annotations.UnsafeAPI
import com.kotlindiscord.kord.extensions.modules.unsafe.extensions.unsafeSlashCommand
import dev.kord.core.event.Event
import me.qbosst.kordex.commands.hybrid.HybridCommand
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

@ExtensionDSL
suspend fun <T: Arguments> Extension.hybridCommand(
    arguments: (() -> T)?,
    body: suspend HybridCommand<T>.() -> Unit
): HybridCommand<T> {
    val hybridCommandObj = HybridCommand(this, arguments)
    body.invoke(hybridCommandObj)

    return hybridCommand(hybridCommandObj)
}

@OptIn(UnsafeAPI::class)
suspend fun <T: Arguments> Extension.hybridCommand(commandObj: HybridCommand<T>): HybridCommand<T> {
    try {
        commandObj.validate()

        // create a message command
        val messageCommandObj = commandObj.toChatCommand()
        chatCommand(messageCommandObj)

        // create a slash command
        val slashCommandObj = commandObj.toSlashCommand()
        unsafeSlashCommand(slashCommandObj)

    } catch (e: CommandRegistrationException) {
        logger.error(e) { "Failed to register command - $e" }
    } catch (e: InvalidCommandException) {
        logger.error(e) { "Failed to register command - $e" }
    }

    return commandObj
}

@ExtensionDSL
suspend fun Extension.hybridCommand(
    body: suspend HybridCommand<Arguments>.() -> Unit
): HybridCommand<Arguments> = hybridCommand(null, body)

@ExtensionDSL
fun Extension.hybridCheck(body: Check<Event>) {
    chatCommandChecks.add(body)
    slashCommandChecks.add(body)
}

@ExtensionDSL
fun Extension.hybridCheck(vararg checks: Check<Event>) {
    chatCommandChecks.addAll(checks)
    slashCommandChecks.addAll(checks)
}