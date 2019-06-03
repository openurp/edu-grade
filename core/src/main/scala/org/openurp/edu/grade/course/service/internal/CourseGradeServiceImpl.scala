package org.openurp.edu.grade.course.service.internal

import org.openurp.edu.grade.Grade.Status.New
import org.openurp.edu.grade.Grade.Status.Published
import java.util.Collection
import java.util.Collections
import java.util.List
import java.util.Set
import org.beangle.commons.bean.comparators.PropertyComparator
import org.beangle.commons.collection.CollectUtils
import org.beangle.commons.dao.Operation
import org.beangle.commons.dao.impl.BaseServiceImpl
import org.beangle.commons.dao.query.builder.OqlBuilder
import org.beangle.commons.entity.metadata.Model
import org.beangle.commons.lang.Objects
import org.beangle.commons.lang.Strings
import org.beangle.commons.lang.functor.Predicate
import org.openurp.edu.base.code.model.CourseTakeType
import org.openurp.edu.base.code.model.GradeType
import org.openurp.edu.base.model.Project
import org.openurp.edu.grade.Grade
import org.openurp.edu.grade.course.domain.GradeTypeConstants
import org.openurp.edu.grade.course.model.CourseGrade
import org.openurp.edu.grade.course.model.CourseGradeSetting
import org.openurp.edu.grade.course.model.CourseGradeState
import org.openurp.edu.grade.course.model.ExamGrade
import org.openurp.edu.grade.course.model.ExamGradeState
import org.openurp.edu.grade.course.model.GaGrade
import org.openurp.edu.grade.course.model.GaGradeState
import org.openurp.edu.grade.course.model.GradeState
import org.openurp.edu.grade.course.service.CourseGradeCalculator
import org.openurp.edu.grade.course.service.CourseGradePublishStack
import org.openurp.edu.grade.course.service.CourseGradeService
import org.openurp.edu.grade.course.service.CourseGradeSettings
import org.openurp.edu.grade.course.service.GradeCourseTypeProvider
import org.openurp.edu.grade.course.service.ScoreConverter
import org.openurp.edu.course.model.Clazz
//remove if not needed
import scala.collection.JavaConversions._

class CourseGradeServiceImpl extends BaseServiceImpl with CourseGradeService {

  protected var calculator: CourseGradeCalculator = _

  protected var gradeCourseTypeProvider: GradeCourseTypeProvider = _

  protected var publishStack: CourseGradePublishStack = _

  protected var settings: CourseGradeSettings = _

  private def getGrades(clazz: Clazz): List[CourseGrade] = {
    val query = OqlBuilder.from(classOf[CourseGrade], "courseGrade")
    query.where("courseGrade.clazz = :clazz", clazz)
    entityDao.search(query)
  }

  def getState(clazz: Clazz): CourseGradeState = {
    val list = entityDao.get(classOf[CourseGradeState], "clazz", clazz)
    if (list.isEmpty) {
      return null
    }
    list.get(0)
  }

  def getPublishableGradeTypes(project: Project): List[GradeType] = {
     // 查找除去最终成绩之外的所有可发布成绩
    val gradeTypes = entityDao.getAll(classOf[GradeType])
    CollectUtils.filter(gradeTypes, new Predicate[GradeType]() {

      override def apply(input: GradeType): java.lang.Boolean = {
        return input.isGa || input.id == GradeTypeConstants.FINAL_ID
      }
    })
    Collections.sort(gradeTypes, new PropertyComparator("code"))
    gradeTypes
  }

  
  /**
   * 发布学生成绩
   */
  def publish(clazzIdSeq: String, gradeTypes: Array[GradeType], isPublished: Boolean) {
    val clazzes = entityDao.get(classOf[Clazz], Strings.transformToLong(clazzIdSeq.split(",")))
    if (CollectUtils.isNotEmpty(clazzes)) {
      for (clazz <- clazzes) {
        updateState(clazz, gradeTypes, if (isPublished) Published else New)
      }
    }
  }

   /** 依据状态调整成绩 */
  def recalculate(gradeState: CourseGradeState) {
    if (null == gradeState) {
      return
    }
    val published = CollectUtils.newArrayList()
    for (egs <- gradeState.getExamStates if egs.getStatus == Published) published.add(egs.gradeType)
    for (egs <- gradeState.getGaStates if egs.getStatus == Published) published.add(egs.gradeType)
    val grades = getGrades(gradeState.getClazz)
    for (grade <- grades) {
      updateGradeState(grade, gradeState, grade.getProject)
      for (state <- gradeState.getExamStates) {
        val gradeType = state.gradeType
        updateGradeState(grade.getExamGrade(gradeType), state, grade.getProject)
      }
      calculator.calcAll(grade, gradeState)
    }
    entityDao.saveOrUpdate(grades)
    if (!published.isEmpty) publish(gradeState.clazz.id.toString, published.toArray(Array.ofDim[GradeType](published.size)), 
      true)
  }

  def remove(clazz: Clazz, gradeType: GradeType) {
    val state = getState(clazz)
    val courseGrades = entityDao.get(classOf[CourseGrade], "clazz", clazz)
    val gradeSetting = settings.setting(clazz.getProject)
    val save = CollectUtils.newArrayList()
    val remove = CollectUtils.newArrayList()
    val gts = CollectUtils.newHashSet(gradeType)
    if (GradeTypeConstants.GA_ID == gradeType.id) {
      gts.addAll(gradeSetting.getGaElementTypes)
    } else if (GradeTypeConstants.MAKEUP_GA_ID == gradeType.id) {
      gts.add(new GradeType(GradeTypeConstants.MAKEUP_ID))
    } else if (GradeTypeConstants.DELAY_GA_ID == gradeType.id) {
      gts.add(new GradeType(GradeTypeConstants.DELAY_ID))
    }
    for (courseGrade <- courseGrades;if (courseGrade.courseTakeType.id != CourseTakeType.Exemption) ) {
      if (GradeTypeConstants.FINAL_ID == gradeType.id) {
        if (New == courseGrade.getStatus) remove.add(courseGrade)
      } else {
        if (removeGrade(courseGrade, gts, state)) {
          remove.add(courseGrade)
        } else {
          save.add(courseGrade)
        }
      }
    }
    if (null != state) {
      if (GradeTypeConstants.FINAL_ID == gradeType.id) {
        state.setStatus(New)
        state.getExamStates.clear()
        state.getGaStates.clear()
      } else {
        for (gt <- gts) {
          if (gt.isGa) {
            val ggs = state.getState(gt).asInstanceOf[GaGradeState]
            state.getGaStates.remove(ggs)
          } else {
            val egs = state.getState(gt).asInstanceOf[ExamGradeState]
            state.getExamStates.remove(egs)
          }
        }
      }
    }
    if (state.getExamStates.isEmpty && state.getGaStates.isEmpty) {
      remove.add(state)
    } else {
      save.add(state)
    }
            // FIXME 日志
    entityDao.execute(Operation.saveOrUpdate(save).remove(remove))
  }

  private def removeGrade(courseGrade: CourseGrade, gradeTypes: Collection[GradeType], state: CourseGradeState): Boolean = {
    for (gradeType <- gradeTypes) {
      if (gradeType.isGa) {
        val ga = courseGrade.getGaGrade(gradeType)
        if (null != ga && New == ga.getStatus) courseGrade.getGaGrades.remove(ga)
      } else {
        val exam = courseGrade.getExamGrade(gradeType)
        if (null != exam && New == exam.getStatus) courseGrade.getExamGrades.remove(exam)
      }
    }
    if (CollectUtils.isNotEmpty(courseGrade.getGaGrades) || CollectUtils.isNotEmpty(courseGrade.getExamGrades)) {
      calculator.calcAll(courseGrade, state)
      false
    } else {
      true
    }
  }

  def setCalculator(calculator: CourseGradeCalculator) {
    this.calculator = calculator
  }

  def setCourseGradePublishStack(courseGradePublishStack: CourseGradePublishStack) {
    this.publishStack = courseGradePublishStack
  }

  def setGradeCourseTypeProvider(gradeCourseTypeProvider: GradeCourseTypeProvider) {
    this.gradeCourseTypeProvider = gradeCourseTypeProvider
  }

    /**
   * 依据状态信息更新成绩的状态和记录方式
   *
   * @param grade
   * @param state
   */
  private def updateGradeState(grade: Grade, state: GradeState, project: Project) {
    if (null != grade && null != state) {
      if (Objects.!=(grade.gradingMode, state.gradingMode)) {
        grade.setGradingMode(state.gradingMode)
        val converter = calculator.getGradeRateService.getConverter(project, state.gradingMode)
        grade.setScoreText(converter.convert(grade.score))
      }
      grade.setStatus(state.getStatus)
    }
  }

  private def updateState(clazz: Clazz, gradeTypes: Array[GradeType], status: Int) {
    val courseGradeStates = entityDao.get(classOf[CourseGradeState], "clazz", clazz)
    var gradeState: CourseGradeState = null
    for (gradeType <- gradeTypes) {
      gradeState = if (courseGradeStates.isEmpty) Model.newInstance(classOf[CourseGradeState]) else courseGradeStates.get(0)
      if (gradeType.id == GradeTypeConstants.FINAL_ID) {
        gradeState.setStatus(status)
      } else {
        gradeState.updateStatus(gradeType, status)
      }
    }
    val grades = entityDao.get(classOf[CourseGrade], "clazz", clazz)
    val toBeSaved = CollectUtils.newArrayList()
    val published = CollectUtils.newHashSet()
    for (grade <- grades; if (grade.courseTakeType.id != CourseTakeType.Exemption)) {
      for (gradeType <- gradeTypes) {
        var updated = false
        if (gradeType.id == GradeTypeConstants.FINAL_ID) {
          grade.setStatus(status)
          updated = true
        } else {
          val examGrade = grade.getGrade(gradeType)
          if (null != examGrade) {
            examGrade.setStatus(status)
            updated = true
          }
        }
        if (updated) published.add(grade)
      }
    }
    if (status == Published) toBeSaved.addAll(publishStack.onPublish(published, gradeState, gradeTypes))
    toBeSaved.addAll(Operation.saveOrUpdate(clazz, gradeState).saveOrUpdate(published)
      .build())
    entityDao.execute(toBeSaved.toArray(Array.ofDim[Operation](toBeSaved.size)))
  }

  def setSettings(settings: CourseGradeSettings) {
    this.settings = settings
  }
}
