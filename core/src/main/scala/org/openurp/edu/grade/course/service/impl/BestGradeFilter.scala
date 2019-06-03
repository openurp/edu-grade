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

import org.beangle.commons.collection.Collections
import org.openurp.edu.base.model.Course
import org.openurp.edu.grade.course.model.CourseGrade
import org.openurp.edu.grade.course.domain.GradeComparator
import org.openurp.edu.grade.course.domain.GradeFilter
import org.openurp.edu.program.plan.domain.AlternativeCourseProvider
import org.openurp.edu.program.plan.model.AlternativeCourse

/**
 * 最好成绩过滤器
 */
class BestGradeFilter extends GradeFilter {

  var alternativeCourseProvider: AlternativeCourseProvider = _

  protected def buildGradeMap(grades: Iterable[CourseGrade]): collection.mutable.Map[Course, CourseGrade] = {
    val gradesMap = Collections.newMap[Course, CourseGrade]
    var old: CourseGrade = null
    for (grade <- grades) {
      old = gradesMap.get(grade.course).orNull
      if (GradeComparator.betterThan(grade, old)) gradesMap.put(grade.course, grade)
    }
    gradesMap
  }

  def filter(grades: Seq[CourseGrade]): Seq[CourseGrade] = {
    val gradesMap = buildGradeMap(grades)
    val substituteCourses = getAlternatives(grades)
    for (subCourse <- substituteCourses if GradeComparator.isSubstitute(subCourse, gradesMap); c <- subCourse.olds) {
      gradesMap.remove(c)
    }
    gradesMap.values.toSeq
  }

  private def getAlternatives(grades: Iterable[CourseGrade]): Seq[AlternativeCourse] = {
    if (grades.isEmpty) {
      List.empty
    } else {
      alternativeCourseProvider.getAlternatives(grades.head.std)
    }
  }
}
