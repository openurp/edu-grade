package org.openurp.edu.grade.course.service.internal

import java.util.List
import org.beangle.commons.dao.impl.BaseServiceImpl
import org.beangle.commons.dao.query.builder.OqlBuilder
import org.openurp.edu.base.code.model.CourseType
import org.openurp.edu.base.model.Course
import org.openurp.edu.base.model.Student
import org.openurp.edu.grade.course.service.GradeCourseTypeProvider
import org.openurp.edu.program.plan.model.CourseGroup
import org.openurp.edu.program.plan.model.CoursePlan
import org.openurp.edu.program.plan.model.PlanCourse
import org.openurp.edu.program.plan.model.SharePlan
import org.openurp.edu.program.plan.service.CoursePlanProvider
//remove if not needed
import scala.collection.JavaConversions._

class GradeCourseTypeProviderImpl extends BaseServiceImpl with GradeCourseTypeProvider {

  var coursePlanProvider: CoursePlanProvider = _

  def courseType(std: Student, course: Course, defaultCourseType: CourseType): CourseType = {
    val plan = coursePlanProvider.coursePlan(std)
    var planCourseType: CourseType = null
    if (null != plan) {
      for (cg <- plan.groups; if (cg != null && planCourseType==null)) {
        cg.planCourses.find(_.course=course) foreach(cg=> planCourseType=cg.courseType)
      }
    }
    if (null == planCourseType) {
      val grade = java.lang.Integer.valueOf(std.getGrade.substring(0, 4))
      val builder = OqlBuilder.from(classOf[SharePlan], "sp").join("sp.groups", "spg")
        .join("spg.planCourses", "spgp")
        .where("spgp.course=:course", course)
        .where("sp.project=:project", std.getProject)
        .where("year(sp.beginOn)<=:grade and (sp.endOn is null or year(sp.endOn)>=:grade)", grade)
        .select("spg.courseType")
      val types = entityDao.search(builder)
      if (!types.isEmpty) {
        planCourseType = if (null != defaultCourseType && types.contains(defaultCourseType)) defaultCourseType else types.get(0)
      }
    }
    if (null == planCourseType) planCourseType = defaultCourseType
    planCourseType
  }

  def setCoursePlanProvider(coursePlanProvider: CoursePlanProvider) {
    this.coursePlanProvider = coursePlanProvider
  }
}
