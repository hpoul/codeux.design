package com.dc2f.starter

import com.dc2f.common.Generator
import com.dc2f.common.contentdef.BaseWebsite
import com.dc2f.common.theme.baseTheme
import com.dc2f.render.*
import com.dc2f.util.Dc2fConfig

class WebsiteTheme : Theme() {
    override fun configure(config: ThemeConfig) {
        baseTheme()
    }
}

abstract class MyWebsite : BaseWebsite()

fun main(args: Array<String>) =
    Generator(
        Dc2fConfig(
            contentDirectory = "web/content",
            staticDirectory = "web/static",
            rootContentType = MyWebsite::class,
            theme = WebsiteTheme(),
            urlConfigFromRootContent = { it.config.url }
        )
    ).main(args)
