package io.github.sirmspencer.cljkondofix

import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.COLUMNS_LARGE
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.panel

/** The Settings | Tools | clj-kondo-fix page. */
class CljKondoFixConfigurable : BoundConfigurable("clj-kondo-fix") {

    private val options = CljKondoFixSettings.getInstance().state

    override fun createPanel(): DialogPanel = panel {
        row("clj-kondo-fix executable:") {
            textField()
                .bindText({ options.path.orEmpty() }, { options.path = it })
                .columns(COLUMNS_LARGE)
                .comment("Leave empty to download the release binary that matches the plugin version.")
        }
    }
}
