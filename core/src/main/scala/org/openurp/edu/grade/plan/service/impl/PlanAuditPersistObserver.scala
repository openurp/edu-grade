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
package org.openurp.edu.grade.plan.service.impl

import java.time.Instant

import org.beangle.commons.collection.Collections
import org.beangle.commons.logging.Logging
import org.beangle.data.dao.{EntityDao, OqlBuilder}
import org.openurp.base.edu.model.{Course, Student}
import org.openurp.edu.grade.plan.domain.{GroupResultAdapter, PlanAuditContext}
import org.openurp.edu.grade.plan.model.{CourseAuditResult, GroupAuditResult, PlanAuditResult}

/**
 * 计划审核保存
 *
 */
class PlanAuditPersistObserver extends Logging {

  var entityDao: EntityDao = null

  def notifyBegin(context: PlanAuditContext, index: Int): Boolean = true

  private def result(std: Student): Option[PlanAuditResult] = {
    val query = OqlBuilder.from(classOf[PlanAuditResult], "planResult")
    query.where("planResult.std = :std", std)
    entityDao.search(query).headOption
  }

  def notifyEnd(context: PlanAuditContext, index: Int) : Unit = {
    val newResult = context.result
    var existedResult = result(context.std).orNull
    if (null != existedResult) {
      existedResult.remark = newResult.remark
      existedResult.updatedAt = Instant.now
      val existedcreditsCompleted = existedResult.auditStat.passedCredits
      existedResult.auditStat = newResult.auditStat
      var updatePassed = true
      val existedPassed = existedResult.passed
      if (!existedResult.archived) {
        updatePassed = false
        if (existedResult.passed &&
          newResult.auditStat.passedCredits < existedcreditsCompleted) {
          updatePassed = true
        }
      }
      val updates = new StringBuilder()
      mergeGroupResult(existedResult, new GroupResultAdapter(existedResult), new GroupResultAdapter(newResult),
        updates)
      if (!updatePassed) {
        existedResult.passed = (existedPassed)
      } else {
        existedResult.passed = (newResult.passed)
      }
      // delete last ';'
      if (updates.nonEmpty) updates.deleteCharAt(updates.length - 1)
      existedResult.updates = Some(updates.toString)
    } else {
      existedResult = newResult
    }
    entityDao.saveOrUpdate(existedResult)
    context.result = existedResult
  }

  private def mergeGroupResult(
                                existedResult: PlanAuditResult,
                                target: GroupAuditResult,
                                source: GroupAuditResult,
                                updates: StringBuilder) : Unit = {
    // 统计完成学分的变化
    val delta = source.auditStat.passedCredits - target.auditStat.passedCredits
    if (java.lang.Float.compare(delta, 0) != 0) {
      updates.append(source.name)
      if (delta > 0) updates.append('+').append(delta) else updates.append(delta)
      updates.append(';')
    }
    target.auditStat = source.auditStat
    target.passed = source.passed
    target.indexno = source.indexno
    // 收集课程组[groupName->groupResult]
    val targroupResults = Collections.newMap[String, GroupAuditResult]
    val sourceGroupResults = Collections.newMap[String, GroupAuditResult]
    for (result <- target.children) targroupResults.put(result.name, result)
    for (result <- source.children) sourceGroupResults.put(result.name, result)

    // 收集课程结果[course->courseResult]
    val tarcourseResults = Collections.newMap[Course, CourseAuditResult]
    val sourceCourseResults = Collections.newMap[Course, CourseAuditResult]
    for (courseResult <- target.courseResults) tarcourseResults.put(courseResult.course, courseResult)
    for (courseResult <- source.courseResults) sourceCourseResults.put(courseResult.course, courseResult)

    // 删除没有的课程组
    val removed = Collections.subtract(targroupResults.keySet, sourceGroupResults.keySet)
    for (groupName <- removed) {
      val gg = targroupResults(groupName)
      gg.detach()
      target.removeChild(gg)
    }
    // 添加课程组
    val added = Collections.subtract(sourceGroupResults.keySet, targroupResults.keySet)
    for (groupName <- added) {
      val groupResult = sourceGroupResults.get(groupName).asInstanceOf[GroupAuditResult]
      target.addChild(groupResult)
      groupResult.attachTo(existedResult)
    }
    // 合并课程组
    val common = Collections.intersection(sourceGroupResults.keySet, targroupResults.keySet)
    for (groupName <- common) {
      mergeGroupResult(existedResult, targroupResults(groupName), sourceGroupResults(groupName),
        updates)
    }
    // ------- 合并课程结果
    // 删除没有的课程
    val removedCourses = Collections.subtract(tarcourseResults.keySet, sourceCourseResults.keySet)
    for (course <- removedCourses) {
      val courseResult = tarcourseResults(course)
      target.courseResults -= courseResult
    }
    // 添加新的课程结果
    val addedCourses = Collections.subtract(sourceCourseResults.keySet, tarcourseResults.keySet)
    for (course <- addedCourses) {
      val courseResult = sourceCourseResults(course)
      courseResult.groupResult.courseResults -= courseResult
      courseResult.groupResult = target
      target.courseResults += courseResult
    }
    // 合并共同的课程
    val commonCourses = Collections.intersection(sourceCourseResults.keySet, tarcourseResults.keySet)
    for (course <- commonCourses) {
      val targetCourseResult = tarcourseResults(course)
      val sourceCourseResult = sourceCourseResults(course)
      targetCourseResult.passed = sourceCourseResult.passed
      targetCourseResult.scores = sourceCourseResult.scores
      targetCourseResult.compulsory = sourceCourseResult.compulsory
      targetCourseResult.remark = sourceCourseResult.remark
    }
  }
}
