/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2017
 * Software Technology Group
 * Department of Computer Science
 * Technische Universität Darmstadt
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.opalj
package hermes
package queries

import java.util.{HashMap ⇒ JMap}
import scala.collection.JavaConversions._

import org.opalj.br.analyses.Project

/**
 * Extracts basic metric information.
 *
 * @author Michael Reif
 */
object Metrics extends FeatureQuery {
    /**
     * The unique ids of the extracted features.
     */
    override def featureIDs: Seq[String] = Seq(
        // FPC - fields per class
        "min FPC", // 0
        "max FPC", // 1
        "avg FPC", // 2
        // MPC - methods per class
        "min MPC", // 3
        "max MPC", // 4
        "avg MPC", // 5
        // CPP - classes per package
        "min CPP", // 6
        "max CPP", // 7
        "avg CPP", // 8
        // NOC - number of children
        "min NOC", // 9
        "max NOC", // 10
        "avg NOC" // 11
    )

    override def apply[S](
        projectConfiguration: ProjectConfiguration,
        project:              Project[S],
        rawClassFiles:        Traversable[(da.ClassFile, S)]
    ): TraversableOnce[Feature[S]] = {

        val classLocations = Array.fill(featureIDs.size)(new LocationsContainer[S])

        val fpc = MetricValueContainer(classLocations, 0, 1, 2)
        val mpc = MetricValueContainer(classLocations, 3, 4, 5)
        val cpp = MetricValueContainer(classLocations, 6, 7, 8)
        val noc = MetricValueContainer(classLocations, 9, 10, 11)

        val packageMap = new JMap[String, Int]()

        val classHierarchy = project.classHierarchy

        for {
            (classFile, source) ← project.projectClassFilesWithSources
            classLocation = ClassFileLocation(source, classFile)
        } {
            // simple counts
            fpc.update(classFile.fields.size, classLocation)
            mpc.update(classFile.methods.size, classLocation)
            noc.update(classHierarchy.directSubtypesOf(classFile.thisType).size, classLocation)

            // classes per package
            val pkg = classFile.thisType.packageName
            val previousCount = packageMap.get(pkg)
            if (previousCount == null)
                packageMap.put(pkg, 1)
            else
                packageMap.put(pkg, previousCount + 1)

            // depth of inheritance
        }

        //compute cpp, we need to have processed all class files before
        packageMap.values().foreach(cpp.update(_))

        // prepare metric values
        val values = Array[Int](
            fpc.getMin,
            fpc.getMax,
            fpc.computeAvg(),
            mpc.getMin,
            mpc.getMax,
            mpc.computeAvg(),
            cpp.getMin,
            cpp.getMax,
            cpp.computeAvg(),
            noc.getMin,
            noc.getMax,
            noc.computeAvg()
        )

        for { (featureID, featureIDIndex) ← featureIDs.iterator.zipWithIndex } yield {
            if (values(featureIDIndex) < classLocations(featureIDIndex).locations.size)
                println(" "+featureID+" : "+values(featureIDIndex)+" < "+classLocations(featureIDIndex).locations.size)
            Feature[S](featureID, values(featureIDIndex), classLocations(featureIDIndex).locations)
        }
    }
}

/**
 * Mutable container for metric values. It has to be instantiated with the first value.
 */
class MetricValueContainer[S](
        location: Array[LocationsContainer[S]],
        minIndex: Int,
        maxIndex: Int,
        avgIndex: Int
) {

    private[this] var min = Int.MaxValue
    private[this] var max = Int.MinValue
    private[this] var sum = 0
    private[this] var vCount_ = 0

    def getMin = if (min == Int.MaxValue) 0 else min

    def getMax = if (max == Int.MinValue) 0 else max

    def update(value: Int, classFileLocation: ClassFileLocation[S]): Unit = {
        if (value < min) {
            min = value
            location(minIndex) = new LocationsContainer[S]
            location(minIndex) += classFileLocation
        }

        if (value > max) {
            max = value
            location(maxIndex) = new LocationsContainer[S]
            location(maxIndex) += classFileLocation
        }

        sum += value
        location(avgIndex) += classFileLocation

        vCount_ += 1
    }

    def update(value: Int): Unit = {
        if (value < min) {
            min = value
        }

        if (value > max) {
            max = value
        }

        sum += value
        vCount_ += 1
    }

    def computeAvg(): Int = if (vCount_ > 0) sum / vCount_ else 0
}

object MetricValueContainer {

    def apply[S](
        locations: Array[LocationsContainer[S]],
        minIndex:  Int,
        maxIndex:  Int,
        avgIndex:  Int
    ): MetricValueContainer[S] = {
        new MetricValueContainer(locations, minIndex, maxIndex, avgIndex)
    }
}