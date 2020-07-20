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

    for (numMages <- 4 to 8) {
      for (numWB <- 0 to numMages) {
        for (semiWB <- numWB to numMages) {
          for (pis <- 1 to 5) {
            for (nightfall <- 0 to 1) {
              for (dmf <- 0 to 1) {
                for (fightLength <- 45 to 105 by 15) {
                  for (rotation <- Rotations.rotations) {
                    val ms = MageSetup(numMages, numWB, semiWB - numWB, pis, baseCrit, hit, sp, nightfall == 1, dmf == 1, fightLength, rotation)
                    val mageList = List.tabulate(numMages) { mageI =>
                      val adjustedCrit = if (mageI < numWB) baseCrit + 18 else if (mageI < semiWB) baseCrit + 10 else baseCrit
                      val rotationList = if (mageI < pis) {
                        rotation.piRotation
                      } else if (mageI < 4 && rotation.name == Rotations.FIREBALL_FROSTBOLT.name) {
                        Rotations.FIREBALL_4
                      } else {
                        rotation.rotation
                      }
                      val scorchMage = if (rotation.name == Rotations.SMART_SCORCH.name && mageI == 0) true else false
                      Mage(adjustedCrit.toDouble / 100, hit.toDouble / 100, mageI, rotationList, sp, scorchMage, mageI < pis, if (mageI < pis) 4 else 24)
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
