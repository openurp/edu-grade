package org.openurp.edu.grade.course.service

import javax.validation.ConstraintViolation
import javax.validation.ConstraintViolationException
import org.beangle.commons.collection.Collections
import org.beangle.data.dao.EntityDao
import org.beangle.data.dao.OqlBuilder
import org.beangle.commons.entity.Entity
import org.beangle.commons.entity.metadata.Model
import org.beangle.commons.lang.Strings
import org.beangle.commons.transfer.TransferMessage
import org.beangle.commons.transfer.TransferResult
import org.beangle.commons.transfer.importer.listener.ItemImporterListener
import org.openurp.base.edu.model.Semester
import org.openurp.code.edu.model.CourseType
import org.openurp.code.edu.model.ExamStatus
import org.openurp.code.edu.model.GradeType
import org.openurp.code.edu.model.GradingMode
import org.openurp.base.edu.model.Course
import org.openurp.base.edu.model.Project
import org.openurp.base.edu.model.Student
import org.openurp.edu.clazz.model.Clazz
import org.openurp.edu.grade.course.model.CourseGrade
import org.openurp.edu.grade.course.model.CourseGradeState
import org.openurp.edu.grade.course.model.ExamGrade

/**
 * 成绩导入监听器
 */
class CourseGradeImportListener(protected var entityDao: EntityDao, protected var project: Project, protected var calculator: CourseGradeCalculator)
  extends ItemImporterListener {

  override def onFinish(tr: TransferResult) {
  }

  override def onItemStart(tr: TransferResult) {
  }

  override def onItemFinish(tr: TransferResult) {
    val errors = tr.getErrs
    tr.getErrs.clear()
    val courseGrade = populateCourseGrade(tr)
    if (!tr.hasErrors()) {
      try {
        courseGrade.updatedAt = Instant.now
        entityDao.saveOrUpdate(courseGrade)
      } catch {
        case e: ConstraintViolationException => {
          tr.getErrs.addAll(errors)
          for (constraintViolation <- e.getConstraintViolations) {
            tr.addFailure(constraintViolation.getPropertyPath + constraintViolation.getMessage, constraintViolation.getInvalidValue)
          }
        }
      }
    }
  }

  private def getPropEntity[T <: Entity[_]](
    clazz:   Class[T],
    tr:      TransferResult,
    key:     String,
    notNull: Boolean): T = {
    val description = importer.getDescriptions.get(key)
    val value = importer.getCurData.get(key).asInstanceOf[String]
    if (Strings.isBlank(value)) {
      if (notNull) {
        tr.addFailure(description + "不能为空", value)
      } else {
        return null
      }
    }
    if (classOf[Semester].isAssignableFrom(clazz)) {
      val query = OqlBuilder.from(clazz, "semester")
      query.where("semester.code = :code", value)
      val titleList = entityDao.search(query)
      if (titleList.size == 1) {
        return titleList.head
      }
    }
    val nameList = entityDao.get(clazz, "name", value)
    if (nameList.size != 1) {
      val codeList = entityDao.get(clazz, "code", value)
      if (codeList.size == 1) {
        return codeList.head
      } else if ((nameList.size + codeList.size) == 0) {
        tr.addFailure(importer.getDescriptions.get(key) + "不存在", value)
      } else {
        tr.addFailure(importer.getDescriptions.get(key) + "存在多条记录", value)
      }
      return null
    }
    nameList.head
  }

  private def getClazz(
    tr:       TransferResult,
    key:      String,
    course:   Course,
    semester: Semester): Clazz = {
    val value = importer.getCurData.get(key).asInstanceOf[String]
    val builder = OqlBuilder.from(classOf[Clazz], "clazz")
    builder.where("clazz.semester =:semester", semester)
    builder.where("clazz.course = :course", course)
    if (value != null) {
      builder.where("clazz.crn =:crn", value)
    }
    val noList = entityDao.search(builder)
    if (noList.size == 1) {
      return noList.head
    }
    null
  }

  private def populateCourseGrade(tr: TransferResult): CourseGrade = {
    val std = getPropEntity(classOf[Student], tr, "std", true)
    val course = getPropEntity(classOf[Course], tr, "course", true)
    val semester = getPropEntity(classOf[Semester], tr, "semester", true)
    val courseGrade = checkCourseGradeExists(project, std, course, semester, tr)
    val clazz = getClazz(tr, "clazz", course, semester)
    var cgs: CourseGradeState = null
    if (null != clazz) {
      courseGrade.setClazz(clazz)
      cgs = getCourseGradeState(clazz)
    }
    setExamGrades(courseGrade, tr, cgs)
    if (null != std) {
      calculator.calcAll(courseGrade, cgs)
    }
    val courseType = getPropEntity(classOf[CourseType], tr, "courseType", true)
    if (null != courseType) {
      courseGrade.courseType = (courseType)
    }
    courseGrade
  }

  /**
   * 根据任务查找成绩状态
   *
   * @param clazz
   * @return
   */
  private def getCourseGradeState(clazz: Clazz): CourseGradeState = {
    val cgses = entityDao.get(classOf[CourseGradeState], "clazz", clazz)
    if (cgses.size == 1) {
      cgses.head
    } else {
      null
    }
  }

  private def checkCourseGradeExists(
    project:  Project,
    std:      Student,
    course:   Course,
    semester: Semester,
    tr:       TransferResult): CourseGrade = {
    val builder = OqlBuilder.from(classOf[CourseGrade], "courseGrade")
    builder.where("courseGrade.std = :student", std)
    builder.where("courseGrade.course = :course", course)
    builder.where("courseGrade.semester = :semester", semester)
    builder.where("courseGrade.project = :project", project)
    var courseGrades: List[CourseGrade] = null
    val courseGrade = Model.newInstance(classOf[CourseGrade])
    try {
      courseGrades = entityDao.search(builder)
      if (courseGrades.size == 1) {
        return courseGrades.head
      } else if (courseGrades.size > 1) {
        tr.addFailure("存在多条记录(", std.name + "," + project.name + "," + course.name +
          "," +
          semester.getCode +
          ")")
        return null
      }
      courseGrade.setStd(std)
      courseGrade.setProject(project)
      courseGrade.setSemester(semester)
      courseGrade.course = (course)
    } catch {
      case e: Exception =>
    }
    courseGrade
  }

  /**
   * 成绩验证(期中成绩,期末成绩等)
   *
   * @param courseGrade
   * @param tr
   * @return
   */
  private def setExamGrades(courseGrade: CourseGrade, tr: TransferResult, cgs: CourseGradeState) {
    var examGrades = courseGrade.examGrades
    val gradeTypes = entityDao.getAll(classOf[GradeType])
    if (Collections.isEmpty(examGrades)) {
      examGrades = Collections.newHashSet()
    }
    var examStatus = getPropEntity(classOf[ExamStatus], tr, "examStatus", false)
    if (examStatus == null) {
      examStatus = entityDao.get(classOf[ExamStatus], ExamStatus.NORMAL)
    }
    var gradingMode = getPropEntity(classOf[GradingMode], tr, "gradingMode", false)
    if (null == gradingMode) {
      gradingMode = entityDao.get(classOf[GradingMode], GradingMode.Percent)
    }
    courseGrade.setGradingMode(gradingMode)
    for (gradeType <- gradeTypes) {
      val value = importer.getCurData.get("TYPE" + gradeType.id).asInstanceOf[String]
      if (Strings.isNotBlank(value)) {
        val examGrade = checkExamGradeExists(examGrades, gradeType)
        examGrade.setGradeType(gradeType)
        examGrade.setExamStatus(examStatus)
        examGrade.setCourseGrade(courseGrade)
        examGrade.setGradingMode(gradingMode)
        checkGrade(value, examGrade, tr, gradingMode)
        courseGrade.addExamGrade(examGrade)
      }
    }
    calculator.calcAll(courseGrade, null)
  }

  private def checkGrade(
    value:       String,
    examGrade:   ExamGrade,
    tr:          TransferResult,
    gradingMode: GradingMode) {
    if (Strings.isNotEmpty(value)) {
      if (value.matches("^\\d*\\.?\\d*$") && java.lang.Float.parseFloat(value) <= 100) {
        examGrade.setScore(java.lang.Float.parseFloat(value))
      } else if (value.matches("^\\d*\\.?\\d*$") && java.lang.Float.parseFloat(value) > 100) {
        tr.addFailure("分数不能大于100", value)
      } else {
        examGrade.setScoreText(value)
        val converter = calculator.getGradeRateService.getConverter(
          examGrade.courseGrade.project,
          gradingMode)
        examGrade.setScore(converter.convert(examGrade.scoreText))
      }
    }
  }

  private def checkExamGradeExists(examGrades: Set[ExamGrade], gradeType: GradeType): ExamGrade = {
    var itor = examGrades.iterator()
    while (itor.hasNext) {
      val examGrade = itor.next()
      if (examGrade.gradeType.id == gradeType.id) {
        return examGrade
      }
    }
    Model.newInstance(classOf[ExamGrade])
  }

  def setEntityDao(entityDao: EntityDao) {
    this.entityDao = entityDao
  }

  def setProject(project: Project) {
    this.project = project
  }

  def setCalculator(calculator: CourseGradeCalculator) {
    this.calculator = calculator
  }
}
