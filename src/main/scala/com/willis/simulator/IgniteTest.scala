package com.willis.simulator

import scala.collection.mutable.{ ListBuffer, PriorityQueue }

case class MageState(mage: Mage, castTime: Double = 1 * Math.random(), castNumber: Int = 0, mqgActivated: Double = -21,
  spell: String = "", combustionStacks: Int = -1, combustionMult: Int = 0, lastFireblast: Double = -7.5)

case class TrialSetup(mageSetup: MageSetup, mageStates: List[MageState])

//spark row classes
case class ResultRow(mageSetup: MageSetup, totalDmg: Int, igniteDmg: Int, igniteTimes: ListBuffer[Double])
case class SetupMetric(mageSetup: MageSetup, avgDmg: Double, avgIgniteDmg: Double)
case class FinalRowFormat(numMages: Int, nightfall: Boolean, darkmoon: Boolean, fightLength: Int, numPIs: Int,
  numWB: Int, semiWB: Int, rotation: String, dps: Double, igniteDps: Double)

case class FinalRowFormatPercent(numMages: Int, nightfall: Boolean, darkmoon: Boolean, fightLength: Int, numPIs: Int,
  numWB: Int, semiWB: Int, rotation: String, dps: Double, igniteDps: Double,
  percentDpsIncrease: Double, percentIgniteDpsIncrease: Double)

object IgniteTest {

  val NUM_TRIALS = 50000

  var maxTick = 0.0

  def main(args: Array[String]): Unit = {
    //
    //        implicit val spark = SparkSession.builder().appName("Agg Script For Playtesting")
    //          .master(Config.sparkMaster)
    //          .getOrCreate()
    //
    //        import spark.implicits._
    //
    //
    //    println(magesMap.size)
    //
    //        val dataset = spark.createDataset(magesMap.toList).repartition(1000).flatMap {
    //          case (mageSetup, mages) =>
    //            List.fill(NUM_TRIALS) {
    //              val mageStates = mages.map(mage => MageState(mage))
    //              TrialSetup(mageSetup, mageStates)
    //            }
    //        }.map(ts => simulateFight(ts.mageStates, ts.mageSetup))
    //          .groupBy($"mageSetup")
    //          .agg(mean($"totalDmg") as "avgDmg", mean($"igniteDmg") as "avgIgniteDmg").as[SetupMetric]
    //          .map(m => FinalRowFormat(m.mageSetup.numMages, m.mageSetup.nightfall, m.mageSetup.dmf, m.mageSetup.fightLength,
    //            m.mageSetup.numPIs, m.mageSetup.numWB, m.mageSetup.semiWB, m.mageSetup.rotation.name,
    //            m.avgDmg / m.mageSetup.fightLength, m.avgIgniteDmg / m.mageSetup.fightLength))
    //
    //        val frostbolt3 = dataset.filter(_.rotation == Rotations.FROSTBOLT3.name).withColumnRenamed("dps", "fb3dps")
    //          .withColumnRenamed("igniteDps", "ignitefb3dps").withColumnRenamed("rotation", "fb3Rotation")
    //
    //        dataset.join(frostbolt3, Seq("numMages", "nightfall", "darkmoon", "fightLength", "numPIs", "numWB", "semiWB"))
    //          .withColumn("percentDpsIncrease", $"dps" / $"fb3dps")
    //          .withColumn("percentIgniteDpsIncrease", $"igniteDps" / $"ignitefb3dps")
    //          .as[FinalRowFormatPercent]
    //          .repartition(1)
    //          .write.mode(SaveMode.Overwrite).csv("gs://unity-ai-playtesting-data-stg/willis/simulation")

    val magesMap = MageSetups.genMageSetups(21, 98, 750)

    val setups = List[MageSetup](MageSetup(8, 8, 0, 5, 21, 98, 750, true, true, 105, Rotations.FROSTBOLT3))

    for (setup <- setups) {
      val mages = magesMap(setup)

      val trials = List.fill(NUM_TRIALS) {
        val mageStates = mages.map(mage => MageState(mage))

        simulateFight(mageStates, setup)
      }

      val avgDps = trials.map(_.totalDmg.toDouble / NUM_TRIALS).sum / setup.fightLength
      val sortedDps = trials.map(_.totalDmg.toDouble / setup.fightLength).sorted
      val firstQuartile = sortedDps(trials.size / 4)
      val median = sortedDps(trials.size / 2)
      val thirdQuartile = sortedDps(trials.size - trials.size / 4)
      val avgIgniteDmg = trials.map(_.igniteDmg.toDouble / NUM_TRIALS).sum
      val dpsStdDev = Math.sqrt((trials.map(xi => math.pow(xi.totalDmg.toDouble / setup.fightLength - avgDps, 2)).sum) / NUM_TRIALS)
      val maxIgniteDrops = trials.map(_.igniteTimes.size).max
      val igniteDrops = trials.map(_.igniteTimes).map(x => x.toList ++ List.fill(maxIgniteDrops - x.size)(setup.fightLength.toDouble))
        .reduce((l, r) => l.zip(r).map(z => z._2 + z._1)).map(_ / NUM_TRIALS)

      println(maxTick)

      println(setup.rotation)
      println(dpsStdDev)
      println(s"Boxplot values: ${sortedDps.head}, $firstQuartile, $median, $avgDps, $thirdQuartile, ${sortedDps.last}")
      println(s"Confidence interval 99%: ${avgDps} +/- ${2.576 * dpsStdDev / math.sqrt(NUM_TRIALS)}")
      println(avgIgniteDmg / setup.fightLength)
      println(igniteDrops)

    }

  }

  case class FVulnState(stacks: Int = 0, refresh: Double = 0)
  case class IgniteState(lastCrit: Double = 0, igniteTick: Double = Double.MaxValue,
    igniteSize: Double = 0, igniteStacks: Int = 0, dipMultiplier: Double = 1)

  def updateIgnite(castDmg: Double, castTime: Double, fireCrit: Boolean, multiplier: Double,
    igniteState: IgniteState): (Double, IgniteState) = {

    val shouldTick = castTime > igniteState.igniteTick
    val dmg = if (shouldTick) igniteState.igniteSize * igniteState.dipMultiplier else 0
    maxTick = math.max(dmg, maxTick)
    val nextTick = if (shouldTick) igniteState.igniteTick + 2 else igniteState.igniteTick

    if (fireCrit) {
      if (igniteState.igniteStacks == 0) {
        (dmg, IgniteState(castTime, castTime, (castDmg * 1.5 * .2).toInt, 1, multiplier))
      } else if (igniteState.igniteStacks < 5) {
        (dmg, IgniteState(castTime, nextTick, igniteState.igniteSize + (castDmg * 1.5 * .2).toInt,
          igniteState.igniteStacks + 1, multiplier))
      } else {
        (dmg, igniteState.copy(castTime, nextTick))
      }
    } else {
      (dmg, igniteState.copy(igniteTick = nextTick))
    }
  }

  def updateCombustion(mageState: MageState, fireCrit: Boolean, numActiveCombustion: Int, staggerCombustion: Boolean): (Int, MageState) = {
    //Activate combustion
    val stacks = mageState.combustionStacks
    val mult = mageState.combustionMult

    if (stacks == -1 && mageState.castNumber > 4) {
      if (numActiveCombustion == 0 && staggerCombustion) {
        (1, mageState.copy(combustionMult = 1, combustionStacks = 3))
      } else if (!staggerCombustion) {
        (numActiveCombustion + 1, mageState.copy(combustionMult = 1, combustionStacks = 3))
      } else {
        (numActiveCombustion, mageState)
      }
    } else if (stacks > 0) {
      if (fireCrit && stacks == 1) {
        (numActiveCombustion - 1, mageState.copy(combustionMult = 0, combustionStacks = 0))
      } else if (fireCrit) {
        (numActiveCombustion, mageState.copy(combustionMult = mult + 1, combustionStacks = stacks - 1))
      } else {
        (numActiveCombustion, mageState.copy(combustionMult = mult + 1))
      }
    } else {
      if (mageState.castNumber == 2 && mageState.mage.hasPI) {
        (numActiveCombustion + 1, mageState.copy(combustionMult = 1, combustionStacks = 3))
      } else {
        (numActiveCombustion, mageState)
      }
    }
  }

  def simulateFight(mageStates: List[MageState], mageSetup: MageSetup): ResultRow = {
    val pq = PriorityQueue.empty[MageState](Ordering.by(_.castTime * -1))
    pq ++= mageStates

    var curMageState = pq.dequeue()
    var igniteDropList = ListBuffer.empty[Double]

    var igniteState = IgniteState()
    var fVulnState = FVulnState()

    val staggerCombustion = if (mageSetup.rotation.name == Rotations.STAGGER_COMBUSTION.name) true else false
    val fireblastWeaving = if (mageSetup.rotation.name == Rotations.FIREBLAST_WEAVING.name) true else false

    var numActiveCombustion = 0

    var totalIgniteDmg = 0
    var totalCastDmg = 0
    var piActive = -15.0

    var nightfall = if (math.random < .3 && mageSetup.nightfall) 5 else -10
    val dmf = if (mageSetup.dmf) 1.21 else 1.1

    //debugging
    var mqgCasts = 0
    var scorchCasts = 0
    var fireballCasts = 0

    while (curMageState.castTime < mageSetup.fightLength) {

      val piMultiplier = if (piActive < curMageState.castTime &&
        piActive + 15 > curMageState.castTime && curMageState.mage.hasPI) {
        1.2
      } else {
        1
      }

      val nightfallMultiplier = if (nightfall < curMageState.castTime && nightfall + 4 > curMageState.castTime) 1.2 else 1

      //does ignite drop
      if (igniteState.lastCrit + 4 < curMageState.castTime && igniteState.igniteStacks > 0) {
        //        println(s"dropped ignite at ${lastCrit + 4}")

        if (igniteState.lastCrit > 6) {
          igniteDropList += igniteState.lastCrit + 4
        }
        totalIgniteDmg = totalIgniteDmg + igniteState.igniteSize.toInt
        igniteState = IgniteState()
      }

      //Resolve spell
      val fireCast = Rotations.FIRE_SPELLS.contains(curMageState.spell)
      val spellHit = hit(curMageState.mage.probHit)
      var spellCrit = false

      val castDmg = if (!spellHit) {
        0
      } else if (curMageState.spell == Rotations.SCORCH) {
        scorchCasts += 1
        val stacks = fVulnState.stacks
        fVulnState = FVulnState(math.min(stacks + 1, 5), curMageState.castTime)
        if (crit(curMageState.mage.probCrit + .04 + curMageState.combustionMult * .1)) {
          spellCrit = true
        }
        curMageState.mage.scorch() * (1 + stacks * .03)
      } else if (curMageState.spell == Rotations.FROSTBOLT) {
        if (crit(curMageState.mage.probCrit - .06)) {
          spellCrit = true
        }
        curMageState.mage.frostbolt()
      } else if (curMageState.spell == Rotations.FIREBALL) {
        fireballCasts += 1
        if (crit(curMageState.mage.probCrit + curMageState.combustionMult * .1)) {
          spellCrit = true
        }
        curMageState.mage.fireball() * (1 + fVulnState.stacks * .03)
      } else if (curMageState.spell == Rotations.FIREBLAST) {
        if (crit(curMageState.mage.probCrit + .04)) {
          spellCrit = true
        }
        curMageState.mage.fireblast() * (1 + fVulnState.stacks * .03)
      } else {
        0
      }

      val multCastDmg = castDmg * piMultiplier * dmf * nightfallMultiplier

      val castDmgWithCrit = if (spellCrit) multCastDmg * 1.5 else multCastDmg

      totalCastDmg = totalCastDmg + castDmgWithCrit.toInt

      val igniteUpdate = updateIgnite(multCastDmg, curMageState.castTime, fireCast && spellCrit, piMultiplier * dmf, igniteState)

      totalIgniteDmg = totalIgniteDmg + igniteUpdate._1.toInt
      igniteState = igniteUpdate._2

      val combustionUpdate = updateCombustion(curMageState, fireCast && spellCrit, numActiveCombustion, staggerCombustion)

      numActiveCombustion = combustionUpdate._1
      curMageState = combustionUpdate._2

      //choose next spell and create next mage state

      val mageRotation = curMageState.mage.rotation
      var nextSpell = if (fVulnState.refresh + 26 < curMageState.castTime && curMageState.mage.mageId == mageStates.size) {
        Rotations.SCORCH
      } else {
        mageRotation(Math.min(mageRotation.size - 1, curMageState.castNumber))
      }

      val gcdIncurred = if (curMageState.spell == Rotations.FIREBLAST) 1.5 else 0

      val mqgActivated = if (curMageState.mqgActivated == -21 && curMageState.castTime > curMageState.mage.mqgStart
        && nextSpell != Rotations.SCORCH && nextSpell != Rotations.FIREBLAST && gcdIncurred == 0) {
        curMageState.castTime
      } else {
        curMageState.mqgActivated
      }

      if (mqgActivated + 20 < curMageState.castTime && igniteState.igniteStacks == 5
        && curMageState.combustionStacks <= 0 && curMageState.castNumber > 4) {
        if (curMageState.mage.scorchMage && igniteState.lastCrit + 4 > curMageState.castTime + 1.5) {
          nextSpell = Rotations.SCORCH
        } else if (fireblastWeaving && igniteState.lastCrit + 4 > curMageState.castTime
          && nextSpell != Rotations.SCORCH && curMageState.lastFireblast + 7.5 < curMageState.castTime) {
          nextSpell = Rotations.FIREBLAST
        }
      }

      if (piActive == -15 && curMageState.mage.hasPI && nextSpell == Rotations.FIREBALL) {
        piActive = curMageState.castTime
      }

      val nextCast = if (nextSpell == Rotations.SCORCH) {
        curMageState.castTime + 1.5 + gcdIncurred
      } else if (nextSpell == Rotations.FIREBLAST) {
        curMageState.castTime
      } else if (mqgActivated + 20 > curMageState.castTime) {
        mqgCasts += 1
        curMageState.castTime + 2.25 + gcdIncurred
      } else {
        curMageState.castTime + 3 + gcdIncurred
      }

      val fireblastCast = if (nextSpell == Rotations.FIREBLAST) curMageState.castTime else curMageState.lastFireblast

      pq += curMageState.copy(castTime = nextCast, spell = nextSpell, castNumber = curMageState.castNumber + 1,
        mqgActivated = mqgActivated, lastFireblast = fireblastCast)
      curMageState = pq.dequeue()

    }

    val lastIgniteTicks = if (igniteState.igniteTick < mageSetup.fightLength - 2) igniteState.igniteSize * 2 else if (igniteState.igniteTick < mageSetup.fightLength) igniteState.igniteSize else 0

    totalIgniteDmg = totalIgniteDmg + lastIgniteTicks.toInt
    val totalDmg = totalIgniteDmg + totalCastDmg

    //    println(scorchCasts)
    //    println(mqgCasts)
    //    println(totalDmg)
    ResultRow(mageSetup, totalDmg, totalIgniteDmg, igniteDropList)
  }

  def crit(probCrit: Double): Boolean = {
    Math.random() <= probCrit
  }

  def hit(probHit: Double): Boolean = {
    Math.random() <= probHit
  }

}
