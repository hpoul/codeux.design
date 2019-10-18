package com.dc2f.starter

import com.dc2f.*
import com.dc2f.common.*
import com.dc2f.common.contentdef.*
import com.dc2f.common.theme.*
import com.dc2f.render.*
import com.dc2f.richtext.RichText
import com.fasterxml.jackson.annotation.JacksonInject
import kotlinx.html.*
import org.w3c.dom.Document

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

    override fun <T> baseTemplate(
        tc: TagConsumer<T>,
        context: RenderContext<*>,
        seo: PageSeo,
        headInject: HEAD.() -> Unit,
        navbarMenuOverride: (DIV.() -> Unit)?,
        mainContent: DIV.() -> Unit
    ): Document = scaffold(tc, context, seo, headInject) {
        val website = context.rootNode as BaseWebsite
        tc.nav("") {
            role = "navigation"
            attributes["aria-label"] = "main navigation"
            div("container") {
                baseTemplateNavBar(tc, context, website, navbarMenuOverride)
            }
            val loaderContext = context.renderer.loaderContext

            (loaderContext.contentByPath[ContentPath.parse("/partials/blogheader")] as? Partial)
                ?.let { header ->
                    richText(context.createSubContext(header, context.out, context.node), header.html)
//                    context.renderer.renderPartialContent(
//                        header,
//                        requireNotNull(loaderContext.metadata[header]),
//                        context
//                    )
                }
        }

        tc.main {
            div {
                if (context.node is Blog || context.node is Article) {
//                    div("columns is-desktop") {
                    div("blog-main-content") {
                        mainContent()
                    }
//                        div("column is-4") {
//                            +"this must be sub stuff"
//                        }
//                    }
                } else {
                    mainContent()
                }
            }
        }

        siteFooter(context)
    }


    override fun <T> scaffold(
        tc: TagConsumer<T>,
        context: RenderContext<*>,
        seo: PageSeo,
        headInject: HEAD.() -> Unit,
        body: BODY.() -> Unit
    ): Document =
        super.scaffold(tc, context, seo, headInject, body = {
            if (context.node is Blog || context.node is Article) {
                classes = classes + "blog-content"
            }
            if (context.node is CodeUxLandingPage) {
                classes = classes + "ux-landingpage"
            }
            body()
        })
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
            assetBaseDirectory = "web/assets",
            rootContentType = MyWebsite::class,
            theme = WebsiteTheme(),
            urlConfigFromRootContent = { it.config.url }
        )
    ).main(args)
