package gui

import javafx.application.Application
import tornadofx.App

class MainApp : App(MainForm::class)

fun main(args: Array<String>) {
    Application.launch(MainApp::class.java, *args)
}