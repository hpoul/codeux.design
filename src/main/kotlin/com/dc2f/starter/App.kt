package com.dc2f.starter

import com.dc2f.*
import com.dc2f.common.*
import com.dc2f.common.contentdef.*
import com.dc2f.common.theme.*
import com.dc2f.render.*
import com.dc2f.richtext.RichText
import com.fasterxml.jackson.annotation.JacksonInject
import kotlinx.html.*
import org.unbescape.javascript.JavaScriptEscape
import org.w3c.dom.Document
import java.time.format.*

class WebsiteTheme : BaseTheme() {
    override fun configure(config: ThemeConfig) {
        themeOverrides(config)
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
                    div("subscribe-button") {
                        a(
                            href = "https://mailchi.mp/f62fd35b6fb3/articles-subscribe",
                            target = "_blank",
                            classes = "button is-primary is-large"
                        ) {
                            attributes["rel"] = "noopener"
                            +"Subscribe to new articles"
                        }
                        p {
                            +("Be the first to get notified for new articles " +
                                "about Flutter, Dart or mobile App Development" +
                                " in general.")
                        }
                    }
                } else {
                    mainContent()
                }
            }
        }

        siteFooter(tc, context)
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


    fun <T> siteFooter(
        tc: TagConsumer<T>,
        context: RenderContext<*>
    ) {
        val website = context.rootNode as BaseWebsite
        tc.footer("footer") {
            div("columns") {
                website.footerMenu.map { menu ->
                    div("column") {
                        span("footer-title title is-4") { +menu.name }

                        ul {
                            menu.children.map { entry ->
                                li {
                                    a(entry.href(context)) {
                                        +entry.linkLabel
                                    }
                                }
                            }
                        }
                    }
                }
                div("column") {
                    richText(
                        context,
                        (website.footerContent?.referencedContent as? Partial)?.html,
                        mapOf("type" to "footer")
                    )

                }


            }

        }

    }
}

fun WebsiteTheme.themeOverrides(config: ThemeConfig) {
    config.pageRenderer<Article> {
        baseTemplate(appendHtmlDocument(), this, node.seo) {
            div("hero is-medium has-bg-img") {
                div("bg-image") {
                    // TODO image resize and blur
                    style = "background-image: url('${node.teaser.href(context)}')"
                    +"x"
                }
                div("hero-body has-text-centered") {
                    h1("title") { +node.title }
                    h2("subtitle is-size-6 has-text-weight-bold") {
                        // TODO format date
                        +(node.subTitle ?: node.date.format(
                            DateTimeFormatter.ofLocalizedDate(
                                FormatStyle.LONG)))
                    }
                }
            }

            div("section") {
                div("container") {
                    div("columns") {
                        div("column") {
                            div("content has-drop-caps line-numbers") {
                                node.html?.let { richText(context, node.html) }
                                    ?: markdown(context, node.body)
                            }
                        }
                    }
                }
            }

            div("section") {
                div("container") {
                    unsafe {
                        val website = context.rootNode as MyWebsite
                        raw("""
<div id="disqus_thread"></div>
<script>

var disqus_config = function () {
this.page.url = '${JavaScriptEscape.escapeJavaScript(context.href(node, true))}';
this.page.identifier = '${JavaScriptEscape.escapeJavaScript(context.renderer.findRenderPath(node).toString())}';
};
setTimeout(function(){
(function() { // DON'T EDIT BELOW THIS LINE
var d = document, s = d.createElement('script');
s.src = 'https://codeuxdesign.disqus.com/embed.js';
s.async = true;
s.setAttribute('data-timestamp', +new Date());
(d.head || d.body).appendChild(s);
})();
}, 1000);
</script>
<noscript>Please enable JavaScript.</noscript>
                        """.trimIndent())
                    }
                }
            }
        }
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
            assetBaseDirectory = "web/assets",
            rootContentType = MyWebsite::class,
            theme = WebsiteTheme(),
            urlConfigFromRootContent = { it.config.url }
        )
    ).main(args)
