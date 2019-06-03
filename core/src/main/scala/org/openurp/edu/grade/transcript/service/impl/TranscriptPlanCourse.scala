package org.openurp.edu.grade.transcript.service.impl

import java.util.ArrayList
import java.util.List
import java.util.Map
import org.beangle.commons.collection.CollectUtils
import org.beangle.commons.dao.impl.BaseServiceImpl
import org.openurp.edu.base.model.Student
import org.openurp.edu.grade.transcript.service.TranscriptDataProvider
import org.openurp.edu.program.plan.model.CourseGroup
import org.openurp.edu.program.plan.model.CoursePlan
import org.openurp.edu.program.plan.model.PlanCourse
import org.openurp.edu.program.plan.service.CoursePlanProvider
//remove if not needed
import scala.collection.JavaConversions._

class TranscriptPlanCourse extends BaseServiceImpl with TranscriptDataProvider {

  private var coursePlanProvider: CoursePlanProvider = _

  def getDataName(): String = "planCourses"

  def getDatas(stds: List[Student], options: Map[String, String]): AnyRef = {
    val datas = CollectUtils.newHashMap()
    for (std <- stds) {
      datas.put(std, planCourses(std))
    }
    datas
  }

  private def planCourses(std: Student): List[PlanCourse] = {
    val planCourses = new ArrayList[PlanCourse]()
    val coursePlan = coursePlanProvider.getMajorPlan(std)
    if (coursePlan != null) {
      for (courseGroup <- coursePlan.groups) {
        planCourses.addAll(courseGroup.planCourses)
      }
    }
    planCourses
  }

  def setCoursePlanProvider(coursePlanProvider: CoursePlanProvider) {
    this.coursePlanProvider = coursePlanProvider
  }
}
