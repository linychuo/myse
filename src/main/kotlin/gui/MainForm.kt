package gui

import tornadofx.View
import tornadofx.hbox
import tornadofx.label

class MainForm : View("JavaFx html") {

    override val root = hbox {
        label("Hello world")
    }
}