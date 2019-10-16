package com.dc2f.starter

import com.dc2f.Nestable
import com.dc2f.common.*
import com.dc2f.common.contentdef.*
import com.dc2f.common.theme.*
import com.dc2f.render.*
import com.dc2f.richtext.RichText
import com.dc2f.util.Dc2fConfig
import com.fasterxml.jackson.annotation.JacksonInject
import kotlinx.html.*

class WebsiteTheme : BaseTheme() {
    override fun configure(config: ThemeConfig) {
        super.configure(config)
        portfolioRenderer(config)
    }

    override fun <T> baseTemplateNavBar(
        tc: TagConsumer<T>,
        context: RenderContext<*>,
        website: BaseWebsite,
        navbarMenuOverride: (DIV.() -> Unit)?
    ) {
        (context.node as? CodeUxLandingPage)?.let {
            tc.div {
                richText(context, it.navBarOverride)
            }
        }
        super.baseTemplateNavBar(tc, context, website, navbarMenuOverride)
    }
}

abstract class MyWebsite : BaseWebsite

@Nestable("uxlandingpage")
interface CodeUxLandingPage : SimpleLandingPage {
    @set:JacksonInject("navBarOverride")
    open var navBarOverride: RichText?
}

fun main(args: Array<String>) =
    Generator(
        GeneratorDc2fConfig(
            contentDirectory = "web/content",
            staticDirectory = "web/static",
            assetBaseDirectory = "src/main/resources/theme",
            rootContentType = MyWebsite::class,
            theme = WebsiteTheme(),
            urlConfigFromRootContent = { it.config.url }
        )
    ).main(args)
