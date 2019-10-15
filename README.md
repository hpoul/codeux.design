# dc2f-starter-site

Demo web site based on [dc2f](https://github.com/dc2f/dc2f). This is a (very) opinionated starter package to 
easily start off with creating a statically published app with dc2f.

I also use it as an opportunity to document the steps necessary to 
start a new project from scratch.

## Getting Started

Just click the [Use this template](https://github.com/dc2f/dc2f-starter-site/generate) button and create a new repository.

Afterwards clone your repository locally

* Install a [Java SDK](https://www.oracle.com/technetwork/java/javase/downloads/index.html)
* Install [npm](https://www.npmjs.com/) (required for installing bulma).
* Run `npm install` in the root directory.

## Launching

```bash
./dc2f.sh serve
```

Visit: http://localhost:5050/


# Steps - Starting from scratch.

## Gradle

gradle init (kotlin)

## Dependencies

**build.gradle**

```kotlin
// add mavenCentral() as repository
repositories {
    // watchservice is installed through jitpack.
    maven("https://jitpack.io")
    mavenCentral()
    // allow snapshot versions (for now)
    maven("https://oss.sonatype.org/content/groups/public/")
}

// .....
dependencies {
    implementation("com.dc2f:dc2f:+")
    implementation("com.dc2f:dc2f-common:+")
}
```

**App.kt**

```kotlin
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
```

## Creating content

Minimal directory structure:

```bash
mkdir -p web/content web/static
```

* ./web/content:
  * _index.yml
  * @index.landingpage/
    * _index.yml
    * 001.content/
      * @body.yml

**web/content/_index.yml**

```yaml
name: 'Starter Site for dc2f'
config:
  url:
    protocol: 'https'
    host: 'starter.dc2f.com'
  favicons: []
mainMenu: []
footerMenu: []
```

**web/content/@index.landingpage/_index.yml**

```yaml
seo:
  title: DC2F Starter Site
  description: Nice little starter page.
```

**web/content/@index.landingpage/@body.md**

```markdown
# Welcome...

...to this wonderful [dc2f starter page](https://dc2f.com).
```

## CSS

Let's create a simple stylesheet based on bulma

```bash
> npm init -y
> npm install bulma
```

**src/main/resources/theme/scss/main.scss**

```scss
$primary: #626BC6;
$secondary: #97c965;
@import 'node_modules/bulma/sass/utilities/initial-variables';
@import 'node_modules/bulma/bulma';

```

## JS

create an empty file at `src/main/resources/theme/script/main.js`
