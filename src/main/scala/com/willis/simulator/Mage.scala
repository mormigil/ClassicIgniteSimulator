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
  def scorch(): Double = {
    multiply((237 + (Math.random() * 43) + spellPower * (1.5 / 3.5)))
  }

  def fireball(): Double = {
    multiply(596 + Math.random() * 164 + spellPower)
  }

  def fireballr11(): Double = {
    multiply(561 + Math.random() * 154 + spellPower)
  }

  def frostbolt(): Double = {
    (515 + (Math.random() * 40) + spellPower * (3 / 3.5) * .95)
  }

  def fireblast(): Double = {
    multiply(446 + Math.random() * 78 + spellPower * (1.5 / 3.5))
  }

  def pyroblast(): Double = {
    multiply(716 + Math.random() * 174 + spellPower)
  }

  //Fire talents dmg increase
  def multiply(dmg: Double): Double = {
    (dmg * 1.1)
  }
}
