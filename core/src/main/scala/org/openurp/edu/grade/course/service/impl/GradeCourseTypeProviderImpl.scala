/*
 * OpenURP, Agile University Resource Planning Solution.
 *
 * Copyright © 2014, The OpenURP Software.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful.
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.openurp.edu.grade.course.service.impl

import org.beangle.data.dao.impl.BaseServiceImpl
import org.beangle.data.dao.OqlBuilder
import org.openurp.edu.base.code.model.CourseType
import org.openurp.edu.base.model.Course
import org.openurp.edu.base.model.Student
import org.openurp.edu.grade.course.service.GradeCourseTypeProvider
import org.openurp.edu.program.plan.model.CourseGroup
import org.openurp.edu.program.plan.model.CoursePlan
import org.openurp.edu.program.plan.model.PlanCourse
import org.openurp.edu.program.plan.model.SharePlan
import org.openurp.edu.program.plan.domain.CoursePlanProvider

class GradeCourseTypeProviderImpl extends BaseServiceImpl with GradeCourseTypeProvider {

  var coursePlanProvider: CoursePlanProvider = _

  def courseType(std: Student, course: Course, defaultCourseType: CourseType): CourseType = {
    val plan = coursePlanProvider.getCoursePlan(std)
    var planCourseType: CourseType = null
    if (null != plan) {
      for (cg <- plan.groups; if (cg != null && planCourseType == null)) {
        cg.planCourses.find(_.course == course) foreach (_ => planCourseType = cg.courseType)
      }
    }
    if (null == planCourseType) {
      //FIXME ??
      val grade = java.lang.Integer.valueOf(std.state.get.grade.substring(0, 4))
      val builder = OqlBuilder.from[CourseType](classOf[SharePlan].getName, "sp").join("sp.groups", "spg")
        .join("spg.planCourses", "spgp")
        .where("spgp.course=:course", course)
        .where("sp.project=:project", std.project)
        .where("year(sp.beginOn)<=:grade and (sp.endOn is null or year(sp.endOn)>=:grade)", grade)
        .select("spg.courseType")
      val types = entityDao.search(builder)
      if (!types.isEmpty) {
        planCourseType =
          if (null != defaultCourseType && types.contains(defaultCourseType)) defaultCourseType else types.head
      }
    }
    if (null == planCourseType) planCourseType = defaultCourseType
    planCourseType
  }

  def setCoursePlanProvider(coursePlanProvider: CoursePlanProvider): Unit = {
    this.coursePlanProvider = coursePlanProvider
  }
}
