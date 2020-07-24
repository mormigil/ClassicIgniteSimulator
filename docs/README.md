# Mage Rotation Comparisons

The optimal rotation for your mage group is very dependent on the number of mages you have, the presence of world buffs, and the number of PIs your group has access to. All of the generated data was done with a standard set of mages that all have 98% hit, 21% crit, and 750 spell power.

In order to accomodate how different rotations perform against each other I generated a large dataset with a large variety of conditions. I've now represented this dataset as two parallel coordinates graphs.

The first graph which can be found [here](https://mormigil.github.io/ClassicIgniteSimulator/percent_dmg_increase.html) compares all the conditions to the dps increase over a standard 1 frostbolt rotation. The second graph can be found [here](https://mormigil.github.io/ClassicIgniteSimulator/dps.html) compares all the conditions to the total dps of the given conditions. Finally a third graph can be found [here](https://mormigil.github.io/ClassicIgniteSimulator/ignite_dps.html) to find the highest ignite dps given these conditions if you want to game it for parsing.

Both graphs can be filtered by dragging a line along any of the axes. This way you can find the optimal rotations for your particular mage team's situation. 

## Rotations

I had to use all numeric values for the parallel coordinates graph so the rotations are numbered as the indexes of a list. 

- Rotation 0:
    + PI: Scorch -> Combustion + MQG -> Frostbolt -> Fireball
    + Non-PI: Scorch -> Frostbolt -> Frostbolt -> Frostbolt -> Fireball
- Rotation 1:
    + PI: Scorch -> Combustion + MQG -> Frostbolt -> Fireball
    + Non-PI: Scorch -> Frostbolt -> Fireball -> Combustion
- Rotation 2:
    + PI: Scorch -> Combustion + MQG -> Frostbolt -> Fireball
    + Non-PI: Scorch -> Frostbolt -> Frostbolt -> Fireball -> Combustion
- Rotation 3:
    + PI: Scorch -> Fire Blast -> Combustion -> Fireball -> MQG
    + Non-PI: Scorch -> Frostbolt -> Fireball -> Combustion
- Rotation 4: Fireblast weaving. Same as Rotation 1 except cast fireblast when off cooldown when a 5 stack ignite exists
- Rotation 5: Same as rotation 1 except 1 Mage casts scorch continously while ignite has 5 stacks and at least 1.5 seconds left
- Rotation 6 Same as rotation 2 except all mages have improved frostbolt talent
- Rotation 7: Same as rotation 1 except use an on use trinket (toep) at the expense of having less raw spell power
- Rotation 8: Same as rotation 1 except have combustion pre-stacked before the fight starts specifically 30% crit chance and 2 charges remaining
- Rotation 9: 
    + PI: Scorch -> Combustion + MQG -> Pyroblast -> Frostbolt
    + Non-PI: Scorch -> Pyroblast -> Frostbolt
- Rotation 10: Same as 9, but with R11 fireballs
- Rotation 11: Same as 1, but with R11 fireballs

## Num Ony

This column just reflects mages that only haave ony buff.


## Looking at the raw data

Feel free to peruse through the raw data I split it into files based on the number of mages to make it a little easier to parse. There is a new column called weightedTicks that can be used to recompute the ignite dps for any value of spell power instead of resimming the result.

