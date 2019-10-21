package com.dc2f.starter

import com.dc2f.*
import com.dc2f.common.contentdef.*
import com.dc2f.common.theme.*
import com.dc2f.render.ThemeConfig
import com.dc2f.richtext.markdown.Markdown
import kotlinx.html.*

@Nestable("herby")
abstract class HerbyIntro : LandingPageElement() {
    abstract val intro: String
    abstract val introImage: ImageAsset
    abstract val profileImage: ImageAsset
}


@Nestable("portfolio")
abstract class PortfolioElement : LandingPageElement() {
    abstract val screenshot: ImageAsset
    abstract val title: String
    abstract val subTitle: String
    // TODO change this to a URL?
    abstract val link: String?
    abstract val intro: Markdown

    abstract val codeTags: List<String>
    abstract val code: String

    abstract val uxTags: List<String>
    abstract val ux: String
}

fun WebsiteTheme.portfolioRenderer(config: ThemeConfig) {

    config.pageRenderer<HerbyIntro> {
        appendHtmlPartial() {
            header("herby-info") {
                div("info-column") {
                    div("header-logo") {
                        img {
                            alt = "codeux.design"
                            src = node.introImage.href(context)
                            width = "505"
                            height = "68"
                        }
                    }
                    h1 { +node.intro }
                }
                div("avatar-column") {
                    div("avatar-herbert-image") {
                        imageAsPicture(
                            context,
                            node.profileImage,
                            "Herbert Profile Image",
                            resize = Resize(width = 400, height = 400, fillType = FillType.Cover)
                        )
                    }
                }
            }
        }
    }

    config.pageRenderer<PortfolioElement> {
        val index = ((enclosingNode as LandingPage).children.indexOf(node))
        appendHtmlPartial {
            div(
                "portfolio-project"
            ) {
                fun info() {
                    div("portfolio-project-info") {
                        h2 { +node.title }
                        h3 {
                            node.link?.let { a(it) { +node.subTitle } }
                                ?: +node.subTitle
                        }

                        // markdown content is always rendered with <p> tag.
                        richText(context, node.intro)

                        div("portfolio-project-info-code") {
                            div("info-tags") {
                                node.codeTags.map { +"#$it " }
                            }
                            p { +node.code }
                        }

                        div("portfolio-project-info-ux") {
                            div("info-tags") {
                                node.uxTags.map { +"#$it " }
                            }
                            p { +node.ux }
                        }
                    }
                }

                fun mockup() {
                    div("portfolio-project-mockup") {
                        p { +" " }
                        img(
                            context,
                            node.screenshot,
                            Resize(height = 400, fillType = FillType.Fit)
                        ) {
                            alt = node.title
                        }
                    }
                }
                if (index % 2 == 0) {
                    classes = classes + "even"
                }
                mockup()
                info()

            }
        }
    }
}
