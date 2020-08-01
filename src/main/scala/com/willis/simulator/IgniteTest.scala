package com.willis.simulator

import org.apache.spark.sql.{ SaveMode, SparkSession }
import org.apache.spark.sql.functions.{ max, mean, stddev }

import scala.collection.mutable.{ ListBuffer, PriorityQueue }

case class MageState(mage: Mage, castTime: Double = .5 * Math.random(), castNumber: Int = 0, mqgActivated: Double = -21,
  spell: String = "", combustionStacks: Int = -1, combustionMult: Int = 0, lastFireblast: Double = -7.5, toep: Double = -15)

case class TrialSetup(mageSetup: MageSetup, mageStates: List[MageState])

object IgniteTest {

  //add travel time
  //add combustion batching
  //add second trinket usage

  val NUM_TRIALS = 10000

  //debugging
  var maxTick = 0.0
  var biggestCrit = 0.0
  var avgMultiplier = 0.0
  var multiplierCount = 0
  var maxMultiplier = 0.0
  var firstBigIgnite = false
  var notPICount = 0

  //debugging
  var mqgCasts = 0
  var fireballCasts = 0
  var scorchCasts = 0
  var fireballDPS = 0.0
  var fireballDPSWithCrit = 0.0
  var numTicks = 0.0

  //spark row classes
  case class ResultRow(mageSetup: MageSetup, totalDmg: Double, igniteDmg: Double, weightedTicks: Double, igniteTimes: ListBuffer[Double])
  //  case class SetupMetric(mageSetup: MageSetup, avgDmg: Double, avgIgniteDmg: Double)
  case class FinalRowFormat(numMages: Int, nightfall: Boolean, darkmoon: Boolean, fightLength: Int, numPIs: Int,
    numWB: Int, semiWB: Int, rotation: String, dps: Double, igniteDps: Double, weightedTicks: Double, stdDev: Double,
    boxPlot: String, igniteDrops: String)

  case class FinalRowFormatPercent(numMages: Int, nightfall: Boolean, darkmoon: Boolean, fightLength: Int, numPIs: Int,
    numWB: Int, semiWB: Int, rotation: String, dps: Double, igniteDps: Double, weightedTicks: Double, stdDev: Double,
    boxPlot: String, igniteDrops: String, percentDpsIncrease: Double, percentIgniteDpsIncrease: Double)

  def main(args: Array[String]): Unit = {

    val magesMap = MageSetups.genMageSetups(21, 99, 863)

    val testMap = magesMap.filter(setup => setup._1.dmf == false && setup._1.nightfall == false)

//    println(testMap.size)
//
//    implicit val spark = SparkSession.builder().appName("Agg Script For Playtesting")
//      .master("yarn")
//      .getOrCreate()
//
//    spark.conf.set("spark.sql.autoBroadcastJoinThreshold", -1)
//    spark.conf.set("spark.sql.broadcastTimeout", 3000)
//
//    import spark.implicits._
//
//    println(magesMap.size)
//
//    val dataset = spark.createDataset(testMap.toList).repartition(1000)
//      .map {
//        case (setup, mages) =>
//          val trialSetups = List.fill(NUM_TRIALS) {
//            setupMageStates(setup, mages)
//          }
//          (setup, trialSetups)
//      }.map {
//        case (setup, trialSetups) =>
//          val trials = trialSetups.map(ts => simulateFight(ts.mageStates, ts.mageSetup))
//          createMetrics(setup, trials)
//      }.cache()
//
//    val standard = dataset.filter(_.rotation == Rotations.FIREBALL_DROP.name).withColumnRenamed("dps", "fbdps")
//      .withColumnRenamed("igniteDps", "ignitefbdps").withColumnRenamed("rotation", "fbRotation")
//      .drop("stdDev", "boxPlot", "igniteDrops", "weightedTicks")
//
//    dataset
//      .join(standard, Seq("numMages", "nightfall", "darkmoon", "fightLength", "numPIs", "numWB", "semiWB"))
//      .withColumn("percentDpsIncrease", $"dps" / $"fbdps")
//      .withColumn("percentIgniteDpsIncrease", $"igniteDps" / $"ignitefbdps")
//      .as[FinalRowFormatPercent].drop("fbdps", "ignitefbdps", "fbRotation")
//      .repartition(1)
//      .write.option("header", "true").mode(SaveMode.Overwrite)
//      //      .csv("Simulation")
//      .csv("gs://unity-analytics-blender-data-prd/willis/test_run")

    val setups = Rotations.rotations.flatMap{ x =>
      List(
//            MageSetup(7, 7, 0, 2, 21, 99, 750, false, false, 100, x),
        MageSetup(7, 1, 5, 2, 21, 99, 863, false, false, 100, x)
      )
    }.sortBy(_.rotation.name)



//        val setups = List[MageSetup](MageSetup(7, 7, 0, 2, 21, 99, 750, true, true, 105, Rotations.FIREBALL_DROP),
//          MageSetup(7, 7, 0, 2, 21, 99, 750, true, true, 105, Rotations.FROSTBOLT2))

    for (setup <- setups) {
      val mages = magesMap(setup)

      val trials = List.fill(NUM_TRIALS) {
        val ts = setupMageStates(setup, mages)

        simulateFight(ts.mageStates, ts.mageSetup)
      }

      //      println(maxTick)
      //      println(biggestCrit)
      //      println(maxMultiplier)
      //      println(avgMultiplier / multiplierCount)
      //      println(multiplierCount)
      //      println(notPICount)

//          println(s"Num ticks equals: ${numTicks/NUM_TRIALS}")

      numTicks = 0
      avgMultiplier = 0
      multiplierCount = 0
      maxMultiplier = 0.0
      notPICount = 0

      val rowMetric = createMetrics(setup, trials)

      //      println(mqgCasts.toDouble / NUM_TRIALS)
      //      println(fireballCasts.toDouble / NUM_TRIALS)
      //      println(scorchCasts.toDouble / NUM_TRIALS)

      println(setup)
      //      println(dpsStdDev)
      println(rowMetric.boxPlot)
      println(s"Confidence interval 99%: ${rowMetric.dps} +/- ${(2.576 * rowMetric.stdDev / math.sqrt(NUM_TRIALS)).round}")
      println(rowMetric.igniteDps)
      println(rowMetric.weightedTicks)
      println(rowMetric.igniteDrops)

    }

  }

  def setupMageStates(setup: MageSetup, mages: List[Mage]): TrialSetup = {

    val mageStates = mages.map { mage =>
      val startTime = if (!mage.hasPI || mage.mageId > mages.size / 2) 2 * math.random + .25 else .5 * math.random

      if (mage.hasPI && (setup.rotation.name == Rotations.PRE_COMBUSTION
        || (mage.mageId == 0 && setup.rotation.name == Rotations.PYRO_START.name))) {
        MageState(mage, startTime, combustionMult = 3, combustionStacks = 2)
      } else if (mage.hasPI && setup.rotation.name != Rotations.FROSTBOLT2_HOLD_COMBUSTION.name) {
        MageState(mage, startTime, combustionMult = 1, combustionStacks = 3)
      } else {
        MageState(mage, startTime)
      }
    }
    TrialSetup(setup, mageStates)
  }

  def createMetrics(setup: MageSetup, trials: List[ResultRow]): FinalRowFormat = {
    val avgDps = (trials.map(_.totalDmg / NUM_TRIALS).sum).round
    val sortedDps = trials.map(_.totalDmg).sorted
    val firstQuartile = sortedDps(trials.size / 4).round
    val median = sortedDps(trials.size / 2).round
    val thirdQuartile = sortedDps(trials.size - trials.size / 4).round
    val avgIgniteDps = trials.map(_.igniteDmg / NUM_TRIALS).sum
    val avgIgniteTicks = trials.map(_.weightedTicks / NUM_TRIALS).sum
    val dpsStdDev = Math.sqrt((trials.map(xi => math.pow(xi.totalDmg - avgDps, 2)).sum) / NUM_TRIALS)
    val maxIgniteDrops = trials.map(_.igniteTimes.size).max
    val igniteDrops = trials.map(_.igniteTimes).map(x => x.toList ++ List.fill(maxIgniteDrops - x.size)(setup.fightLength.toDouble))
      .reduce((l, r) => l.zip(r).map(z => z._2 + z._1)).map(_ / NUM_TRIALS)

    val boxPlot = s"Boxplot values: ${sortedDps.head.round}, $firstQuartile, $median, $avgDps, $thirdQuartile, ${sortedDps.last.round}"
    val igniteDropString = s"Ignite Drop times on avg $igniteDrops"

    FinalRowFormat(setup.numMages, setup.nightfall, setup.dmf, setup.fightLength, setup.numPIs, setup.numWB, setup.semiWB,
      setup.rotation.name, avgDps, avgIgniteDps.round, avgIgniteTicks, dpsStdDev, boxPlot, igniteDropString)
  }

  case class FVulnState(stacks: Int = 0, refresh: Double = 0)
  case class IgniteState(lastCrit: Double = 0, igniteTick: Double = Double.MaxValue, igniteSize: Double = 0,
    igniteStacks: Int = 0, dipMultiplier: Double = 1, numPIFireball: Int = 0, weightedTicks: Double = 0)

  def weightTick(igniteState: IgniteState): Double = {
    igniteState.dipMultiplier * (igniteState.igniteStacks + igniteState.numPIFireball.toDouble * .2) / 5
  }

  def updateIgnite(castDmg: Double, castTime: Double, fireCrit: Boolean, multiplier: Double,
    igniteState: IgniteState): (Double, IgniteState) = {

    val shouldTick = castTime > igniteState.igniteTick && igniteState.igniteTick < igniteState.lastCrit + 4
    val dmg = if (shouldTick) igniteState.igniteSize * igniteState.dipMultiplier else 0
    val weightedTicks = if (shouldTick) {
      numTicks += 1
      igniteState.weightedTicks + weightTick(igniteState)
    } else igniteState.weightedTicks
    maxTick = math.max(dmg, maxTick)
    val nextTick = if (shouldTick) igniteState.igniteTick + 2 else igniteState.igniteTick

    if (fireCrit) {
      if (igniteState.igniteStacks == 0) {
        if (castDmg > 1500 && !firstBigIgnite) {
          firstBigIgnite = true
          avgMultiplier += multiplier
          multiplierCount += 1
        }
        maxMultiplier = math.max(maxMultiplier, multiplier)
        (dmg, IgniteState(castTime, castTime + 2, (castDmg * 1.5 * .2), 1, multiplier, if (multiplier > 1.5) 1 else 0))
      } else if (igniteState.igniteStacks < 5) {
        val numPIFireballs = if (multiplier > 1.5) igniteState.numPIFireball + 1 else igniteState.numPIFireball
        (dmg, igniteState.copy(castTime, nextTick, igniteState.igniteSize + (castDmg * 1.5 * .2),
          igniteState.igniteStacks + 1, weightedTicks = weightedTicks, numPIFireball = numPIFireballs))
      } else {
        (dmg, igniteState.copy(castTime, nextTick, weightedTicks = weightedTicks))
      }
    } else {
      (dmg, igniteState.copy(igniteTick = nextTick, weightedTicks = weightedTicks))
    }
  }

  def updateCombustion(mageState: MageState, fireCrit: Boolean, numActiveCombustion: Int, staggerCombustion: Boolean): (Int, MageState) = {
    //Activate combustion
    val stacks = mageState.combustionStacks
    val mult = mageState.combustionMult

    if (stacks == -1 && mageState.castNumber > 4) {
      if (numActiveCombustion <= 2 && staggerCombustion) {
        (numActiveCombustion + 1, mageState.copy(combustionMult = 1, combustionStacks = 3))
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
      if (mageState.castNumber == 1 && mageState.mage.hasPI && mageState.combustionStacks == -1) {
        (numActiveCombustion + 1, mageState.copy(combustionMult = 1, combustionStacks = 3))
      } else {
        (numActiveCombustion, mageState)
      }
    }
  }

  def simulateFight(mageStates: List[MageState], mageSetup: MageSetup): ResultRow = {
    val pq = PriorityQueue.empty[MageState](Ordering.by(_.castTime * -1))
    pq ++= mageStates

    val fightLength = mageSetup.fightLength - 5 + math.random * 10

    var curMageState = pq.dequeue()
    var igniteDropList = ListBuffer.empty[Double]

    var igniteState = IgniteState()
    var fVulnState = FVulnState()
    firstBigIgnite = false

    val staggerCombustion = Rotations.STAGGER.contains(mageSetup.rotation.name)
    val fireblastWeaving = Rotations.WEAVING.contains(mageSetup.rotation.name == Rotations.FIREBLAST_WEAVING.name)
    val improvedFrostbolt = mageSetup.rotation.name == Rotations.IMPROVED_FROSTBOLT.name
    val fireballR11 = mageSetup.rotation.name == Rotations.FIREBALL_R11.name || mageSetup.rotation.name == Rotations.PYRO_R11.name

    var numActiveCombustion = mageSetup.numPIs

    var totalIgniteDmg = 0
    var totalCastDmg = 0
    var totalWeightedTicks = 0.0
    var piStart = -15.0

    val nightfall = if (math.random < .4 && mageSetup.nightfall) 6.5 else -10
    val dmf = if (mageSetup.dmf) 1.1 else 1
    val coe = 1.1

    while (curMageState.castTime < fightLength) {

      val piMultiplier = calcPI(piStart, curMageState)

      val fVulnMultiplier = 1 + fVulnState.stacks * .03

      val nightfallMultiplier = if (nightfall < curMageState.castTime && nightfall + 4 > curMageState.castTime) 1.15 else 1

      val toepDmg = if (curMageState.castTime < curMageState.toep + 15 && curMageState.castTime > curMageState.toep) 175 else 0

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
        if (crit(curMageState.mage.probCrit + .04)) {
          spellCrit = true
        }
        curMageState.mage.scorch() * (fVulnMultiplier)
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
        if (fireballR11) {
          curMageState.mage.fireballr11() * fVulnMultiplier
        } else {
          curMageState.mage.fireball() * fVulnMultiplier
        }
      } else if (curMageState.spell == Rotations.FIREBLAST) {
        if (crit(curMageState.mage.probCrit + .04)) {
          spellCrit = true
        }
        (curMageState.mage.fireblast() + toepDmg * 1.1) * fVulnMultiplier
      } else if (curMageState.spell == Rotations.PYROBLAST) {
        if (crit(curMageState.mage.probCrit + curMageState.combustionMult * .1)) {
          spellCrit = true
        }
        (curMageState.mage.pyroblast() + toepDmg * 1.1) * fVulnMultiplier
      } else {
        0
      }

      val multCastDmg = castDmg * piMultiplier * coe * dmf * nightfallMultiplier

      val castDmgWithCrit = if (spellCrit) multCastDmg * 1.5 else multCastDmg

      if (curMageState.spell == Rotations.FIREBALL) {
        fireballDPS += multCastDmg / fightLength
        fireballDPSWithCrit += castDmgWithCrit / fightLength
      }

      biggestCrit = math.max(biggestCrit, castDmgWithCrit)

      totalCastDmg = totalCastDmg + castDmgWithCrit.toInt

      //does ignite drop
      if (igniteState.lastCrit < igniteState.igniteTick - 2 && igniteState.igniteTick < curMageState.castTime && igniteState.igniteStacks > 0) {
        //      if(igniteState.igniteStacks > 0 && igniteState.lastCrit + 4 < curMageState.castTime) {
        //        println(s"dropped ignite at ${lastCrit + 4}")

        if (igniteState.lastCrit > 6) {
          totalWeightedTicks += igniteState.weightedTicks
          igniteDropList += igniteState.lastCrit + 4
        }
        igniteState = IgniteState()
      }

      val igniteUpdate = updateIgnite(multCastDmg, curMageState.castTime, fireCast && spellCrit, piMultiplier * dmf * coe * fVulnMultiplier, igniteState)

      totalIgniteDmg = totalIgniteDmg + igniteUpdate._1.toInt
      igniteState = igniteUpdate._2

      if (fireCast) {
        val combustionUpdate = updateCombustion(curMageState, fireCast && spellCrit, numActiveCombustion, staggerCombustion)

        numActiveCombustion = combustionUpdate._1
        curMageState = combustionUpdate._2
      }
      //choose next spell and create next mage state

      val mageRotation = curMageState.mage.rotation
      var nextSpell = if (fVulnState.refresh + 26 < curMageState.castTime && curMageState.mage.mageId == mageStates.size - 1) {
        Rotations.SCORCH
      } else {
        mageRotation(Math.min(mageRotation.size - 1, curMageState.castNumber))
      }

      val gcdIncurred = if (curMageState.spell == Rotations.FIREBLAST) 1.5 else 0

      val mqgActivated = if (curMageState.mqgActivated == -21 && curMageState.castTime > curMageState.mage.mqgStart
        && nextSpell != Rotations.SCORCH && nextSpell != Rotations.FIREBLAST && gcdIncurred == 0) {
        curMageState.castTime
      } else if (curMageState.mqgActivated == -21 && nextSpell == Rotations.PYROBLAST && curMageState.mage.hasPI && gcdIncurred == 0) {
        curMageState.castTime
      } else {
        curMageState.mqgActivated
      }

      // && igniteState.lastCrit + 4 > curMageState.castTime + 1.5
      if (mqgActivated + 20 < curMageState.castTime && igniteState.igniteStacks == 5
        && curMageState.combustionStacks <= 0) {
        if (curMageState.mage.scorchMage) {
          nextSpell = Rotations.SCORCH
        } else if (fireblastWeaving && nextSpell != Rotations.SCORCH && curMageState.lastFireblast + 7.5 < curMageState.castTime) {
          nextSpell = Rotations.FIREBLAST
        }
      }

      val pi_trinket_starters = List(Rotations.FIREBALL, Rotations.PYROBLAST)

      if (piStart == -15 && curMageState.mage.hasPI && pi_trinket_starters.contains(nextSpell)) {
        piStart = curMageState.castTime
      }

      val toepStart = if (curMageState.toep == -15 && curMageState.mage.hasPI && pi_trinket_starters.contains(nextSpell)) {
        curMageState.castTime
      } else curMageState.toep

      val nextCast = if (nextSpell == Rotations.SCORCH) {
        curMageState.castTime + 1.5 + gcdIncurred
      } else if (nextSpell == Rotations.FIREBLAST) {
        curMageState.castTime
      } else if (mqgActivated + 20 > curMageState.castTime) {
        mqgCasts += 1
        if (nextSpell == Rotations.PYROBLAST) {
          curMageState.castTime + 4.5 + gcdIncurred
        } else {
          curMageState.castTime + 2.25 + gcdIncurred
        }
      } else if (nextSpell == Rotations.PYROBLAST) {
        curMageState.castTime + 6 + gcdIncurred
      } else {
        if (improvedFrostbolt && nextSpell == Rotations.FROSTBOLT) {
          curMageState.castTime + 2.5 + gcdIncurred
        } else {
          curMageState.castTime + 3 + gcdIncurred
        }
      }

      val fireblastCast = if (nextSpell == Rotations.FIREBLAST) curMageState.castTime else curMageState.lastFireblast

      pq += curMageState.copy(castTime = nextCast, spell = nextSpell, castNumber = curMageState.castNumber + 1,
        mqgActivated = mqgActivated, lastFireblast = fireblastCast, toep = toepStart)
      curMageState = pq.dequeue()

    }

    totalWeightedTicks += igniteState.weightedTicks

    val lastIgniteTicks = if (igniteState.igniteTick < fightLength) {
      igniteState.igniteSize
    } else {
      0
    }

    totalIgniteDmg = totalIgniteDmg + lastIgniteTicks.toInt
    val totalDmg = totalIgniteDmg + totalCastDmg

    ResultRow(mageSetup, totalDmg / fightLength, totalIgniteDmg / fightLength, totalWeightedTicks, igniteDropList)
  }

  def calcPI(piStart: Double, curMageState: MageState): Double = {
    if (piStart < curMageState.castTime &&
      piStart + 15 > curMageState.castTime && curMageState.mage.hasPI) {
      1.2
    } else {
      1
    }
  }

  def crit(probCrit: Double): Boolean = {
    Math.random() <= probCrit
  }

  def hit(probHit: Double): Boolean = {
    Math.random() <= probHit
  }

}
