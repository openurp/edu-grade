package org.openurp.edu.grade.course.service.internal

import java.util.ArrayList
import java.util.HashMap
import java.util.List
import java.util.Map
import org.beangle.commons.dao.impl.BaseServiceImpl
import org.beangle.commons.dao.query.builder.OqlBuilder
import org.beangle.commons.script.ExpressionEvaluator
import org.openurp.edu.base.code.model.GradingMode
import org.openurp.edu.base.model.Project
import org.openurp.edu.grade.course.model.GradeRateConfig
import org.openurp.edu.grade.course.model.GradeRateItem
import org.openurp.edu.grade.course.service.GradeRateService
import org.openurp.edu.grade.course.service.ScoreConverter
//remove if not needed
import scala.collection.JavaConversions._

class GradeRateServiceImpl extends BaseServiceImpl with GradeRateService {

  private var expressionEvaluator: ExpressionEvaluator = _

   /**
   * 查询记录方式对应的配置
   */
  def getConverter(project: Project, gradingMode: GradingMode): ScoreConverter = {
    if (null == project || null == gradingMode) {
      throw new IllegalArgumentException("require project and grade and grading option ")
    }
    val builder = OqlBuilder.from(classOf[GradeRateConfig], "config")
      .where("config.project=:project and config.gradingMode=:gradingMode", project, gradingMode)
      .cacheable()
    val config = entityDao.uniqueResult(builder)
    if (null == config) throw new RuntimeException("Cannot find ScoreConverter for " + gradingMode.name)
    new ScoreConverter(config, expressionEvaluator)
  }

  def getGradeItems(project: Project): Map[GradingMode, List[GradeRateItem]] = {
    val builder = OqlBuilder.from(classOf[GradeRateConfig], "config")
      .where("config.project=:project and config.gradingMode.numerical=false", project)
    val configs = entityDao.search(builder)
    val datas = new HashMap[GradingMode, Map[String, GradeRateItem]]()
    for (config <- configs) {
      var items = datas.get(config.gradingMode)
      if (null == items) {
        items = new HashMap[String, GradeRateItem]()
        datas.put(config.gradingMode, items)
      }
      for (item <- config.getItems) {
        items.put(item.getGrade, item)
      }
    }
    val rs = new HashMap[GradingMode, List[GradeRateItem]]()
    for ((key, value) <- datas) {
      rs.put(key, new ArrayList[GradeRateItem](value.values))
    }
    rs
  }

    /**
   * 获得支持的记录方式
   *
   * @param project
   * @return
   */
  def gradingModes(project: Project): List[GradingMode] = {
    val builder = OqlBuilder.from(classOf[GradeRateConfig], "config")
      .where("config.project=:project", project)
      .select("config.gradingMode")
      .cacheable()
    entityDao.search(builder)
  }

  def setExpressionEvaluator(expressionEvaluator: ExpressionEvaluator) {
    this.expressionEvaluator = expressionEvaluator
  }
}
