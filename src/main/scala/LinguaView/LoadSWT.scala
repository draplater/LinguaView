package LinguaView

import java.lang.reflect.Method
import java.net.URL
import java.net.URLClassLoader

class LoadSWT {
}

object LoadSWT {
  def main(args: Array[String]) {
    try {
      Class.forName("org.eclipse.swt.SWT")
    } catch {
      case e: ClassNotFoundException => loadSWT()
    }
    LinguaView.main(args)
  }

  def loadSWT(): Unit = {
    val osName = System.getProperty("os.name").toLowerCase
    val osArch = System.getProperty("os.arch").toLowerCase

    val swtFileNameOsPart = if (osName.contains("win")) "win32"
    else if (osName.contains("mac")) "macosx"
    else if (osName.contains("linux") || osName.contains("nix")) "linux_gtk"
    else ""

    val swtFileNameArchPart = if (osArch.contains("64")) "x64" else "x86"
    val swtFileName = s"swt_${swtFileNameOsPart}_$swtFileNameArchPart.jar"

    try {
      val classLoader: URLClassLoader =
        getClass.getClassLoader.asInstanceOf[URLClassLoader]
      val addUrlMethod: Method =
        classOf[URLClassLoader].getDeclaredMethod("addURL", classOf[URL])
      addUrlMethod.setAccessible(true)
      val fileURL = new URL(s"rsrc:jar/$swtFileName")
      addUrlMethod.invoke(classLoader, fileURL)
    }
    catch {
      case e: Exception =>
        throw new RuntimeException(
          s"Unable to add the SWT jar to the class path: $swtFileName", e)
    }
  }
}