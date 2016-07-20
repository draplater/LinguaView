package utils

import java.awt.event.{ActionEvent, ActionListener}

/**
  * Created by draplater on 16-7-18.
  */
object UI {
  implicit def toActionListener(f: ActionEvent => Unit) = new ActionListener {
    def actionPerformed(e: ActionEvent) { f(e) }
  }
}
