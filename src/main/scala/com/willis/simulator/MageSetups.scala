package com.willis.simulator

import scala.collection.mutable

case class MageSetup(numMages: Int, numWB: Int, semiWB: Int, numPIs: Int,
  baseCrit: Int, baseHit: Int, sp: Int, nightfall: Boolean, dmf: Boolean, fightLength: Int, rotation: Rotation)

object MageSetups {

  //from 4 to 8 mages
  //from 0 to 8 WB mages
  //from 0 to 8 semi-WB mages
  //from 1 to 5 PIs
  //from .18 to .25 base crit chance
  //from .95 to .99 hit chance
  def main(args: Array[String]): Unit = {
    genMageSetups(21, 98, 750)
  }

  def genMageSetups(baseCrit: Int, hit: Int, sp: Int): Map[MageSetup, List[Mage]] = {

    val mageSetups = mutable.Map[MageSetup, List[Mage]]()

    for (numMages <- 3 to 8) {
      for (numWB <- 0 to numMages) {
        for (semiWB <- numWB to numMages) {
          for (pis <- 0 to 5) {
            for (nightfall <- 0 to 1) {
              for (dmf <- 0 to 1) {
                for (fightLength <- 40 to 120 by 10) {
                  for (rotation <- Rotations.rotations) {
                    val ms = MageSetup(numMages, numWB, semiWB - numWB, pis, baseCrit, hit, sp, nightfall == 1, dmf == 1, fightLength, rotation)
                    val mageList = List.tabulate(numMages) { mageI =>
                      var adjustedCrit = if (mageI < numWB) baseCrit + 18 else if (mageI < semiWB) baseCrit + 10 else baseCrit
                      val rotationList = if (mageI == 0 && pis > 0 && rotation.name == Rotations.PYRO_START.name) {
                        Rotations.PYROBLAST_START
                      } else if (mageI < pis) {
                        rotation.piRotation
                      } else {
                        rotation.rotation
                      }
                      val finalRotationList = if (numMages < 5) Rotations.SCORCH :: rotationList else rotationList
                      val scorchMage = if (Rotations.ONE_SCORCH.contains(rotation.name) && mageI == 0) {
                        true
                      } else if (Rotations.TWO_SCORCH_LIST.contains(rotation.name) && mageI < 2) {
                        true
                      } else false
                      var spell_power = if (Rotations.ON_USE_VS_TEAR.name == rotation.name && mageI < pis) sp - 55 else sp
                      Mage(adjustedCrit.toDouble / 100, hit.toDouble / 100, mageI, finalRotationList, spell_power, scorchMage, mageI < pis, if (mageI < pis) 4 else 8)
                    }
                    mageSetups.put(ms, mageList)
                  }
                }
              }
            }
          }
        }
      }
    }

    mageSetups.toMap

  }
}
