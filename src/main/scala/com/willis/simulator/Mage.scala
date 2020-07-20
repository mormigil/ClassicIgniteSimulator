package com.willis.simulator

//Add mqg start and combustion start
case class Mage(
  probCrit: Double,
  probHit: Double,
  mageId: Int,
  rotation: List[String] = List("SCORCH", "FROSTBOLT", "FIREBALL"),
  spellPower: Int = 800,
  scorchMage: Boolean = false,
  hasPI: Boolean = true,
  mqgStart: Int = 4) {
  def scorch(): Int = {
    multiply((237 + (Math.random() * 43) + spellPower * (1.5 / 3.5)))
  }

  def fireball(): Int = {
    multiply(596 + Math.random() * 164 + spellPower)
  }

  def fireballr11(): Int = {
    multiply(561 + Math.random() * 154 + spellPower)
  }

  def frostbolt(): Int = {
    (515 + (Math.random() * 40) + spellPower * (3 / 3.5) * .95).toInt
  }

  def fireblast(): Int = {
    multiply(446 + Math.random() * 78 + spellPower * (1.5 / 3.5))
  }

  def multiply(dmg: Double): Int = {
    (dmg * 1.1 * 1.1).toInt
  }
}
