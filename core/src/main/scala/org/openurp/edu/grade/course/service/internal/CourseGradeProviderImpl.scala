package org.openurp.edu.grade.course.service.internal

import java.util.ArrayList
import java.util.Collection
import java.util.List
import java.util.Map
import org.beangle.commons.collection.CollectUtils
import org.beangle.commons.dao.impl.BaseServiceImpl
import org.beangle.commons.dao.query.builder.OqlBuilder
import org.beangle.commons.lang.time.Stopwatch
import org.openurp.base.model.Semester
import org.openurp.edu.base.model.Student
import org.openurp.edu.grade.Grade
import org.openurp.edu.grade.course.model.CourseGrade
import org.openurp.edu.grade.course.service.CourseGradeProvider
//remove if not needed
import scala.collection.JavaConversions._

class CourseGradeProviderImpl extends BaseServiceImpl with CourseGradeProvider {

  def getPassedStatus(std: Student): Map[Long, Boolean] = {
    val query = OqlBuilder.from(classOf[CourseGrade], "cg")
    query.where("cg.std = :std", std)
    query.select("cg.course.id,cg.passed")
    val rs = entityDao.search(query).asInstanceOf[List[Array[Any]]]
    val courseMap = CollectUtils.newHashMap()
    for (obj <- rs) {
      val courseId = obj(0).asInstanceOf[java.lang.Long]
      if (null != obj(1)) {
        if (!courseMap.containsKey(courseId) || !courseMap.get(courseId)) {
          courseMap.put(courseId, obj(1).asInstanceOf[java.lang.Boolean])
        }
      } else {
        courseMap.put(courseId, false)
      }
    }
    courseMap
  }

  def getPublished(std: Student, semesters: Semester*): List[CourseGrade] = {
    val query = OqlBuilder.from(classOf[CourseGrade], "grade")
    query.where("grade.std = :std", std)
    query.where("grade.status =:status", Grade.Status.Published)
    if (null != semesters && semesters.length > 0) {
      query.where("grade.semester in(:semesters)", semesters)
    }
    query.orderBy("grade.semester.beginOn")
    entityDao.search(query)
  }

  def getAll(std: Student, semesters: Semester*): List[CourseGrade] = {
    val query = OqlBuilder.from(classOf[CourseGrade], "grade")
    query.where("grade.std = :std", std)
    if (null != semesters && semesters.length > 0) {
      query.where("grade.semester in(:semesters)", semesters)
    }
    query.orderBy("grade.semester.beginOn")
    entityDao.search(query)
  }

  def getPublished(stds: Collection[Student], semesters: Semester*): Map[Student, List[CourseGrade]] = {
    val sw = new Stopwatch()
    sw.start()
    val query = OqlBuilder.from(classOf[CourseGrade], "grade")
    query.where("grade.std in (:stds)", stds)
    query.where("grade.status =:status", Grade.Status.Published)
    if (null != semesters && semesters.length > 0) {
      query.where("grade.semester in(:semesters)", semesters)
    }
    val allGrades = entityDao.search(query)
    val gradeMap = CollectUtils.newHashMap()
    for (std <- stds) {
      gradeMap.put(std, new ArrayList[CourseGrade]())
    }
    for (g <- allGrades) gradeMap.get(g.std).add(g)
    logger.debug("Get {}'s grade using {}", stds.size, sw)
    gradeMap
  }

  def getAll(stds: Collection[Student], semesters: Semester*): Map[Student, List[CourseGrade]] = {
    val query = OqlBuilder.from(classOf[CourseGrade], "grade")
    query.where("grade.std in (:stds)", stds)
    if (null != semesters && semesters.length > 0) {
      query.where("grade.semester in(:semesters)", semesters)
    }
    val allGrades = entityDao.search(query)
    val gradeMap = CollectUtils.newHashMap()
    for (std <- stds) {
      gradeMap.put(std, new ArrayList[CourseGrade]())
    }
    for (g <- allGrades) gradeMap.get(g.std).add(g)
    gradeMap
  }
}
