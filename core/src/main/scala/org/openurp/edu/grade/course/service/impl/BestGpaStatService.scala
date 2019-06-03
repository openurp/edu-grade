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

import org.openurp.edu.base.model.Semester
import org.openurp.edu.base.model.Student
import org.openurp.edu.grade.course.domain.CourseGradeProvider
import org.openurp.edu.grade.course.domain.GpaPolicy
import org.openurp.edu.grade.course.model.CourseGrade
import org.openurp.edu.grade.course.model.StdGpa
import org.openurp.edu.grade.course.model.StdSemesterGpa
import org.openurp.edu.grade.course.model.StdYearGpa
import org.openurp.edu.grade.course.service.GpaStatService
import org.beangle.commons.collection.Collections

class BestGpaStatService extends GpaStatService {

  private var courseGradeProvider: CourseGradeProvider = _

  private var gpaPolicy: GpaPolicy = _

  private var bestGradeFilter: BestGradeFilter = _

  override def stat(std: Student, grades: Seq[CourseGrade]): StdGpa = {
    val stdGpa = gpaPolicy.calc(std, grades, true)
    val stdGpa2 = gpaPolicy.calc(std, bestGradeFilter.filter(grades), false)
    stdGpa.count = stdGpa2.count
    stdGpa.credits = stdGpa2.credits
    stdGpa.totalCredits = stdGpa2.totalCredits
    stdGpa.ga = stdGpa2.ga
    stdGpa.gpa = stdGpa2.gpa
    stdGpa
  }

  def refresh(stdGpa: StdGpa) {
    val newer = stat(stdGpa.std)
    merge(stdGpa, newer)
  }

  def refresh(stdGpa: StdGpa, grades: Seq[CourseGrade]) {
    val newer = stat(stdGpa.std, grades)
    merge(stdGpa, newer)
  }

  def stat(std: Student): StdGpa = {
    stat(std, courseGradeProvider.getPublished(std))
  }

  override def statBySemester(std: Student, semesters: Seq[Semester]): StdGpa = {
    stat(std, courseGradeProvider.getPublished(std, semesters: _*))
  }

  override def stat(stds: Iterable[Student]): MultiStdGpa = {
    val multiStdGpa = new MultiStdGpa()
    for (std <- stds) {
      val stdGpa = stat(std)
      if (stdGpa != null) {
        multiStdGpa.stdGpas += stdGpa
      }
    }
    multiStdGpa.statSemestersFromStdGpa()
    multiStdGpa
  }

  override def statBySemester(stds: Iterable[Student], semesters: Seq[Semester]): MultiStdGpa = {
    val multiStdGpa = new MultiStdGpa()
    for (std <- stds) {
      val stdGpa = statBySemester(std, semesters)
      if (stdGpa != null) {
        multiStdGpa.stdGpas += stdGpa
      }
    }
    multiStdGpa.statSemestersFromStdGpa()
    multiStdGpa
  }

  private def merge(target: StdGpa, source: StdGpa) {
    target.ga = source.ga
    target.gpa = source.gpa
    target.count = source.count
    target.credits = source.credits
    target.totalCredits = source.totalCredits
    val existedTerms = semesterGpa2Map(target.semesterGpas)
    val sourceTerms = semesterGpa2Map(source.semesterGpas)
    for ((key, value) <- sourceTerms) {
      val sourceTerm = value
      existedTerms.get(key) match {
        case None =>
          source.semesterGpas -= sourceTerm
          target.add(sourceTerm)
        case Some(targetTerm) =>
          targetTerm.ga = sourceTerm.ga
          targetTerm.gpa = sourceTerm.gpa
          targetTerm.count = sourceTerm.count
          targetTerm.credits = sourceTerm.credits
          targetTerm.totalCredits = sourceTerm.totalCredits
      }
    }
    for ((key, value) <- existedTerms if null == sourceTerms.get(key)) {
      val targetTerm = value
      targetTerm.stdGpa = null
      target.semesterGpas -= targetTerm
    }
    val existedYears = yearGpa2Map(target.yearGpas)
    val sourceYears = yearGpa2Map(source.yearGpas)
    for ((key, value) <- sourceYears) {
      val sourceTerm = value
      existedYears.get(key) match {
        case None =>
          source.yearGpas -= sourceTerm
          target.add(sourceTerm)
        case Some(targetTerm) =>
          targetTerm.ga = sourceTerm.ga
          targetTerm.gpa = sourceTerm.gpa
          targetTerm.count = sourceTerm.count
          targetTerm.credits = sourceTerm.credits
          targetTerm.totalCredits = sourceTerm.totalCredits
      }
    }
    for ((key, value) <- existedYears if null == sourceYears.get(key)) {
      val targetTerm = value
      targetTerm.stdGpa = null
      target.yearGpas -= targetTerm
    }
    target.updatedAt = source.updatedAt
  }

  private def semesterGpa2Map(gpas: Iterable[StdSemesterGpa]): Map[Semester, StdSemesterGpa] = {
    gpas.map { x => (x.semester, x) }.toMap
  }

  private def yearGpa2Map(gpas: Iterable[StdYearGpa]): Map[String, StdYearGpa] = {
    gpas.map { x => (x.schoolYear, x) }.toMap
  }

}
