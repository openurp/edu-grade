package org.openurp.edu.grade.web

class PlanAuditHelper {

  def batchAudit(
    setting:       AuditSetting,
    stds:          Iterable[Student],
    observerStack: PlanAuditObserverStack,
    weboutput:     OutputWebObserver) {
    observerStack.notifyStart()
    val amount = stds.size
    var msg = MessageFormat.format("{0} 位学生计划完成审核", amount)
    ObserverUtils.notifyStart(weboutput, msg, amount, null)
    var count = 0
    var auditTermsStr = ""
    if (setting.auditTerms != null && setting.auditTerms.length > 0) {
      auditTermsStr = "(第" + Strings.join(setting.auditTerms, ",") + "学期)"
    }
    val params = Map("_weboutput" -> weboutput)
    var iterator = stds.iterator
    while (iterator.hasNext) {
      val student = iterator.next()
      if (null == context.coursePlan) {
        msg = MessageFormat.format("无法找到 {0} {1} 的培养计划，审核失败。", student.user.name, student.user.code)
        ObserverUtils.outputMessage(context, OutputObserver.error, msg, true)
      } else {
        msg = MessageFormat.format("开始审核 {0} {1} 的计划完成情况{2}", student.user.name, student.user.code,
          auditTermsStr)
        ObserverUtils.outputMessage(context, OutputObserver.good, msg, true)
        if (observerStack.notifyBegin(context, count)) {
          audit(student, setting, params)
          observerStack.notifyEnd(context, count)
        }
        msg = MessageFormat.format("审核完毕 {0} {1} 的计划完成情况", student.user.name, student.user.code)
        ObserverUtils.outputMessage(context, OutputObserver.good, msg, false)
      }
      if (iterator.hasNext) {
        ObserverUtils.delimiter(weboutput)
      }
      count += 1
    }
    observerStack.finish()
  }

}