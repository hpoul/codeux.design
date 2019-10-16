package com.dc2f.starter

import com.dc2f.common.*
import com.dc2f.common.contentdef.BaseWebsite
import com.dc2f.common.theme.*
import com.dc2f.render.*
import com.dc2f.util.Dc2fConfig

class WebsiteTheme : BaseTheme() {
    override fun configure(config: ThemeConfig) {
        super.configure(config)
        portfolioRenderer(config)
    }
}

abstract class MyWebsite : BaseWebsite

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
