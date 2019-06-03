/*
 * OpenURP, Agile University Resource Planning Solution.
 *
 * Copyright © 2005, The OpenURP Software.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.openurp.edu.grade.course.service.impl

import org.beangle.data.dao.EntityDao
import org.openurp.code.edu.model.CourseTakeType
import org.openurp.code.edu.model.ExamStatus
import org.openurp.code.edu.model.GradeType
import org.openurp.code.edu.model.GradingMode
import org.openurp.edu.base.model.Student
import org.openurp.edu.grade.course.model.CourseGrade
import org.openurp.edu.grade.course.model.CourseGradeState
import org.openurp.edu.grade.course.model.ExamGrade
import org.openurp.edu.grade.course.model.GaGrade
import org.openurp.edu.grade.course.service.CourseGradeCalculator
import org.openurp.edu.grade.course.service.CourseGradeSettings
import org.openurp.edu.grade.course.service.GradeRateService
import org.openurp.edu.grade.course.service.ScoreConverter
import DefaultCourseGradeCalculator._
import org.openurp.edu.grade.model.Grade
import org.openurp.edu.grade.course.domain.NumRounder
import java.time.Instant

object DefaultCourseGradeCalculator {

  private val Ga = new GradeType(GradeType.EndGa)

  private val MakeupGa = new GradeType(GradeType.MakeupGa)

  private val Makeup = new GradeType(GradeType.Makeup)

  private val DelayGa = new GradeType(GradeType.DelayGa)

  private val Delay = new GradeType(GradeType.Delay)

  private val End = new GradeType(GradeType.End)
}

import DefaultCourseGradeCalculator._
import org.beangle.security.Securities

class DefaultCourseGradeCalculator extends CourseGradeCalculator {

  var entityDao: EntityDao = _

  var gradeRateService: GradeRateService = _

  var settings: CourseGradeSettings = _

  var minEndScore: Float = 0

  var endIsGaWhenFreeListening: Boolean = true

  var numRounder: NumRounder = NumRounder.Normal

  def calcFinal(grade: CourseGrade, state: CourseGradeState) {
    if (!grade.published) grade.status = guessFinalStatus(grade)
    updateScore(grade, calcScore(grade, state), null)
  }

  override def calcAll(grade: CourseGrade, state: CourseGradeState) {
    calcEndGa(grade, state)
    calcMakeupDelayGa(grade, state)
  }

  override def calcEndGa(grade: CourseGrade, state: CourseGradeState): GaGrade = {
    val stdId = grade.std.id
    grade.std = entityDao.get(classOf[Student], stdId)
    val gag = getGaGrade(grade, Ga)
    val gaScore = calcEndGaScore(grade, state)
    updateScore(gag, gaScore, null)
    if (!gag.published && null != state) gag.status = state.getStatus(gag.gradeType)
    if (!grade.published) grade.status = (guessFinalStatus(grade))
    if (gag.status == grade.status) {
      updateScore(grade, calcScore(grade, state), null)
    }
    gag
  }

  protected def calcEndGaScore(grade: CourseGrade, state: CourseGradeState): Option[Float] = {
    var isCheating = false
    for (eg <- grade.examGrades if !isCheating) {
      if (eg.gradeType != Makeup && eg.gradeType != Delay) {
        if (null != eg.examStatus && eg.examStatus.cheating) {
          isCheating = true
        }
      }
    }
    if (isCheating) return Some(0f)

    var ga: Option[Float] = None
    var gaGrade = grade.getGaGrade(GradeType.EndGa).orNull
    if (gaGrade != null) {
      ga = gaGrade.score
      if (grade.examGrades.isEmpty) return ga
    }
    val endGrade = grade.getExamGrade(GradeType.End).orNull
    gaGrade = getGaGrade(grade, Ga)
    if (null != endGrade) {
      if (endIsGaWhenFreeListening && grade.freeListening) {
        return addDelta(gaGrade, endGrade.score, state)
      }
      if (!hasDelta(gaGrade) && None != endGrade.score &&
        java.lang.Float.compare(endGrade.score.get, minEndScore) < 0) {
        return addDelta(gaGrade, endGrade.score, state)
      }
    }
    var totalGa = 0f
    var totalPercent = 0f
    var scorePercent = 0f
    var hasEmptyEndGrade = false
    for (examGrade <- grade.examGrades if (examGrade.gradeType != Delay)) {
      if (examGrade.gradeType == End && null == examGrade.score) {
        hasEmptyEndGrade = true
      }
      getPercent(examGrade, grade, state) foreach { myPercent =>
        if (None != examGrade.score ||
          (null != examGrade.examStatus && examGrade.examStatus.id != ExamStatus.Normal)) {
          totalPercent += myPercent
          examGrade.score foreach { score =>
            scorePercent += myPercent
            totalGa += (myPercent / 100.0f) * score
          }
        }
      }
    }
    if (totalPercent < 100) {
      if (totalPercent > 0) {
        null
      } else {
        gaGrade.score
      }
    } else {
      if (scorePercent <= 51 || hasEmptyEndGrade) {
        null
      } else {
        addDelta(gaGrade, Some(totalGa), state)
      }
    }
  }

  override def calcMakeupDelayGa(grade: CourseGrade, state: CourseGradeState): GaGrade = {
    val stdId = grade.std.id
    grade.std = entityDao.get(classOf[Student], stdId)
    val gatypes = List(DelayGa, MakeupGa)
    var makeupDelayGa: GaGrade = null
    for (gatype <- gatypes) {
      val gag = getGaGrade(grade, gatype)
      var gaScore: Option[Float] = None
      gaScore = if (gatype == DelayGa) calcDelayGaScore(grade, state) else calcMakeupGaScore(grade, state)
      if (None == gaScore) {
        val gaGrade = grade.getGrade(gatype).asInstanceOf[GaGrade]
        grade.gaGrades -= gaGrade
      } else {
        updateScore(gag, gaScore, null)
        if (!gag.published && null != state) gag.status = state.getStatus(gag.gradeType)
        makeupDelayGa = gag
      }
    }
    if (null != makeupDelayGa) {
      if (!grade.published) grade.status = guessFinalStatus(grade)
      if (makeupDelayGa.status == grade.status) {
        updateScore(grade, calcScore(grade, state), null)
      }
    }
    makeupDelayGa
  }

  private def getPercent(eg: ExamGrade, cg: CourseGrade, cgs: CourseGradeState): Option[Short] = {
    if (None != eg.percent) return eg.percent
    if (eg.gradeType == Delay) {
      val end = cg.getExamGrade(GradeType.End)
      if (None != end && None != end.get.percent) {
        end.get.percent
      } else {
        if (null == cgs) None else cgs.getPercent(End)
      }
    } else {
      if (null == cgs) None else cgs.getPercent(eg.gradeType)
    }
  }

  protected def calcDelayGaScore(grade: CourseGrade, state: CourseGradeState): Option[Float] = {
    var gascore: Option[Float] = None
    var gaGrade = grade.getGaGrade(GradeType.DelayGa).orNull
    if (gaGrade != null) {
      gascore = gaGrade.score
      if (grade.getExamGrade(GradeType.Delay).isEmpty) return gascore
    }
    val deGrade = grade.getExamGrade(GradeType.Delay).orNull
    if (deGrade == null) return null
    if (null != deGrade.examStatus && deGrade.examStatus.cheating) return Some(0f)

    val setting = settings.getSetting(grade.project)
    if (setting.delayIsGa) return deGrade.score

    gaGrade = getGaGrade(grade, DelayGa)
    if (endIsGaWhenFreeListening && grade.freeListening) {
      return addDelta(gaGrade, deGrade.score, state)
    }
    if (!hasDelta(gaGrade) && None != deGrade.score && java.lang.Float.compare(deGrade.score.get, minEndScore) < 0) {
      return addDelta(gaGrade, deGrade.score, state)
    }
    var ga = 0f
    var totalPercent = 0f
    var scorePercent = 0f
    for (examGrade <- grade.examGrades; if (examGrade.gradeType != End)) {
      getPercent(examGrade, grade, state) foreach { myPercent =>
        if (null != examGrade.score ||
          (null != examGrade.examStatus && examGrade.examStatus.id != ExamStatus.Normal)) {
          totalPercent += myPercent
          examGrade.score foreach { score =>
            scorePercent += myPercent
            ga += (myPercent / 100.0f) * score
          }
        }
      }
    }
    if (totalPercent < 100) {
      null
    } else {
      if ((scorePercent < 51)) null else addDelta(gaGrade, Some(ga), state)
    }
  }

  protected def calcMakeupGaScore(grade: CourseGrade, gradeState: CourseGradeState): Option[Float] = {
    var gascore: Option[Float] = None
    var gaGrade = grade.getGaGrade(GradeType.MakeupGa).orNull
    if (gaGrade != null) {
      gascore = gaGrade.score
      if (grade.getExamGrade(GradeType.Makeup).isEmpty) return gascore
    }
    val makeup = grade.getExamGrade(GradeType.Makeup).orNull
    if (null == makeup || None == makeup.score) return None
    if (null != makeup.examStatus && makeup.examStatus.cheating) return Some(0f)
    gaGrade = getGaGrade(grade, MakeupGa)
    addDelta(gaGrade, makeup.score, gradeState)
    if ((java.lang.Float.compare(gaGrade.score.get, 60) >= 0)) Some(60f) else gaGrade.score
  }

  protected def calcScore(grade: CourseGrade, state: CourseGradeState): Option[Float] = {
    var best: Option[Float] = None
    for (gg <- grade.gaGrades if gg.score.isDefined) {
      var myScore: Option[Float] = None
      if (gg.gradeType != Ga) {
        if (gg.published) myScore = gg.score
      } else {
        myScore = gg.score
      }
      myScore.foreach { ms =>
        best match {
          case None    => best = myScore
          case Some(b) => if (ms.compareTo(b) > 0) best = Some(ms)
        }
      }
    }
    best
  }

  private def getGaGrade(grade: CourseGrade, gradeType: GradeType): GaGrade = {
    var gaGrade = grade.getGrade(gradeType).asInstanceOf[GaGrade]
    if (null != gaGrade) return gaGrade
    gaGrade = new GaGrade
    gaGrade.gradingMode = grade.gradingMode
    gaGrade.gradeType = gradeType
    gaGrade.updatedAt = Instant.now
    grade.addGaGrade(gaGrade)
    gaGrade
  }

  private def guessFinalStatus(grade: CourseGrade): Int = {
    var status = Grade.Status.New
    grade.getGaGrade(GradeType.EndGa) foreach { ga =>
      if (ga.status > status) status = ga.status
    }
    grade.getGaGrade(GradeType.MakeupGa) foreach { ga =>
      if (ga.status > status) status = ga.status
    }
    grade.getGaGrade(GradeType.DelayGa) foreach { ga =>
      if (ga.status > status) status = ga.status
    }
    status
  }

  def updateScore(grade: CourseGrade, score: Option[Float], newStyle: GradingMode) {
    var gradingMode = newStyle
    if (null == gradingMode) gradingMode = grade.gradingMode else grade.gradingMode = (gradingMode)
    val converter = gradeRateService.getConverter(grade.project, gradingMode)
    grade.score = (score)
    grade.scoreText = (converter.convert(score))
    if (null != grade.courseTakeType &&
      grade.courseTakeType.id == CourseTakeType.Exemption) {
      grade.passed = (true)
    } else {
      grade.passed = (converter.passed(grade.score))
    }
    grade.gp = converter.calcGp(grade.score)
    grade.operator = Securities.user
    grade.updatedAt = Instant.now
  }

  def updateScore(eg: ExamGrade, score: Option[Float], newStyle: GradingMode) {
    eg.score = score
    var gradingMode = newStyle
    if (null == gradingMode) gradingMode = eg.gradingMode else eg.gradingMode = (gradingMode)
    val converter = gradeRateService.getConverter(eg.courseGrade.project, eg.gradingMode)
    eg.scoreText = converter.convert(eg.score)
    eg.passed = converter.passed(eg.score)
    eg.updatedAt = Instant.now
    eg.operator = Securities.user
  }

  def updateScore(gag: GaGrade, score: Option[Float], newStyle: GradingMode) {
    gag.score = score
    var gradingMode = newStyle
    if (null == gradingMode) gradingMode = gag.gradingMode else gag.gradingMode = (gradingMode)
    val converter = gradeRateService.getConverter(gag.courseGrade.project, gradingMode)
    gag.scoreText = converter.convert(gag.score)
    gag.passed = converter.passed(gag.score)
    gag.updatedAt = Instant.now
    gag.operator = Securities.user
    gag.gp = converter.calcGp(gag.score)
  }

  protected def hasDelta(gaGrade: GaGrade): Boolean = {
    gaGrade.delta != None
  }

  protected def getDelta(gaGrade: GaGrade, score: Option[Float], state: CourseGradeState): Float = {
    gaGrade.delta.getOrElse(0f)
  }

  private def addDelta(gaGrade: GaGrade, score: Option[Float], state: CourseGradeState): Option[Float] = {
    if (None == score) return None
    val delta = getDelta(gaGrade, score, state)
    val ga = reserve(delta + score.get, state)
    gaGrade.score = Some(ga)
    gaGrade.score
  }

  protected def reserve(score: Float, state: CourseGradeState): Float = {
    this.numRounder.round(score, 0) //state.precision)
  }

}
