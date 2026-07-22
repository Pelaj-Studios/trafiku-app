package com.pelajstudios.trafiku.ui.app

import android.content.Context
import android.content.res.Resources
import android.os.LocaleList
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

private val supportedLocales = listOf(
    Locale.forLanguageTag("sq"),
    Locale.forLanguageTag("sr-Latn"),
    Locale.ENGLISH,
)

private val LocalTrilingualStringResolver = staticCompositionLocalOf<TrilingualStringResolver?> {
    null
}

@Composable
internal fun TrilingualStrings(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val resolver = remember(context) { TrilingualStringResolver(context) }
    CompositionLocalProvider(
        LocalTrilingualStringResolver provides resolver,
        content = content,
    )
}

@Composable
internal fun trilingualStringResource(
    @StringRes resourceId: Int,
    vararg formatArguments: Any,
): String {
    val context = LocalContext.current
    val providedResolver = LocalTrilingualStringResolver.current
    val fallbackResolver = remember(context, providedResolver) {
        providedResolver ?: TrilingualStringResolver(context)
    }
    return fallbackResolver.getString(resourceId, formatArguments)
}

internal fun localizedStringArgument(@StringRes resourceId: Int): LocalizedStringArgument =
    LocalizedStringArgument(resourceId)

@JvmInline
internal value class LocalizedStringArgument(@param:StringRes val resourceId: Int)

private class TrilingualStringResolver(context: Context) {
    private val localizedResources = supportedLocales.map { locale ->
        val configuration = context.resources.configuration
        val localizedConfiguration = android.content.res.Configuration(configuration).apply {
            setLocales(LocaleList(locale))
        }
        context.createConfigurationContext(localizedConfiguration).resources
    }

    fun getString(
        @StringRes resourceId: Int,
        formatArguments: Array<out Any>,
    ): String = localizedResources.joinToString(separator = "\n") { resources ->
        val localizedArguments = formatArguments.map { argument ->
            argument.resolve(resources)
        }.toTypedArray()
        resources.getString(resourceId, *localizedArguments)
    }

    private fun Any.resolve(resources: Resources): Any = when (this) {
        is LocalizedStringArgument -> resources.getString(resourceId)
        else -> this
    }
}
