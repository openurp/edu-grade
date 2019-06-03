package org.openurp.edu.grade.audit.service.observers

import org.beangle.commons.lang.Strings
import org.beangle.ems.rule.model.SimpleContext
import org.openurp.edu.eams.web.util.OutputMessage
import org.openurp.edu.eams.web.util.OutputWebObserver

object ObserverUtils {

  private var delimiter: String = Strings.repeat("-", 100)

  /**
   * 在前台输出信息
   *
   * @param context
   *          参数传输容器
   * @param level
   *          级别：1 - 信息（good），2 - 警告（warnning），3 - 错误（error）
   * @param msg
   *          输出信息
   * @param increaceProcess
   *          是否增长
   */
  def outputMessage(
    context:         SimpleContext,
    level:           Int,
    msg:             String,
    increaceProcess: Boolean) {
    val weboutput = context.getParams.get("_weboutput").asInstanceOf[OutputWebObserver]
    outputMessage(weboutput, level, msg, increaceProcess)
  }

  def outputMessage(
    weboutput:       OutputWebObserver,
    level:           Int,
    msg:             String,
    increaceProcess: Boolean) {
    if (weboutput != null) {
      weboutput.outputNotify(level, new OutputMessage(msg, ""), increaceProcess)
    }
  }

  def delimiter(context: SimpleContext) {
    val weboutput = context.getParams.get("_weboutput").asInstanceOf[OutputWebObserver]
    delimiter(weboutput)
  }

  def delimiter(weboutput: OutputWebObserver) {
    outputMessage(weboutput, OutputWebObserver.warnning, delimiter, false)
  }

  def notifyStart(
    weboutput: OutputWebObserver,
    summary:   String,
    count:     Int,
    msgs:      Array[String]) {
    if (weboutput != null) {
      weboutput.notifyStart(summary, count, msgs)
    }
  }
}
